package com.archit.profilemail.config;

import com.archit.profilemail.service.AuthService;
import com.archit.profilemail.utils.JWTUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.authorization.AuthenticatedAuthorizationManager.anonymous;
import static org.springframework.security.config.Customizer.withDefaults;

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,JWTAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)  // Fully disable CSRF
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // No session
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/user","/").permitAll()  // Ensure public access
                        .requestMatchers(HttpMethod.POST,"/api/import/csv").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
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

//    @Bean
//    public UserDetailsService users() {
//        UserDetails user1 = User.withUsername("archit")
//                .password("{noop}pass")
//                .roles("USER")
//                .build();
//        return new InMemoryUserDetailsManager(user1);
//    }
    }
