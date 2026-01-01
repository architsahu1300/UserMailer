package com.archit.profilemail.repository;

import com.archit.profilemail.model.Profile;
import com.archit.profilemail.model.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Profile save(Profile profile);
    Optional<Profile> findByEmailAndOwner(String email, UserAccount owner);
    List<Profile> findByEmailInAndOwner(List<String> emails, UserAccount owner);
    
    // For frontend: Get paginated profiles for a user
    Page<Profile> findByOwner(UserAccount owner, Pageable pageable);
}
