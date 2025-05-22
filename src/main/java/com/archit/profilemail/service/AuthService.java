package com.archit.profilemail.service;

import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(UserAccount userAccount) {
        UserAccount newUserAccount = new UserAccount();
        newUserAccount.setEmail(userAccount.getEmail());
        newUserAccount.setPassword(passwordEncoder.encode(userAccount.getPassword()));
        userRepository.save(newUserAccount);
    }

    public UserAccount findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
