package com.archit.profilemail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ProfileMailApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileMailApplication.class, args);
    }

}
