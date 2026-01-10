package com.archit.profilemail.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Campaign entity - represents an email campaign created by a user.
 * 
 * Stores the campaign configuration, content, filters, and execution status.
 */
@Entity
@Getter
@Setter
@Table(name = "campaigns")
public class Campaign {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Campaign name for identification
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * Email subject line (supports placeholders)
     */
    @Column(nullable = false)
    private String subject;
    
    /**
     * Email body content (supports placeholders)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;
    
    /**
     * JSON string storing the filter criteria
     * Format: [{"key": "city", "operator": "EQUALS", "value": "NYC"}, ...]
     */
    @Column(columnDefinition = "TEXT")
    private String filtersJson;
    
    /**
     * Filter logic: AND or OR
     */
    @Column(length = 10)
    private String filterLogic;
    
    /**
     * The user who created this campaign
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;
    
    /**
     * Campaign status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;
    
    /**
     * Number of profiles that matched the filters when campaign was created/executed
     */
    private Integer totalRecipients;
    
    /**
     * Number of emails successfully sent
     */
    private Integer sentCount;
    
    /**
     * Number of emails that failed to send
     */
    private Integer failedCount;
    
    /**
     * When the campaign was created
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * Scheduled send time (null if sent immediately)
     */
    private LocalDateTime scheduledAt;
    
    /**
     * When the campaign started sending
     */
    private LocalDateTime startedAt;
    
    /**
     * When the campaign finished sending
     */
    private LocalDateTime completedAt;
    
    /**
     * Error message if campaign failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = CampaignStatus.DRAFT;
        }
        if (sentCount == null) {
            sentCount = 0;
        }
        if (failedCount == null) {
            failedCount = 0;
        }
    }
    
    public enum CampaignStatus {
        DRAFT,          // Campaign created but not yet sent
        SCHEDULED,      // Campaign scheduled for future delivery
        QUEUED,         // Campaign is queued for processing
        PROCESSING,     // Campaign is currently being sent
        COMPLETED,      // All emails sent successfully
        PARTIALLY_SENT, // Some emails sent, some failed
        FAILED,         // Campaign failed to send
        CANCELLED       // Campaign was cancelled
    }
}
