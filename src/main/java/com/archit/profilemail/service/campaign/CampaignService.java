package com.archit.profilemail.service.campaign;

import com.archit.profilemail.dtos.CampaignRequest;
import com.archit.profilemail.dtos.FilterCriteria;
import com.archit.profilemail.model.Campaign;
import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.CampaignRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing email campaigns.
 * 
 * Handles campaign creation, scheduling, and execution.
 * Uses ProfileFilterService to determine which profiles receive emails.
 */
@Service
public class CampaignService {
    
    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);
    
    private final CampaignRepository campaignRepository;
    private final ProfileFilterService profileFilterService;
    private final ObjectMapper objectMapper;
    
    public CampaignService(
            CampaignRepository campaignRepository,
            ProfileFilterService profileFilterService,
            ObjectMapper objectMapper
    ) {
        this.campaignRepository = campaignRepository;
        this.profileFilterService = profileFilterService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create a new campaign.
     */
    @Transactional
    public Campaign createCampaign(UserAccount owner, CampaignRequest request) {
        Campaign campaign = new Campaign();
        campaign.setName(request.getName());
        campaign.setSubject(request.getSubject());
        campaign.setBody(request.getBody());
        campaign.setOwner(owner);
        
        // Store filters as JSON
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            try {
                campaign.setFiltersJson(objectMapper.writeValueAsString(request.getFilters()));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize filters", e);
            }
        }
        
        campaign.setFilterLogic(
                request.getFilterLogic() != null 
                        ? request.getFilterLogic().name() 
                        : CampaignRequest.FilterLogic.AND.name()
        );
        
        // Count recipients
        long recipientCount = profileFilterService.countFilteredProfiles(
                owner,
                request.getFilters(),
                request.getFilterLogic() != null ? request.getFilterLogic() : CampaignRequest.FilterLogic.AND
        );
        campaign.setTotalRecipients((int) recipientCount);
        
        // Set status and schedule
        if (request.isSendImmediately()) {
            campaign.setStatus(Campaign.CampaignStatus.QUEUED);
        } else if (request.getScheduledTime() != null) {
            campaign.setScheduledAt(LocalDateTime.parse(request.getScheduledTime(), DateTimeFormatter.ISO_DATE_TIME));
            campaign.setStatus(Campaign.CampaignStatus.SCHEDULED);
        } else {
            campaign.setStatus(Campaign.CampaignStatus.DRAFT);
        }
        
        Campaign saved = campaignRepository.save(campaign);
        log.info("Created campaign {} with {} recipients", saved.getId(), recipientCount);
        
        // If sending immediately, trigger async processing
        if (request.isSendImmediately()) {
            processCampaignAsync(saved.getId());
        }
        
        return saved;
    }
    
    /**
     * Get a campaign by ID for a specific owner.
     */
    public Optional<Campaign> getCampaign(Long campaignId, UserAccount owner) {
        return campaignRepository.findByIdAndOwner(campaignId, owner);
    }
    
    /**
     * Get all campaigns for a user, paginated.
     */
    public Page<Campaign> getCampaigns(UserAccount owner, Pageable pageable) {
        return campaignRepository.findByOwnerOrderByCreatedAtDesc(owner, pageable);
    }
    
    /**
     * Cancel a campaign (only if it's in DRAFT or SCHEDULED status).
     */
    @Transactional
    public boolean cancelCampaign(Long campaignId, UserAccount owner) {
        Optional<Campaign> campaignOpt = campaignRepository.findByIdAndOwner(campaignId, owner);
        if (campaignOpt.isEmpty()) {
            return false;
        }
        
        Campaign campaign = campaignOpt.get();
        if (campaign.getStatus() == Campaign.CampaignStatus.DRAFT ||
            campaign.getStatus() == Campaign.CampaignStatus.SCHEDULED) {
            campaign.setStatus(Campaign.CampaignStatus.CANCELLED);
            campaignRepository.save(campaign);
            log.info("Cancelled campaign {}", campaignId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get filter criteria from a campaign.
     */
    public List<FilterCriteria> getFiltersFromCampaign(Campaign campaign) {
        if (campaign.getFiltersJson() == null || campaign.getFiltersJson().isEmpty()) {
            return List.of();
        }
        
        try {
            return objectMapper.readValue(
                    campaign.getFiltersJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FilterCriteria.class)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize filters for campaign {}", campaign.getId(), e);
            return List.of();
        }
    }
    
    /**
     * Get profiles for a campaign based on its filters.
     */
    public List<Profile> getCampaignRecipients(Campaign campaign, int offset, int limit) {
        List<FilterCriteria> filters = getFiltersFromCampaign(campaign);
        CampaignRequest.FilterLogic logic = campaign.getFilterLogic() != null
                ? CampaignRequest.FilterLogic.valueOf(campaign.getFilterLogic())
                : CampaignRequest.FilterLogic.AND;
        
        return profileFilterService.filterProfiles(
                campaign.getOwner(),
                filters,
                logic,
                offset,
                limit
        );
    }
    
    /**
     * Process a campaign asynchronously.
     * This method will be enhanced with actual email sending logic.
     */
    @Async
    public void processCampaignAsync(Long campaignId) {
        log.info("Starting async processing of campaign {}", campaignId);
        
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            log.error("Campaign {} not found for processing", campaignId);
            return;
        }
        
        try {
            campaign.setStatus(Campaign.CampaignStatus.PROCESSING);
            campaign.setStartedAt(LocalDateTime.now());
            campaignRepository.save(campaign);
            
            // Get all recipients in batches
            int batchSize = 1000;
            int offset = 0;
            int totalSent = 0;
            int totalFailed = 0;
            
            while (true) {
                List<Profile> recipients = getCampaignRecipients(campaign, offset, batchSize);
                if (recipients.isEmpty()) {
                    break;
                }
                
                for (Profile profile : recipients) {
                    try {
                        // TODO: Send email using notification service
                        // For now, just simulate sending
                        log.debug("Would send email to {} for campaign {}", profile.getEmail(), campaignId);
                        totalSent++;
                    } catch (Exception e) {
                        log.error("Failed to send email to {} for campaign {}", profile.getEmail(), campaignId, e);
                        totalFailed++;
                    }
                }
                
                // Update progress
                campaign.setSentCount(totalSent);
                campaign.setFailedCount(totalFailed);
                campaignRepository.save(campaign);
                
                offset += batchSize;
            }
            
            // Mark as completed
            campaign.setCompletedAt(LocalDateTime.now());
            if (totalFailed == 0) {
                campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
            } else if (totalSent == 0) {
                campaign.setStatus(Campaign.CampaignStatus.FAILED);
            } else {
                campaign.setStatus(Campaign.CampaignStatus.PARTIALLY_SENT);
            }
            campaignRepository.save(campaign);
            
            log.info("Completed campaign {} - sent: {}, failed: {}", campaignId, totalSent, totalFailed);
            
        } catch (Exception e) {
            log.error("Campaign {} failed", campaignId, e);
            campaign.setStatus(Campaign.CampaignStatus.FAILED);
            campaign.setErrorMessage(e.getMessage());
            campaign.setCompletedAt(LocalDateTime.now());
            campaignRepository.save(campaign);
        }
    }
    
    /**
     * Get campaign statistics for a user.
     */
    public CampaignStats getStats(UserAccount owner) {
        return new CampaignStats(
                campaignRepository.countByOwnerAndStatus(owner, Campaign.CampaignStatus.COMPLETED),
                campaignRepository.countByOwnerAndStatus(owner, Campaign.CampaignStatus.SCHEDULED),
                campaignRepository.countByOwnerAndStatus(owner, Campaign.CampaignStatus.DRAFT),
                campaignRepository.getTotalSentCountByOwner(owner)
        );
    }
    
    public record CampaignStats(long completed, long scheduled, long drafts, long totalSent) {}
    
    // =========================================================================
    // SCHEDULER - Executes scheduled campaigns
    // =========================================================================
    
    /**
     * Scheduled job that runs every minute to check for campaigns
     * that are scheduled to be sent and executes them.
     * 
     * How it works:
     * 1. Runs every 60 seconds (fixedRate = 60000 ms)
     * 2. Queries for campaigns with status=SCHEDULED and scheduledAt <= now
     * 3. For each matching campaign, triggers async processing
     * 4. Updates status to QUEUED to prevent duplicate execution
     */
    @Scheduled(fixedRate = 60000)  // Run every 60 seconds
    @Transactional
    public void executeScheduledCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        List<Campaign> campaignsToExecute = campaignRepository.findScheduledCampaignsToExecute(now);
        
        if (!campaignsToExecute.isEmpty()) {
            log.info("Found {} scheduled campaigns ready to execute", campaignsToExecute.size());
        }
        
        for (Campaign campaign : campaignsToExecute) {
            try {
                log.info("Executing scheduled campaign {} (scheduled for {})", 
                        campaign.getId(), campaign.getScheduledAt());
                
                // Update status to QUEUED to prevent duplicate execution
                campaign.setStatus(Campaign.CampaignStatus.QUEUED);
                campaignRepository.save(campaign);
                
                // Trigger async processing
                processCampaignAsync(campaign.getId());
                
            } catch (Exception e) {
                log.error("Failed to execute scheduled campaign {}", campaign.getId(), e);
                campaign.setStatus(Campaign.CampaignStatus.FAILED);
                campaign.setErrorMessage("Failed to start: " + e.getMessage());
                campaignRepository.save(campaign);
            }
        }
    }
}
