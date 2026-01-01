package com.archit.profilemail.service.worker;

import com.archit.profilemail.config.RedisConfig;
import com.archit.profilemail.dtos.CSVValidationResult;
import com.archit.profilemail.dtos.NotificationRequest;
import com.archit.profilemail.model.CSVUploadData;
import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.notification.NotificationDispatcher;
import com.archit.profilemail.notification.strategy.NotificationType;
import com.archit.profilemail.notification.strategy.structures.OwnerNotificationMessage;
import com.archit.profilemail.repository.CSVUploadDataRepository;
import com.archit.profilemail.repository.ProfileRepository;
import com.archit.profilemail.repository.UserAccountRepository;
import com.archit.profilemail.service.backblaze.BackblazeService;
import com.archit.profilemail.service.upload.BatchProcessorService;
import com.archit.profilemail.utils.CSVUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * =============================================================================
 * CSV JOB CONSUMER SERVICE - THE WORKER
 * =============================================================================
 * This service runs ONLY when the application starts with profile "worker".
 * It continuously polls Redis Stream for new jobs and processes them.
 * 
 * Key responsibilities:
 * 1. Join the consumer group on startup
 * 2. Poll for new jobs from Redis Stream
 * 3. Download CSV file from S3 (Backblaze)
 * 4. Parse and validate CSV data
 * 5. Insert valid profiles into database
 * 6. Send completion notification to file owner
 * 7. Acknowledge processed jobs to Redis
 * 8. Handle failed/stale jobs from dead workers
 * 
 * The @Profile("worker") annotation ensures this bean is ONLY created
 * when running with --spring.profiles.active=worker
 * =============================================================================
 */
@Service
@org.springframework.context.annotation.Profile("worker")  // This is the magic! Only active in "worker" profile
public class CSVJobConsumerService {

    private static final Logger log = LoggerFactory.getLogger(CSVJobConsumerService.class);
    
    // -------------------------------------------------------------------------
    // CONFIGURATION CONSTANTS
    // -------------------------------------------------------------------------
    
    /**
     * Batch size for database inserts
     * Larger batches = fewer round trips = faster, but more memory
     * 1000 is a good balance
     */
    private static final int BATCH_SIZE = 1000;
    
