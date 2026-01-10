package com.archit.profilemail.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single filter condition for profile properties.
 * 
 * Examples:
 * - { key: "city", operator: "EQUALS", value: "New York" }
 * - { key: "age", operator: "GREATER_THAN", value: "25" }
 * - { key: "subscription", operator: "IN", value: "premium,enterprise" }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCriteria {
    
    private String key;
    private FilterOperator operator;
    private String value;
    
    public enum FilterOperator {
        EQUALS,
        NOT_EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN_OR_EQUALS,
        IN,           // value is comma-separated list
        NOT_IN,       // value is comma-separated list
        IS_EMPTY,
        IS_NOT_EMPTY
    }
}
