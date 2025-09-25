package com.archit.profilemail.service.upload;

import com.archit.profilemail.dtos.CSVValidationResult;
import com.archit.profilemail.dtos.NotificationRequest;
import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.notification.NotificationDispatcher;
import com.archit.profilemail.notification.strategy.NotificationType;
import com.archit.profilemail.notification.strategy.structures.OwnerNotificationMessage;
import com.archit.profilemail.repository.ProfileRepository;
import com.archit.profilemail.repository.UserAccountRepository;
import com.archit.profilemail.utils.CSVUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CSVImportService {
    private final CSVUtils csvUtils;
    private final ProfileRepository profileRepository;
    private final BatchProcessorService batchProcessorService;
    private final UserAccountRepository userAccountRepository;
    private final NotificationDispatcher notificationDispatcher;

    public CSVImportService(CSVUtils csvUtils,
                            ProfileRepository profileRepository,
                            BatchProcessorService batchProcessorService,
                            UserAccountRepository userAccountRepository,
                            NotificationDispatcher notificationDispatcher) {
        this.csvUtils = csvUtils;
        this.profileRepository = profileRepository;
        this.batchProcessorService = batchProcessorService;
        this.userAccountRepository = userAccountRepository;
        this.notificationDispatcher=notificationDispatcher;
    }

    private static final int BATCH_SIZE = 1000;
    private static String UPLOAD_COMPLETION_MESSAGE = "Your CSV upload processing is complete";
    private static String ALL_ROWS_SUCCESSFUL_MESSAGE = "All rows have been successfully uploaded";
    private static String INVALID_ROWS_MESSAGE = "Some rows were not successfully uploaded. Kindly fix these and re-upload: ";
    private static final Path TEMP_DIR = Paths.get("uploads/tmp").toAbsolutePath();
    private static final String UPLOAD_COMPLETION_SUBJECT = "Your CSV Upload has been completed";

    public void handleCSVUpload(MultipartFile file, String username) throws IOException {
        if (file.isEmpty()) {
            throw new FileNotFoundException("Empty file");
        }
        Files.createDirectories(TEMP_DIR);
        System.out.println(TEMP_DIR);
        Path tempFile = Files.createTempFile(TEMP_DIR, "upload_", ".csv");
        file.transferTo(tempFile.toFile());
        System.out.println("ImportService - start - " + System.currentTimeMillis());
        importCSVAsync(tempFile.toString(), username);
    }

    @Async("csvTaskExecutor")
    public void importCSVAsync(String filePath, String username) throws FileNotFoundException {
        Path path = Paths.get(filePath);
        UserAccount owner = userAccountRepository.findByEmail(username);
        try{
            String completeData = Files.readString(path);
            CSVValidationResult validationResult = csvUtils.extractValidProfiles(completeData, owner);
            if (validationResult.getValidProfiles().isEmpty()) {
                System.out.println("No valid profiles found in CSV.");
                return;
            }
            List<Profile> nonDuplicateProfiles = csvUtils.removeDuplicates(validationResult.getValidProfiles(), owner, profileRepository);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < nonDuplicateProfiles.size(); i += BATCH_SIZE) {
                List<Profile> batch = nonDuplicateProfiles.subList(
                        i, Math.min(i + BATCH_SIZE, nonDuplicateProfiles.size()));
                futures.add(batchProcessorService.processInBatches(batch));
            }
            OwnerNotificationMessage message = OwnerNotificationMessage.builder()
                    .fromAddress(NotificationRequest.UNIVERSAL_FROM_ADDRESS)
                    .toAddress(owner.getEmail())
                    .subject(UPLOAD_COMPLETION_SUBJECT)
                    .body(createMessageBody(validationResult)).build();

            CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allDone.thenRun(()->notificationDispatcher.dispatch(NotificationRequest.builder()
                    .notificationType(NotificationType.OWNER_EMAIL)
                    .message(message)
                    .build()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ex) {
                System.err.println("Failed to delete temp file: " + path);
            }
        }
    }

    public String createMessageBody(CSVValidationResult validationResult){
        if(validationResult.getInvalidRows().isEmpty()){
            return UPLOAD_COMPLETION_MESSAGE + ALL_ROWS_SUCCESSFUL_MESSAGE;
        }
        else{
            return UPLOAD_COMPLETION_MESSAGE + INVALID_ROWS_MESSAGE + validationResult.getInvalidRows();
        }
    }
}
