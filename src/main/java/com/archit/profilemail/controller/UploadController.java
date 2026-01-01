package com.archit.profilemail.controller;

import com.archit.profilemail.config.RedisConfig;
import com.archit.profilemail.model.CSVUploadData;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.CSVUploadDataRepository;
import com.archit.profilemail.repository.UserAccountRepository;
import com.archit.profilemail.service.upload.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * =============================================================================
 * UPLOAD CONTROLLER - Handles CSV file uploads
 * =============================================================================
 * This controller is the entry point for CSV uploads. It:
 * 1. Receives the file from the HTTP request
 * 2. Uploads it to S3 (Backblaze)
 * 3. Creates a job record in the database
 * 4. Pushes the job to Redis Stream for processing
 * 5. Returns immediately with a job ID
 * 
 * The actual processing happens in the Worker (CSVJobConsumerService)
 * =============================================================================
 */
@RestController
@RequestMapping("/api/import")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    // -------------------------------------------------------------------------
    // DEPENDENCIES
    // -------------------------------------------------------------------------
    
    private final UploadService uploadService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CSVUploadDataRepository csvUploadDataRepository;
    private final UserAccountRepository userAccountRepository;

    public UploadController(
            UploadService uploadService,
            RedisTemplate<String, Object> redisTemplate,
            CSVUploadDataRepository csvUploadDataRepository,
            UserAccountRepository userAccountRepository) {
        this.uploadService = uploadService;
        this.redisTemplate = redisTemplate;
        this.csvUploadDataRepository = csvUploadDataRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // -------------------------------------------------------------------------
    // CSV UPLOAD ENDPOINT
    // -------------------------------------------------------------------------
    
    /**
     * Handles CSV file upload.
     * 
     * Flow:
     * 1. Validate user is authenticated and exists
     * 2. Upload file to S3 (Backblaze) - this blocks but is necessary
     * 3. Create job record in database with PENDING status
     * 4. Push job to Redis Stream - workers will pick it up
     * 5. Return immediately with job ID for tracking
     * 
     * @param file The CSV file uploaded by the user
     * @param authentication Spring Security authentication object (auto-injected)
     * @return Response with job ID or error message
     */
    @PostMapping("/csv")
    public ResponseEntity<Map<String, Object>> csvImport(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        // Response map to return JSON
        Map<String, Object> response = new HashMap<>();
        
        try {
            // -----------------------------------------------------------------
            // STEP 1: Validate user
            // -----------------------------------------------------------------
            String username = authentication.getName();
            UserAccount owner = userAccountRepository.findByEmail(username);
            
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Validate file
            if (file.isEmpty()) {
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("CSV upload started for user: {}", username);
            
            // -----------------------------------------------------------------
            // STEP 2: Upload file to S3 (Backblaze)
            // -----------------------------------------------------------------
            // This is synchronous - we need the file in S3 before workers can access it
            // For very large files, consider chunked/multipart upload
            String fileReference = uploadService.upload(file, owner);
            log.info("File uploaded to S3: {}", fileReference);
            
            // -----------------------------------------------------------------
            // STEP 3: Create job record in database
            // -----------------------------------------------------------------
            // This allows us to track job status (PENDING → PROCESSING → COMPLETED)
            CSVUploadData uploadData = new CSVUploadData();
            uploadData.setFileKey(fileReference);
            uploadData.setOwner(owner);
            uploadData.setStatus("PENDING");
            uploadData = csvUploadDataRepository.save(uploadData);
            
            log.info("Job record created with ID: {}", uploadData.getId());
            
            // -----------------------------------------------------------------
            // STEP 4: Push job to Redis Stream
            // -----------------------------------------------------------------
            // Create job data as a Map (will be stored in Redis Stream)
            String jobId = UUID.randomUUID().toString();
            Map<String, String> jobData = new HashMap<>();
            jobData.put("jobId", jobId);
            jobData.put("ownerEmail", username);
            jobData.put("fileRef", fileReference);
            jobData.put("dbRecordId", uploadData.getId().toString());
            
            // XADD csv-jobs-stream * jobId xxx ownerEmail xxx fileRef xxx
            // This adds a new message to the stream
            // Workers will pick it up with XREADGROUP
            redisTemplate.opsForStream().add(
                StreamRecords.mapBacked(jobData).withStreamKey(RedisConfig.CSV_JOBS_STREAM)
            );
            
            log.info("Job pushed to Redis Stream: {}", jobId);
            
            // -----------------------------------------------------------------
            // STEP 5: Return success response
            // -----------------------------------------------------------------
            response.put("message", "CSV upload queued successfully");
            response.put("jobId", jobId);
            response.put("dbRecordId", uploadData.getId());
            response.put("status", "PENDING");
            response.put("info", "You will receive an email when processing is complete");
            
            // Return 202 Accepted (request accepted but not yet processed)
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            response.put("error", "Import failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // -------------------------------------------------------------------------
    // JOB STATUS ENDPOINT (Optional - for checking job status)
    // -------------------------------------------------------------------------
    
    /**
     * Check the status of a job.
     * 
     * @param dbRecordId The database record ID returned from the upload endpoint
     * @return Current status of the job
     */
    @GetMapping("/status/{dbRecordId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(
            @PathVariable Long dbRecordId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = authentication.getName();
            UserAccount owner = userAccountRepository.findByEmail(username);
            
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Find the job record
            CSVUploadData job = csvUploadDataRepository.findById(dbRecordId).orElse(null);
            
            if (job == null) {
                response.put("error", "Job not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Verify ownership
            if (!job.getOwner().getId().equals(owner.getId())) {
                response.put("error", "Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            response.put("jobId", dbRecordId);
            response.put("status", job.getStatus());
            response.put("fileKey", job.getFileKey());
            response.put("uploadedAt", job.getUploadedAt().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Status check failed: {}", e.getMessage(), e);
            response.put("error", "Failed to get status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
