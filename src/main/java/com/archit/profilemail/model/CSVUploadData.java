package com.archit.profilemail.model;

import com.amazonaws.services.s3.model.Owner;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class CSVUploadData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileKey;   // Key in Backblaze bucket

    @ManyToOne
    private UserAccount owner;      // Who uploaded this file

    private Instant uploadedAt = Instant.now();

    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED



}
