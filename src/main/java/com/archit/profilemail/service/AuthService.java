package com.archit.profilemail.service;

import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder){
        this.userAccountRepository=userAccountRepository;
        this.passwordEncoder=passwordEncoder;
    }

    public void registerUser(UserAccount userAccount) {
        UserAccount newUserAccount = new UserAccount();
        newUserAccount.setEmail(userAccount.getEmail());
        newUserAccount.setPassword(passwordEncoder.encode(userAccount.getPassword()));
        userAccountRepository.save(newUserAccount);
    }

    public UserAccount authenticate(String email, String rawPassword) {
        UserAccount user = userAccountRepository.findByEmail(email);
        if(user==null){
            throw new UsernameNotFoundException("User not found");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return user;
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
