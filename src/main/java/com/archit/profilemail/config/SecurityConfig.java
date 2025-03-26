package com.archit.profilemail.config;

import com.archit.profilemail.model.User;
import com.archit.profilemail.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserRepository userRepository() {
        return new UserRepository() {
            @Override
            public User save(User user) {
                return null;
            }

            @Override
            public User findByEmail(String email) {
                return null;
            }
        };
    }
}