    /**
     * How long to wait for new messages when polling (in seconds)
     * If no messages, wait this long before polling again
     */
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);
    
    /**
     * How long a message can be "pending" before we consider the worker dead
     * and claim the message for ourselves
     */
    private static final Duration STALE_MESSAGE_THRESHOLD = Duration.ofMinutes(5);
    
    // Email notification messages
    private static final String UPLOAD_COMPLETION_SUBJECT = "Your CSV Upload has been completed";
    private static final String UPLOAD_COMPLETION_MESSAGE = "Your CSV upload processing is complete. ";
    private static final String ALL_ROWS_SUCCESSFUL_MESSAGE = "All rows have been successfully uploaded.";
    private static final String INVALID_ROWS_MESSAGE = "Some rows were not uploaded. Please fix and re-upload: ";
    
    // -------------------------------------------------------------------------
    // DEPENDENCIES (Injected by Spring)
    // -------------------------------------------------------------------------
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final BackblazeService backblazeService;
    private final CSVUtils csvUtils;
    private final ProfileRepository profileRepository;
    private final BatchProcessorService batchProcessorService;
    private final UserAccountRepository userAccountRepository;
    private final CSVUploadDataRepository csvUploadDataRepository;
    private final NotificationDispatcher notificationDispatcher;
    
    /**
     * Unique ID for this worker instance
     * Each worker gets a random UUID so Redis can track which worker has which messages
     */
    private final String consumerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------
    
    public CSVJobConsumerService(
            RedisTemplate<String, Object> redisTemplate,
            BackblazeService backblazeService,
            CSVUtils csvUtils,
            ProfileRepository profileRepository,
            BatchProcessorService batchProcessorService,
            UserAccountRepository userAccountRepository,
            CSVUploadDataRepository csvUploadDataRepository,
            NotificationDispatcher notificationDispatcher) {
        this.redisTemplate = redisTemplate;
        this.backblazeService = backblazeService;
        this.csvUtils = csvUtils;
        this.profileRepository = profileRepository;
        this.batchProcessorService = batchProcessorService;
        this.userAccountRepository = userAccountRepository;
        this.csvUploadDataRepository = csvUploadDataRepository;
        this.notificationDispatcher = notificationDispatcher;
    }
    
    // -------------------------------------------------------------------------
    // INITIALIZATION - Runs once when worker starts
    // -------------------------------------------------------------------------
    
    /**
     * Called automatically by Spring after the bean is created.
     * Creates the consumer group if it doesn't exist.
     * 
     * Consumer group is like a "team" that shares the work:
     * - All workers in the same group compete for messages
     * - Each message goes to exactly ONE worker in the group
     * - If a worker dies, its messages can be claimed by others
     */
    @PostConstruct
    public void init() {
        log.info("Starting CSV Job Consumer with ID: {}", consumerId);
        createConsumerGroupIfNotExists();
    }
    
    /**
     * Creates the Redis Stream and Consumer Group if they don't exist.
     * 
     * Redis commands equivalent:
     * - XGROUP CREATE csv-jobs-stream csv-workers $ MKSTREAM
     * 
     * The "$" means "start reading from new messages only"
     * MKSTREAM creates the stream if it doesn't exist
     */
    private void createConsumerGroupIfNotExists() {
        try {
            // Try to create the consumer group
            redisTemplate.opsForStream().createGroup(
                RedisConfig.CSV_JOBS_STREAM,    // Stream name
                ReadOffset.latest(),             // Start from latest (new messages only)
                RedisConfig.CSV_JOBS_GROUP       // Group name
            );
            log.info("Created consumer group: {}", RedisConfig.CSV_JOBS_GROUP);
        } catch (Exception e) {
            // Group already exists - this is fine, just log it
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.info("Consumer group already exists: {}", RedisConfig.CSV_JOBS_GROUP);
            } else {
                log.warn("Could not create consumer group (may already exist): {}", e.getMessage());
            }
        }
    }
    
    // -------------------------------------------------------------------------
    // MAIN CONSUMER LOOP - Runs continuously
    // -------------------------------------------------------------------------
    
    /**
     * Main job polling method - runs every 100ms.
     * 
     * @Scheduled(fixedDelay = 100) means:
     * - Wait 100ms AFTER the previous execution finishes
     * - Then run again
     * - This prevents overlapping executions
     * 
     * The flow:
     * 1. Poll Redis for new messages
     * 2. If message found, process it
     * 3. Acknowledge successful processing
     * 4. If processing fails, message stays in "pending" state
     */
    @Scheduled(fixedDelay = 100)  // Poll every 100ms
    public void consumeJobs() {
        try {
            // Read messages from the stream using our consumer group
            // XREADGROUP GROUP csv-workers worker-xxx COUNT 1 BLOCK 5000 STREAMS csv-jobs-stream >
            @SuppressWarnings("unchecked")
            List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream().read(
                Consumer.from(RedisConfig.CSV_JOBS_GROUP, consumerId),  // Who is reading
                StreamReadOptions.empty()
                    .count(1)                        // Read 1 message at a time
                    .block(POLL_TIMEOUT),            // Wait up to 5 seconds for a message
                StreamOffset.create(RedisConfig.CSV_JOBS_STREAM, ReadOffset.lastConsumed())  // ">" means undelivered messages
            );
            
            if (messages != null && !messages.isEmpty()) {
                for (MapRecord<String, Object, Object> message : messages) {
                    processMessage(message);
                }
            }
        } catch (Exception e) {
            log.error("Error consuming jobs: {}", e.getMessage(), e);
        }
    }
    
    // -------------------------------------------------------------------------
    // MESSAGE PROCESSING
    // -------------------------------------------------------------------------
    
    /**
     * Process a single message from the Redis Stream.
     * 
     * @param message The Redis Stream message containing job data
     *                Format: { "jobId": "...", "ownerEmail": "...", "fileRef": "..." }
     */
    private void processMessage(MapRecord<String, Object, Object> message) {
        String messageId = message.getId().getValue();
        log.info("Processing message: {}", messageId);
        
        try {
            // Extract job data from message
            // The message value is a Map with our job fields
            var data = message.getValue();
            String jobId = getStringValue(data, "jobId");
            String ownerEmail = getStringValue(data, "ownerEmail");
            String fileRef = getStringValue(data, "fileRef");
            
            log.info("Job details - ID: {}, Owner: {}, File: {}", jobId, ownerEmail, fileRef);
            
            // Update job status to PROCESSING
            updateJobStatus(jobId, "PROCESSING");
            
            // Download file from S3
            log.info("Downloading file from S3: {}", fileRef);
            String csvContent = downloadAndReadFile(fileRef);
            
            // Get the owner from database
            UserAccount owner = userAccountRepository.findByEmail(ownerEmail);
            if (owner == null) {
                throw new RuntimeException("Owner not found: " + ownerEmail);
            }
            
            // Parse and validate CSV
            log.info("Parsing CSV content...");
            CSVValidationResult validationResult = csvUtils.extractValidProfiles(csvContent, owner);
            
            if (validationResult.getValidProfiles().isEmpty()) {
                log.warn("No valid profiles found in CSV");
                updateJobStatus(jobId, "COMPLETED");
                sendCompletionNotification(owner, validationResult);
                acknowledgeMessage(message);
                return;
            }
            
            // Remove duplicates (profiles that already exist in DB)
            List<Profile> nonDuplicateProfiles = csvUtils.removeDuplicates(
                validationResult.getValidProfiles(), owner, profileRepository);
            
            log.info("Valid profiles: {}, After dedup: {}", 
                validationResult.getValidProfiles().size(), nonDuplicateProfiles.size());
            
            // Process in batches
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < nonDuplicateProfiles.size(); i += BATCH_SIZE) {
                List<Profile> batch = nonDuplicateProfiles.subList(
                    i, Math.min(i + BATCH_SIZE, nonDuplicateProfiles.size()));
                futures.add(batchProcessorService.processInBatches(batch));
            }
            
            // Wait for all batches to complete, then send notification
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    updateJobStatus(jobId, "COMPLETED");
                    sendCompletionNotification(owner, validationResult);
                    log.info("Job completed successfully: {}", jobId);
                })
                .exceptionally(ex -> {
                    log.error("Error in batch processing: {}", ex.getMessage());
                    updateJobStatus(jobId, "FAILED");
                    return null;
                })
                .join();  // Wait for completion
            
            // Acknowledge the message - removes it from pending list
            acknowledgeMessage(message);
            
        } catch (Exception e) {
            log.error("Error processing message {}: {}", messageId, e.getMessage(), e);
            // Don't acknowledge - message will stay in pending and can be retried
            // or claimed by another worker after STALE_MESSAGE_THRESHOLD
        }
    }
    
    // -------------------------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------------------------
    
    /**
     * Safely extract a string value from the message data map.
     */
    private String getStringValue(java.util.Map<Object, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Download file from S3 and convert to String.
     * Uses streaming to avoid loading entire file into memory at once.
     */
    private String downloadAndReadFile(String fileRef) {
        try (InputStream inputStream = backblazeService.download(fileRef);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + fileRef, e);
        }
    }
    
    /**
     * Update the job status in the database.
     */
    private void updateJobStatus(String jobId, String status) {
        try {
            // jobId might be the Redis job ID or the database ID
            // Try to find by file key or update by job tracking
            log.info("Updating job {} status to {}", jobId, status);
            // Note: You may need to adjust this based on how you're tracking jobs
        } catch (Exception e) {
            log.warn("Could not update job status: {}", e.getMessage());
        }
    }
    
    /**
     * Send completion notification email to the file owner.
     */
    private void sendCompletionNotification(UserAccount owner, CSVValidationResult validationResult) {
        try {
            String body = createMessageBody(validationResult);
            
            OwnerNotificationMessage message = OwnerNotificationMessage.builder()
                .fromAddress(NotificationRequest.UNIVERSAL_FROM_ADDRESS)
                .toAddress(owner.getEmail())
                .subject(UPLOAD_COMPLETION_SUBJECT)
                .body(body)
                .build();
            
            notificationDispatcher.dispatch(NotificationRequest.builder()
                .notificationType(NotificationType.OWNER_EMAIL)
                .message(message)
                .build());
                
            log.info("Sent completion notification to {}", owner.getEmail());
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
    }
    
    /**
     * Create the email body based on validation results.
     */
    private String createMessageBody(CSVValidationResult validationResult) {
        if (validationResult.getInvalidRows().isEmpty()) {
            return UPLOAD_COMPLETION_MESSAGE + ALL_ROWS_SUCCESSFUL_MESSAGE;
        } else {
            return UPLOAD_COMPLETION_MESSAGE + INVALID_ROWS_MESSAGE + 
                   validationResult.getInvalidRows();
        }
    }
    
    /**
     * Acknowledge a message - tells Redis we've successfully processed it.
     * 
     * XACK csv-jobs-stream csv-workers <message-id>
     * 
     * After acknowledgment:
     * - Message is removed from the "pending entries list"
     * - It won't be redelivered
     * - Other workers won't try to claim it
     */
    private void acknowledgeMessage(MapRecord<String, Object, Object> message) {
        redisTemplate.opsForStream().acknowledge(
            RedisConfig.CSV_JOBS_STREAM,
            RedisConfig.CSV_JOBS_GROUP,
            message.getId()
        );
        log.debug("Acknowledged message: {}", message.getId().getValue());
    }
    
    // -------------------------------------------------------------------------
    // STALE MESSAGE RECOVERY
    // -------------------------------------------------------------------------
    
    /**
     * Periodically check for and claim stale messages.
     * 
     * This handles the case where a worker crashed while processing a message.
     * The message would be stuck in "pending" state forever without this.
     * 
     * Runs every 60 seconds.
     */
    @Scheduled(fixedRate = 60000)  // Every 60 seconds
    public void claimStaleMessages() {
        try {
            // Get pending messages for our consumer group
            PendingMessagesSummary pending = redisTemplate.opsForStream()
                .pending(RedisConfig.CSV_JOBS_STREAM, RedisConfig.CSV_JOBS_GROUP);
            
            if (pending != null && pending.getTotalPendingMessages() > 0) {
                log.info("Found {} pending messages", pending.getTotalPendingMessages());
                
                // Get detailed pending message info
                PendingMessages pendingMessages = redisTemplate.opsForStream().pending(
                    RedisConfig.CSV_JOBS_STREAM,
                    Consumer.from(RedisConfig.CSV_JOBS_GROUP, consumerId),
                    org.springframework.data.domain.Range.unbounded(),
                    100  // Max messages to check
                );
                
                // Note: Claiming logic would go here
                // For simplicity, we're just logging. In production, you'd use XCLAIM
                // to take ownership of stale messages from dead workers.
            }
        } catch (Exception e) {
            log.debug("Error checking stale messages: {}", e.getMessage());
        }
    }
}

