package com.archit.profilemail.service;

import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.ProfileRepository;
import com.archit.profilemail.repository.UserAccountRepository;
import com.archit.profilemail.utils.CSVUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class CSVImportService {
    private final CSVUtils csvUtils;
    private final ProfileRepository profileRepository;
    private final BatchProcessorService batchProcessorService;
    private final UserAccountRepository userAccountRepository;

    public CSVImportService(CSVUtils csvUtils,
                            ProfileRepository profileRepository,
                            BatchProcessorService batchProcessorService,
                            UserAccountRepository userAccountRepository) {
        this.csvUtils = csvUtils;
        this.profileRepository = profileRepository;
        this.batchProcessorService = batchProcessorService;
        this.userAccountRepository = userAccountRepository;
    }

    private static final int BATCH_SIZE = 1000;
    private static final Path TEMP_DIR = Paths.get("uploads/tmp").toAbsolutePath();

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
            List<Profile> validProfiles = csvUtils.extractValidProfiles(completeData, owner);
            if (validProfiles.isEmpty()) {
                System.out.println("No valid profiles found in CSV.");
                return;
            }
            List<Profile> nonDuplicateProfiles = csvUtils.removeDuplicates(validProfiles, owner, profileRepository);
            for (int i = 0; i < nonDuplicateProfiles.size(); i += BATCH_SIZE) {
                List<Profile> batch = nonDuplicateProfiles.subList(
                        i, Math.min(i + BATCH_SIZE, nonDuplicateProfiles.size()));
                batchProcessorService.processInBatches(batch);
            }
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
}
