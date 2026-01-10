package com.archit.profilemail.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for previewing filter results before creating a campaign.
 * Returns a count and sample of profiles that match the filters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterPreviewRequest {
    
    /**
     * List of filter criteria to apply
     */
    private List<FilterCriteria> filters;
    
    /**
     * Logical operator for combining filters (AND/OR)
     */
    private CampaignRequest.FilterLogic filterLogic;
    
    /**
     * Number of sample profiles to return for preview (max 10)
     */
    private int sampleSize;
}
