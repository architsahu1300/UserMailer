package com.archit.profilemail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.authorization.AuthenticatedAuthorizationManager.anonymous;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(AbstractHttpConfigurer::disable)  // disable CSRF for testing or APIs
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/auth/user").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .httpBasic(Customizer.withDefaults());
        http
                .csrf(AbstractHttpConfigurer::disable)  // Fully disable CSRF
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // No session
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/user","/").permitAll()  // Ensure public access
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .anonymous(AbstractHttpConfigurer::disable);
//
        return http.build();
    }

    @Bean
    public UserDetailsService users() {
//        System.out.println(passwordEncoder().encode("MyrdsdbAmazon"));
        UserDetails user1 = User.withUsername("archit1")
                .password("{noop}MyrdsdbAmazon")
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user1);
    }
    }
