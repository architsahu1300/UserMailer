package com.archit.profilemail.service.upload;

import com.archit.profilemail.model.Profile;
import com.archit.profilemail.repository.ProfileRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class BatchProcessorService {

    private final ProfileRepository profileRepository;
    private final JdbcTemplate jdbcTemplate;

    public BatchProcessorService(ProfileRepository profileRepository, JdbcTemplate jdbcTemplate){
        this.profileRepository=profileRepository;
        this.jdbcTemplate=jdbcTemplate;
    }

    @Async("csvTaskExecutor")
    public CompletableFuture<Void> processInBatches(List<Profile> profileBatch) {
        String sql = "INSERT INTO profilesdb (email, owner_id) VALUES (?, ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        for (Profile profile : profileBatch) {
            batchArgs.add(new Object[]{
                    profile.getEmail(),
                    profile.getOwner().getId()
            });
        }

        try {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        } catch (DataAccessException ex) {
            System.err.println("Error during batch insert :" + ex.getMessage());
            // Optional: retry individually to identify problematic row(s)
        }
//        profileRepository.saveAll(profileBatch);
        System.out.println("Process - end - " + System.currentTimeMillis());
        return CompletableFuture.completedFuture(null);
    }
}
