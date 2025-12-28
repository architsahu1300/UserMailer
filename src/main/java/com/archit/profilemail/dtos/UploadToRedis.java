package com.archit.profilemail.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadToRedis implements Serializable {
    private String jobId;
    private String ownerEmail;
    private String fileRef;
}
