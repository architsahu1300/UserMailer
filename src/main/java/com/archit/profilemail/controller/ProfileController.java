package com.archit.profilemail.controller;

import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.ProfileProperty;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.ProfileRepository;
import com.archit.profilemail.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * =============================================================================
 * PROFILE CONTROLLER - Handles profile-related API endpoints
 * =============================================================================
 * This controller provides endpoints for the frontend to:
 * 1. Get a paginated list of profiles for the authenticated user
 * 2. Get profile details (future)
 * =============================================================================
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;

    public ProfileController(ProfileRepository profileRepository, UserAccountRepository userAccountRepository) {
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Get paginated list of profiles for the authenticated user.
     * 
     * @param page Page number (0-based)
     * @param size Number of profiles per page
     * @param authentication Spring Security authentication object
     * @return Paginated list of profiles
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = authentication.getName();
            UserAccount owner = userAccountRepository.findByEmail(username);
            
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Get paginated profiles for this user
            Pageable pageable = PageRequest.of(page, size);
            Page<Profile> profilePage = profileRepository.findByOwner(owner, pageable);
            
            // Convert to a format suitable for the frontend
            List<Map<String, Object>> profileList = profilePage.getContent().stream()
                .map(this::convertProfileToMap)
                .collect(Collectors.toList());
            
            response.put("profiles", profileList);
            response.put("totalCount", profilePage.getTotalElements());
            response.put("totalPages", profilePage.getTotalPages());
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get profiles: {}", e.getMessage(), e);
            response.put("error", "Failed to get profiles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get a single profile by ID.
     * 
     * @param id The profile ID
     * @param authentication Spring Security authentication object
     * @return Profile details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProfile(
            @PathVariable Long id,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = authentication.getName();
            UserAccount owner = userAccountRepository.findByEmail(username);
            
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Find the profile
            Profile profile = profileRepository.findById(id).orElse(null);
            
            if (profile == null) {
                response.put("error", "Profile not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Verify ownership - users can only view their own profiles
            if (!profile.getOwner().getId().equals(owner.getId())) {
                response.put("error", "Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Return the profile data
            return ResponseEntity.ok(convertProfileToMap(profile));
            
        } catch (Exception e) {
            log.error("Failed to get profile: {}", e.getMessage(), e);
            response.put("error", "Failed to get profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Convert a Profile entity to a Map for JSON response.
     * This flattens the ProfileProperties into a simple properties object.
     */
    private Map<String, Object> convertProfileToMap(Profile profile) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", profile.getId());
        map.put("email", profile.getEmail());
        
        // Convert properties list to a simple key-value map
        Map<String, String> properties = new HashMap<>();
        if (profile.getProperties() != null) {
            for (ProfileProperty prop : profile.getProperties()) {
                properties.put(prop.getKey(), prop.getValue());
            }
        }
        map.put("properties", properties);
        
        return map;
    }
}

