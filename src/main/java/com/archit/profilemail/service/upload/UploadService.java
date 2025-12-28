package com.archit.profilemail.service.upload;

import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.service.backblaze.BackblazeService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UploadService {
    private final BackblazeService backblazeService;

    public UploadService(BackblazeService backblazeService) {
        this.backblazeService = backblazeService;
    }
    public String upload(MultipartFile file, UserAccount owner) throws IOException {
        return backblazeService.upload(file, owner);
    }

}
