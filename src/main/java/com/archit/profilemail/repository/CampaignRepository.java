package com.archit.profilemail.repository;

import com.archit.profilemail.model.Campaign;
import com.archit.profilemail.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    
    /**
     * Find all campaigns for a specific owner, paginated
     */
    Page<Campaign> findByOwnerOrderByCreatedAtDesc(UserAccount owner, Pageable pageable);
    
    /**
     * Find a campaign by ID and owner (for security checks)
     */
    Optional<Campaign> findByIdAndOwner(Long id, UserAccount owner);
    
    /**
     * Find campaigns with a specific status for a user
     */
    List<Campaign> findByOwnerAndStatusOrderByCreatedAtDesc(UserAccount owner, Campaign.CampaignStatus status);
    
    /**
     * Find scheduled campaigns that need to be executed
     */
    @Query("SELECT c FROM Campaign c WHERE c.status = 'SCHEDULED' AND c.scheduledAt <= :now")
    List<Campaign> findScheduledCampaignsToExecute(@Param("now") LocalDateTime now);
    
    /**
     * Count campaigns by status for a user
     */
    long countByOwnerAndStatus(UserAccount owner, Campaign.CampaignStatus status);
    
    /**
     * Get total sent count for a user
     */
    @Query("SELECT COALESCE(SUM(c.sentCount), 0) FROM Campaign c WHERE c.owner = :owner")
    long getTotalSentCountByOwner(@Param("owner") UserAccount owner);
}
