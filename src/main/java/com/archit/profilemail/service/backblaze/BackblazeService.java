package com.archit.profilemail.service.backblaze;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class BackblazeService {

    private final AmazonS3 s3client;
    private final String bucketName;
    private final UserAccountRepository userAccountRepository;

    public BackblazeService(AmazonS3 s3client,
                            @Value("${backblaze.bucket}") String bucketName,
                            UserAccountRepository userAccountRepository) {
        this.s3client = s3client;
        this.bucketName = bucketName;
        this.userAccountRepository = userAccountRepository;
    }

    public String upload(MultipartFile file, UserAccount owner) throws IOException {
        String key = "csv-uploads/" + owner.getUsername() + "/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        s3client.putObject(bucketName, key, file.getInputStream(), metadata);
        //this return statement is to help us get the file in future using key
        return key;
    }
    public InputStream download(String key) {
        S3Object s3Object = s3client.getObject(bucketName, key);
        return s3Object.getObjectContent();
    }
}
