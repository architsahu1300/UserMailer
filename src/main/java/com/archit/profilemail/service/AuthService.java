package com.archit.profilemail.service;

import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserDetailsService {
    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(UserAccount userAccount) {
        UserAccount newUserAccount = new UserAccount();
        newUserAccount.setEmail(userAccount.getEmail());
        newUserAccount.setPassword(passwordEncoder.encode(userAccount.getPassword()));
        userAccountRepository.save(newUserAccount);
    }

    public UserAccount findUserByEmail(String email) {
        return userAccountRepository.findByEmail(email);
    }

    @Override
    public UserAccount loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = findUserByEmail(username);
        if(user==null){
            throw new UsernameNotFoundException("No user was found with the given username");
        }
        return user;
    }
}
