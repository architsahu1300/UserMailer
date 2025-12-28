package com.archit.profilemail.controller;

import com.archit.profilemail.dtos.UploadToRedis;
import com.archit.profilemail.model.CSVUploadData;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.CSVUploadDataRepository;
import com.archit.profilemail.repository.UserAccountRepository;
import com.archit.profilemail.service.backblaze.BackblazeService;
import com.archit.profilemail.service.upload.CSVImportService;
import com.archit.profilemail.service.upload.UploadService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/import")
public class UploadController {
//    private CSVImportService csvImportService;
    private final UploadService uploadService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CSVUploadDataRepository csvUploadDataRepository;
    private final UserAccountRepository userAccountRepository;


//    public UploadController(CSVImportService csvImportService){
//        this.csvImportService=csvImportService;
//    }

    public UploadController(UploadService uploadService, RedisTemplate<String, Object> redisTemplate,
                            CSVUploadDataRepository csvUploadDataRepository,
                            UserAccountRepository userAccountRepository){
        this.uploadService = uploadService;
        this.redisTemplate = redisTemplate;
        this.csvUploadDataRepository = csvUploadDataRepository;
        this.userAccountRepository = userAccountRepository;
    }
    @PostMapping("/csv")
    public ResponseEntity<String> csvImport(@RequestParam("file") MultipartFile file, Authentication authentication){
        try {
            String username = authentication.getName();
            UserAccount owner = userAccountRepository.findByEmail(username);
            if(owner==null){
                return (ResponseEntity<String>) ResponseEntity.status(HttpStatusCode.valueOf(404));
            }
            //Upload file to S3 and get the key
            String fileReference = uploadService.upload(file,owner);

            //Upload file key to DB
            CSVUploadData uploadData = new CSVUploadData();
            uploadData.setFileKey(fileReference);
            uploadData.setOwner(owner);
            uploadData.setStatus("PENDING");
            csvUploadDataRepository.save(uploadData);

            //Push Job to Redis
            String redisJobId = String.valueOf(UUID.randomUUID());
            UploadToRedis job = new UploadToRedis(redisJobId, username, fileReference);
            redisTemplate.opsForList().leftPush("csv-upload-jobs",job);

            return ResponseEntity.ok("CSV import started. You will be notified when upload is complete.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Import failed: " + e.getMessage());
        }
    }
}
