package com.archit.profilemail.config;

import com.archit.profilemail.service.auth.AuthService;
import com.archit.profilemail.utils.JWTUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public JWTAuthenticationFilter jwtAuthenticationFilter(JWTUtils jwtUtils, AuthService authService) {
        return new JWTAuthenticationFilter(jwtUtils, authService);
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * CORS Configuration - Allows the React frontend to communicate with this backend.
     * CORS (Cross-Origin Resource Sharing) is a security feature that browsers enforce.
     * Without this, requests from localhost:3000 (frontend) to localhost:8080 (backend) would be blocked.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow requests from the React dev server
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://127.0.0.1:3000"));
        // Allow these HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allow these headers in requests
        configuration.setAllowedHeaders(List.of("*"));
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JWTAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // Enable CORS
                .csrf(AbstractHttpConfigurer::disable)  // Fully disable CSRF
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // No session
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/user","/auth/login","/").permitAll()  // Ensure public access
                        .requestMatchers(HttpMethod.POST,"/api/import/csv").authenticated()  // Changed to authenticated
                        .requestMatchers("/api/**").authenticated()  // All API endpoints need authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(AbstractHttpConfigurer::disable)  // Disable HTTP Basic - we use JWT
                .formLogin(AbstractHttpConfigurer::disable)  // Disable form login - we use JWT
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//                .anonymous(AbstractHttpConfigurer::disable);
//        http
//                .csrf(AbstractHttpConfigurer::disable)
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))  // No session
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/**").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .httpBasic(Customizer.withDefaults());
//        http
//                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
//                .formLogin(form -> form
//                        .permitAll()
//                        .defaultSuccessUrl("/welcome", true)) // force redirect here always
//                .logout(LogoutConfigurer::permitAll);

        return http.build();
    }
    }
