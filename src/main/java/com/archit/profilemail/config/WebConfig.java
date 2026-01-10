package com.archit.profilemail.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * =============================================================================
 * WEB CONFIGURATION - Conditionally loads web-related components
 * =============================================================================
 * 
 * This configuration ONLY loads when running as a web application.
 * It enables component scanning for packages that were excluded from the 
 * main application's default scan.
 * 
 * When running with worker profile (spring.main.web-application-type=none):
 *   - This config is SKIPPED
 *   - Controllers, SecurityConfig, JWTAuthenticationFilter, AuthService are NOT loaded
 *   - Worker starts cleanly without web dependencies
 * 
 * When running as API (normal web mode):
 *   - This config is LOADED
 *   - All web components are scanned and registered
 *   - Full web functionality available
 * 
 * This is the ONLY place you need to manage web vs non-web component loading!
 * =============================================================================
 */
@Configuration
@ConditionalOnWebApplication
@Import({
    SecurityConfig.class,           // HTTP security configuration
    JWTAuthenticationFilter.class   // JWT filter for requests
})
@ComponentScan(basePackages = {
    "com.archit.profilemail.controller",   // All REST controllers
    "com.archit.profilemail.service.auth"  // Authentication service
})
public class WebConfig {
    // All web-related beans are loaded through this single configuration
    // when running as a web application (API profile)
}
