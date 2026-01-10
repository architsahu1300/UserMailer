package com.archit.profilemail.controller;

import com.archit.profilemail.dtos.CampaignRequest;
import com.archit.profilemail.dtos.FilterCriteria;
import com.archit.profilemail.dtos.FilterPreviewRequest;
import com.archit.profilemail.model.Campaign;
import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.ProfileProperty;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.UserAccountRepository;
import com.archit.profilemail.service.campaign.CampaignService;
import com.archit.profilemail.service.campaign.ProfileFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * =============================================================================
 * CAMPAIGN CONTROLLER - Handles campaign-related API endpoints
 * =============================================================================
 * 
 * Endpoints:
 * - POST   /api/campaigns              - Create a new campaign
 * - GET    /api/campaigns              - Get all campaigns (paginated)
 * - GET    /api/campaigns/{id}         - Get a specific campaign
 * - DELETE /api/campaigns/{id}         - Cancel a campaign
 * - POST   /api/campaigns/preview      - Preview filter results before creating
 * - GET    /api/campaigns/properties   - Get available property keys for filtering
 * - GET    /api/campaigns/properties/{key}/values - Get values for a property
 * =============================================================================
 */
@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {
    
    private static final Logger log = LoggerFactory.getLogger(CampaignController.class);
    
    private final CampaignService campaignService;
    private final ProfileFilterService profileFilterService;
    private final UserAccountRepository userAccountRepository;
    
    public CampaignController(
            CampaignService campaignService,
            ProfileFilterService profileFilterService,
            UserAccountRepository userAccountRepository
    ) {
        this.campaignService = campaignService;
        this.profileFilterService = profileFilterService;
        this.userAccountRepository = userAccountRepository;
    }
    
    /**
     * Create a new email campaign.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCampaign(
            @RequestBody CampaignRequest request,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserAccount owner = getOwner(authentication);
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // Validate request
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                response.put("error", "Campaign name is required");
                return ResponseEntity.badRequest().body(response);
            }
            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                response.put("error", "Email subject is required");
                return ResponseEntity.badRequest().body(response);
            }
            if (request.getBody() == null || request.getBody().trim().isEmpty()) {
                response.put("error", "Email body is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            Campaign campaign = campaignService.createCampaign(owner, request);
            
            response.put("success", true);
            response.put("campaign", convertCampaignToMap(campaign));
            response.put("message", "Campaign created successfully");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Failed to create campaign", e);
            response.put("error", "Failed to create campaign: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get all campaigns for the authenticated user (paginated).
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserAccount owner = getOwner(authentication);
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Page<Campaign> campaignPage = campaignService.getCampaigns(owner, PageRequest.of(page, size));
            
            List<Map<String, Object>> campaigns = campaignPage.getContent().stream()
                    .map(this::convertCampaignToMap)
                    .collect(Collectors.toList());
            
            response.put("campaigns", campaigns);
            response.put("totalCount", campaignPage.getTotalElements());
            response.put("totalPages", campaignPage.getTotalPages());
            response.put("page", page);
            response.put("size", size);
            
            // Include stats
            CampaignService.CampaignStats stats = campaignService.getStats(owner);
            response.put("stats", Map.of(
                    "completed", stats.completed(),
                    "scheduled", stats.scheduled(),
                    "drafts", stats.drafts(),
                    "totalSent", stats.totalSent()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get campaigns", e);
            response.put("error", "Failed to get campaigns: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get a specific campaign by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCampaign(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserAccount owner = getOwner(authentication);
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            return campaignService.getCampaign(id, owner)
                    .map(campaign -> ResponseEntity.ok(convertCampaignToMap(campaign)))
                    .orElseGet(() -> {
                        response.put("error", "Campaign not found");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
            
        } catch (Exception e) {
            log.error("Failed to get campaign", e);
            response.put("error", "Failed to get campaign: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Cancel a campaign.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancelCampaign(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserAccount owner = getOwner(authentication);
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            boolean cancelled = campaignService.cancelCampaign(id, owner);
            
            if (cancelled) {
                response.put("success", true);
                response.put("message", "Campaign cancelled successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "Cannot cancel campaign. It may not exist or is already processing.");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Failed to cancel campaign", e);
            response.put("error", "Failed to cancel campaign: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Preview filter results before creating a campaign.
     * Returns the count and a sample of matching profiles.
     */
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewFilters(
            @RequestBody FilterPreviewRequest request,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserAccount owner = getOwner(authentication);
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            CampaignRequest.FilterLogic logic = request.getFilterLogic() != null 
                    ? request.getFilterLogic() 
                    : CampaignRequest.FilterLogic.AND;
            
            log.info("Preview request - ownerId: {}, filters: {}, sampleSize requested: {}", 
                    owner.getId(), 
                    request.getFilters() != null ? request.getFilters().size() : 0,
                    request.getSampleSize());
            
            // Get count
            long totalCount = profileFilterService.countFilteredProfiles(
                    owner,
                    request.getFilters(),
                    logic
            );
            
            log.info("Preview - totalCount from DB: {}", totalCount);
            
            // Get sample profiles
            int sampleSize = Math.min(request.getSampleSize() > 0 ? request.getSampleSize() : 5, 10);
            log.info("Preview - using sampleSize: {}", sampleSize);
            
            List<Profile> sampleProfiles = profileFilterService.filterProfiles(
                    owner,
                    request.getFilters(),
                    logic,
                    0,
                    sampleSize
            );
            
            log.info("Preview - sampleProfiles retrieved: {}", sampleProfiles.size());
            
            List<Map<String, Object>> samples = sampleProfiles.stream()
                    .map(this::convertProfileToMap)
                    .collect(Collectors.toList());
            
            response.put("totalCount", totalCount);
            response.put("sampleProfiles", samples);
            response.put("filtersApplied", request.getFilters() != null ? request.getFilters().size() : 0);
            
            log.info("Preview response - totalCount: {}, sampleProfiles: {}", totalCount, samples.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to preview filters", e);
            response.put("error", "Failed to preview filters: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get available property keys for filtering.
     * Returns all unique property keys from the user's profiles.
     */
    @GetMapping("/properties")
    public ResponseEntity<Map<String, Object>> getPropertyKeys(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserAccount owner = getOwner(authentication);
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            List<String> propertyKeys = new ArrayList<>(profileFilterService.getUniquePropertyKeys(owner));
            
            // Always include 'email' as it's a direct field
            if (!propertyKeys.contains("email")) {
                propertyKeys.add(0, "email");
            }
            
            response.put("properties", propertyKeys);
            
            // Also return available operators for the UI
            response.put("operators", List.of(
                    Map.of("value", "EQUALS", "label", "equals"),
                    Map.of("value", "NOT_EQUALS", "label", "does not equal"),
                    Map.of("value", "CONTAINS", "label", "contains"),
                    Map.of("value", "NOT_CONTAINS", "label", "does not contain"),
                    Map.of("value", "STARTS_WITH", "label", "starts with"),
                    Map.of("value", "ENDS_WITH", "label", "ends with"),
                    Map.of("value", "IN", "label", "is one of"),
                    Map.of("value", "NOT_IN", "label", "is not one of"),
                    Map.of("value", "IS_EMPTY", "label", "is empty"),
                    Map.of("value", "IS_NOT_EMPTY", "label", "is not empty")
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get property keys", e);
            response.put("error", "Failed to get property keys: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get values for a specific property key (for autocomplete suggestions).
     */
    @GetMapping("/properties/{key}/values")
    public ResponseEntity<Map<String, Object>> getPropertyValues(
            @PathVariable String key,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserAccount owner = getOwner(authentication);
            if (owner == null) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            List<String> values = profileFilterService.getPropertyValues(owner, key, Math.min(limit, 100));
            
            response.put("key", key);
            response.put("values", values);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get property values", e);
            response.put("error", "Failed to get property values: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // =========================================================================
    // Helper methods
    // =========================================================================
    
    private UserAccount getOwner(Authentication authentication) {
        String username = authentication.getName();
        return userAccountRepository.findByEmail(username);
    }
    
    private Map<String, Object> convertCampaignToMap(Campaign campaign) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", campaign.getId());
        map.put("name", campaign.getName());
        map.put("subject", campaign.getSubject());
        map.put("body", campaign.getBody());
        map.put("status", campaign.getStatus().name());
        map.put("totalRecipients", campaign.getTotalRecipients());
        map.put("sentCount", campaign.getSentCount());
        map.put("failedCount", campaign.getFailedCount());
        map.put("createdAt", campaign.getCreatedAt() != null ? campaign.getCreatedAt().toString() : null);
        map.put("scheduledAt", campaign.getScheduledAt() != null ? campaign.getScheduledAt().toString() : null);
        map.put("startedAt", campaign.getStartedAt() != null ? campaign.getStartedAt().toString() : null);
        map.put("completedAt", campaign.getCompletedAt() != null ? campaign.getCompletedAt().toString() : null);
        map.put("filterLogic", campaign.getFilterLogic());
        
        // Parse and include filters
        List<FilterCriteria> filters = campaignService.getFiltersFromCampaign(campaign);
        map.put("filters", filters);
        
        return map;
    }
    
    private Map<String, Object> convertProfileToMap(Profile profile) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", profile.getId());
        map.put("email", profile.getEmail());
        
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
