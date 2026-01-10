package com.archit.profilemail.service.campaign;

import com.archit.profilemail.dtos.CampaignRequest;
import com.archit.profilemail.dtos.FilterCriteria;
import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.UserAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for filtering profiles based on their properties.
 * Uses JPA Criteria API for dynamic query building.
 */
@Service
public class ProfileFilterService {
    
    private static final Logger log = LoggerFactory.getLogger(ProfileFilterService.class);
    
    private final EntityManager entityManager;
    
    public ProfileFilterService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Get profiles matching the given filter criteria.
     * 
     * @param owner The profile owner
     * @param filters List of filter criteria
     * @param filterLogic AND or OR logic for combining filters
     * @param offset Starting index for pagination
     * @param limit Maximum number of results
     * @return List of matching profiles
     */
    public List<Profile> filterProfiles(
            UserAccount owner,
            List<FilterCriteria> filters,
            CampaignRequest.FilterLogic filterLogic,
            int offset,
            int limit
    ) {
        // If no filters, use simple JPQL for reliability
        if (filters == null || filters.isEmpty()) {
            String jpql = "SELECT p FROM Profile p WHERE p.owner.id = :ownerId ORDER BY p.id";
            List<Profile> results = entityManager.createQuery(jpql, Profile.class)
                    .setParameter("ownerId", owner.getId())
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .getResultList();
            log.debug("filterProfiles (no filters): ownerId={}, offset={}, limit={}, returned={}", 
                    owner.getId(), offset, limit, results.size());
            return results;
        }
        
        // For filtered queries, use Criteria API
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Profile> query = cb.createQuery(Profile.class);
        Root<Profile> profile = query.from(Profile.class);
        
        // Build the where clause
        Predicate predicate = buildFilterPredicate(cb, query, profile, owner, filters, filterLogic);
        query.where(predicate);
        query.distinct(true);
        
        // Execute query with pagination
        TypedQuery<Profile> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(offset);
        typedQuery.setMaxResults(limit);
        
        return typedQuery.getResultList();
    }
    
    /**
     * Count profiles matching the given filter criteria.
     */
    public long countFilteredProfiles(
            UserAccount owner,
            List<FilterCriteria> filters,
            CampaignRequest.FilterLogic filterLogic
    ) {
        // If no filters, use a simple JPQL count for reliability
        if (filters == null || filters.isEmpty()) {
            String jpql = "SELECT COUNT(p) FROM Profile p WHERE p.owner.id = :ownerId";
            Long count = entityManager.createQuery(jpql, Long.class)
                    .setParameter("ownerId", owner.getId())
                    .getSingleResult();
            log.info("countFilteredProfiles (no filters): ownerId={}, count={}", owner.getId(), count);
            return count;
        }
        
        // For filtered queries, use Criteria API
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Profile> profile = query.from(Profile.class);
        
        query.select(cb.countDistinct(profile));
        
        Predicate predicate = buildFilterPredicate(cb, query, profile, owner, filters, filterLogic);
        query.where(predicate);
        
        long count = entityManager.createQuery(query).getSingleResult();
        log.info("countFilteredProfiles (with filters): ownerId={}, filters={}, count={}", 
                owner.getId(), filters.size(), count);
        return count;
    }
    
    /**
     * Get all unique property keys for a user's profiles.
     * Used for populating filter dropdowns in the UI.
     */
    public List<String> getUniquePropertyKeys(UserAccount owner) {
        String jpql = """
            SELECT DISTINCT pp.key 
            FROM ProfileProperty pp 
            WHERE pp.profile.owner = :owner 
            ORDER BY pp.key
        """;
        
        return entityManager.createQuery(jpql, String.class)
                .setParameter("owner", owner)
                .getResultList();
    }
    
    /**
     * Get unique values for a specific property key.
     * Used for populating value suggestions in the UI.
     */
    public List<String> getPropertyValues(UserAccount owner, String propertyKey, int limit) {
        String jpql = """
            SELECT DISTINCT pp.value 
            FROM ProfileProperty pp 
            WHERE pp.profile.owner = :owner AND pp.key = :key
            ORDER BY pp.value
        """;
        
        return entityManager.createQuery(jpql, String.class)
                .setParameter("owner", owner)
                .setParameter("key", propertyKey)
                .setMaxResults(limit)
                .getResultList();
    }
    
