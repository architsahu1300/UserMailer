package com.archit.profilemail.repository;

import com.archit.profilemail.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserAccount, Long> {
    UserAccount save(UserAccount userAccount);
    UserAccount findByEmail(String email);
}
