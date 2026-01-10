package com.archit.profilemail.service.upload;

import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.ProfileProperty;
import com.archit.profilemail.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class BatchProcessorService {

    private static final Logger log = LoggerFactory.getLogger(BatchProcessorService.class);

    private final ProfileRepository profileRepository;
    private final JdbcTemplate jdbcTemplate;

    public BatchProcessorService(ProfileRepository profileRepository, JdbcTemplate jdbcTemplate){
        this.profileRepository=profileRepository;
        this.jdbcTemplate=jdbcTemplate;
    }

    @Async("csvTaskExecutor")
    public CompletableFuture<Void> processInBatches(List<Profile> profileBatch) {
        // Step 1: Insert profiles and get their generated IDs
        String profileSql = "INSERT INTO profilesdb (email, owner_id) VALUES (?, ?)";

        List<Object[]> profileArgs = new ArrayList<>();
        for (Profile profile : profileBatch) {
            profileArgs.add(new Object[]{
                    profile.getEmail(),
                    profile.getOwner().getId()
            });
        }

        try {
            jdbcTemplate.batchUpdate(profileSql, profileArgs);
            
            // Step 2: Fetch the inserted profile IDs
            // We need to get the IDs of profiles we just inserted to link properties
            Long ownerId = profileBatch.get(0).getOwner().getId();
            List<String> emails = new ArrayList<>();
            for (Profile profile : profileBatch) {
                emails.add(profile.getEmail());
            }
            
            // Build a map of email -> profileId for the profiles we just inserted
            Map<String, Long> emailToProfileId = fetchProfileIds(emails, ownerId);
            
            // Step 3: Insert properties for each profile
            insertProperties(profileBatch, emailToProfileId);
            
            log.info("Batch insert completed: {} profiles with properties", profileBatch.size());
            
        } catch (DataAccessException ex) {
            log.error("Error during batch insert: {}", ex.getMessage());
            // Optional: retry individually to identify problematic row(s)
        }
        
        log.debug("Process - end - {}", System.currentTimeMillis());
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Fetch profile IDs for the given emails and owner.
     */
    private Map<String, Long> fetchProfileIds(List<String> emails, Long ownerId) {
        Map<String, Long> emailToProfileId = new HashMap<>();
        
        if (emails.isEmpty()) {
            return emailToProfileId;
        }
        
        // Build placeholders for IN clause
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < emails.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }
        
        String sql = "SELECT id, email FROM profilesdb WHERE owner_id = ? AND email IN (" + placeholders + ")";
        
        // Build args array: ownerId first, then all emails
        Object[] args = new Object[emails.size() + 1];
        args[0] = ownerId;
        for (int i = 0; i < emails.size(); i++) {
            args[i + 1] = emails.get(i);
        }
        
        jdbcTemplate.query(sql, args, (rs) -> {
            emailToProfileId.put(rs.getString("email"), rs.getLong("id"));
        });
        
        return emailToProfileId;
    }
    
    /**
     * Insert properties for all profiles in the batch.
     */
    private void insertProperties(List<Profile> profileBatch, Map<String, Long> emailToProfileId) {
        String propertySql = "INSERT INTO propertiesdb (key, value, profile_id) VALUES (?, ?, ?)";
        
        List<Object[]> propertyArgs = new ArrayList<>();
        
        for (Profile profile : profileBatch) {
            Long profileId = emailToProfileId.get(profile.getEmail());
            if (profileId == null) {
                log.warn("Could not find profile ID for email: {}", profile.getEmail());
                continue;
            }
            
            List<ProfileProperty> properties = profile.getProperties();
            if (properties != null) {
                for (ProfileProperty prop : properties) {
                    // Skip empty keys or values
                    if (prop.getKey() != null && !prop.getKey().trim().isEmpty()) {
                        propertyArgs.add(new Object[]{
                                prop.getKey().trim(),
                                prop.getValue() != null ? prop.getValue().trim() : "",
                                profileId
                        });
                    }
                }
            }
        }
        
        if (!propertyArgs.isEmpty()) {
            try {
                jdbcTemplate.batchUpdate(propertySql, propertyArgs);
                log.info("Inserted {} properties for {} profiles", propertyArgs.size(), profileBatch.size());
            } catch (DataAccessException ex) {
                log.error("Error inserting properties: {}", ex.getMessage());
            }
        }
    }
}
