package com.archit.profilemail.controller;

import com.archit.profilemail.service.CSVImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
public class UploadController {
    @Autowired
    private CSVImportService csvImportService;

//    public UploadController(CSVImportService csvImportService){
//        this.csvImportService=csvImportService;
//    }
    @PostMapping("/csv")
    public ResponseEntity<String> csvImport(@RequestParam("file") MultipartFile file, Authentication authentication){
        try {
            String username = authentication.getName();
            csvImportService.importCSV(file,username);
            return ResponseEntity.ok("CSV import successful.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Import failed: " + e.getMessage());
        }
    }
}
