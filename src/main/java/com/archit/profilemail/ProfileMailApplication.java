package com.archit.profilemail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application entry point.
 * 
 * IMPORTANT: We exclude controller and web-security packages from default scanning.
 * These are conditionally loaded by WebConfig only when running as a web application.
 * This allows the worker profile to start without web dependencies.
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(
    basePackages = "com.archit.profilemail",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.archit\\.profilemail\\.(controller|config\\.Security|config\\.JWTAuthentication|service\\.auth).*"
    )
)
public class ProfileMailApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileMailApplication.class, args);
    }

}
