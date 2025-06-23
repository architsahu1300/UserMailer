package com.archit.profilemail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "csvTaskExecutor")
    public Executor csvTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4); // Minimum threads
        executor.setMaxPoolSize(8);  // Max threads in pool
        executor.setQueueCapacity(500); // Queue size before rejecting new tasks
        executor.setThreadNamePrefix("CSV-Async-");
        executor.initialize();

        return executor;
    }

    @Bean(name="userTaskExecutor")
    public Executor userTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(4);
        executor.setCorePoolSize(2);
        executor.setQueueCapacity(2);
        executor.setThreadNamePrefix("User-executor-");
        executor.initialize();

        return executor;
    }
}
