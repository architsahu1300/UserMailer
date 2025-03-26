package com.archit.profilemail.repository;

import com.archit.profilemail.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User save(User user);
    User findByEmail(String email);
}
