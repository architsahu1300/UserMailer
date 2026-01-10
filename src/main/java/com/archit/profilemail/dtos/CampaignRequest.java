package com.archit.profilemail.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new email campaign.
 * 
 * Contains the campaign metadata, email content, and filter criteria
 * to select which profiles should receive the email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequest {
    
    /**
     * Internal name for the campaign (for tracking/reference)
     */
    private String name;
    
    /**
     * Email subject line - supports placeholders like {{name}}
     */
    private String subject;
    
    /**
     * Email body content - supports placeholders like {{name}}, {{email}}, etc.
     */
    private String body;
    
    /**
     * List of filter criteria to apply. Multiple criteria are AND-ed together.
     * If empty or null, all profiles will be included.
     */
    private List<FilterCriteria> filters;
    
    /**
     * Logical operator for combining multiple filters (AND/OR)
     * Defaults to AND if not specified
     */
    private FilterLogic filterLogic;
    
    /**
     * Whether to send immediately or schedule for later
     */
    private boolean sendImmediately;
    
    /**
     * Scheduled send time in ISO 8601 format (if sendImmediately is false)
     */
    private String scheduledTime;
    
    public enum FilterLogic {
        AND,
        OR
    }
}
