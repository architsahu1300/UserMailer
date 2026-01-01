package com.archit.profilemail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * =============================================================================
 * ASYNC AND SCHEDULING CONFIGURATION
 * =============================================================================
 * This configuration enables:
 * 
 * @EnableAsync - Allows methods annotated with @Async to run in background threads
 *                Used by BatchProcessorService for parallel batch inserts
 * 
 * @EnableScheduling - Allows methods annotated with @Scheduled to run periodically
 *                     Used by CSVJobConsumerService to poll Redis for jobs
 * =============================================================================
 */
@Configuration
@EnableAsync
@EnableScheduling  // Required for @Scheduled in CSVJobConsumerService
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

    @Bean(name = "ownerNotificationExecutor")
    public Executor ownerNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // for owner emails, lightweight
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("EmailNotif-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "profileEmailExecutor")
    public Executor profileEmailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4); // for profile emailing, heavier
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ProfileEmail-");
        executor.initialize();
        return executor;
    }
}