    /**
     * Build the filter predicate using Criteria API.
     */
    private Predicate buildFilterPredicate(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<Profile> profile,
            UserAccount owner,
            List<FilterCriteria> filters,
            CampaignRequest.FilterLogic filterLogic
    ) {
        // Always filter by owner ID (more reliable than entity comparison)
        Predicate ownerPredicate = cb.equal(profile.get("owner").get("id"), owner.getId());
        
        if (filters == null || filters.isEmpty()) {
            return ownerPredicate;
        }
        
        // Build individual filter predicates
        List<Predicate> filterPredicates = new ArrayList<>();
        
        for (FilterCriteria filter : filters) {
            Predicate filterPredicate = buildSingleFilterPredicate(cb, query, profile, filter);
            if (filterPredicate != null) {
                filterPredicates.add(filterPredicate);
            }
        }
        
        if (filterPredicates.isEmpty()) {
            return ownerPredicate;
        }
        
        // Combine filter predicates based on logic
        Predicate combinedFilters;
        if (filterLogic == CampaignRequest.FilterLogic.OR) {
            combinedFilters = cb.or(filterPredicates.toArray(new Predicate[0]));
        } else {
            combinedFilters = cb.and(filterPredicates.toArray(new Predicate[0]));
        }
        
        return cb.and(ownerPredicate, combinedFilters);
    }
    
    /**
     * Build a predicate for a single filter criteria.
     * Uses a subquery to match profile properties.
     */
    private Predicate buildSingleFilterPredicate(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<Profile> profile,
            FilterCriteria filter
    ) {
        if (filter.getKey() == null || filter.getOperator() == null) {
            return null;
        }
        
        // Handle special case: filtering by email (which is on Profile, not ProfileProperty)
        if ("email".equalsIgnoreCase(filter.getKey())) {
            return buildEmailFilterPredicate(cb, profile, filter);
        }
        
        // For property filters, use a subquery
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<com.archit.profilemail.model.ProfileProperty> property = 
                subquery.from(com.archit.profilemail.model.ProfileProperty.class);
        
        subquery.select(property.get("profile").get("id"));
        
        // Match property key
        Predicate keyPredicate = cb.equal(cb.lower(property.get("key")), filter.getKey().toLowerCase());
        
        // Build value predicate based on operator
        Predicate valuePredicate = buildValuePredicate(cb, property.get("value"), filter);
        
        if (valuePredicate == null) {
            return null;
        }
        
        subquery.where(cb.and(keyPredicate, valuePredicate));
        
        return profile.get("id").in(subquery);
    }
    
    /**
     * Build predicate for email field filtering.
     */
    private Predicate buildEmailFilterPredicate(
            CriteriaBuilder cb,
            Root<Profile> profile,
            FilterCriteria filter
    ) {
        Expression<String> emailPath = profile.get("email");
        return buildValuePredicate(cb, emailPath, filter);
    }
    
    /**
     * Build value comparison predicate based on operator.
     */
    private Predicate buildValuePredicate(
            CriteriaBuilder cb,
            Expression<String> path,
            FilterCriteria filter
    ) {
        String value = filter.getValue();
        
        return switch (filter.getOperator()) {
            case EQUALS -> cb.equal(cb.lower(path), value != null ? value.toLowerCase() : null);
            
            case NOT_EQUALS -> cb.notEqual(cb.lower(path), value != null ? value.toLowerCase() : null);
            
            case CONTAINS -> cb.like(cb.lower(path), "%" + (value != null ? value.toLowerCase() : "") + "%");
            
            case NOT_CONTAINS -> cb.notLike(cb.lower(path), "%" + (value != null ? value.toLowerCase() : "") + "%");
            
            case STARTS_WITH -> cb.like(cb.lower(path), (value != null ? value.toLowerCase() : "") + "%");
            
            case ENDS_WITH -> cb.like(cb.lower(path), "%" + (value != null ? value.toLowerCase() : ""));
            
            case GREATER_THAN -> cb.greaterThan(path, value);
            
            case LESS_THAN -> cb.lessThan(path, value);
            
            case GREATER_THAN_OR_EQUALS -> cb.greaterThanOrEqualTo(path, value);
            
            case LESS_THAN_OR_EQUALS -> cb.lessThanOrEqualTo(path, value);
            
            case IN -> {
                if (value == null || value.isEmpty()) {
                    yield null;
                }
                List<String> values = Arrays.stream(value.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                yield cb.lower(path).in(values);
            }
            
            case NOT_IN -> {
                if (value == null || value.isEmpty()) {
                    yield null;
                }
                List<String> values = Arrays.stream(value.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                yield cb.not(cb.lower(path).in(values));
            }
            
            case IS_EMPTY -> cb.or(cb.isNull(path), cb.equal(path, ""));
            
            case IS_NOT_EMPTY -> cb.and(cb.isNotNull(path), cb.notEqual(path, ""));
        };
    }
}
