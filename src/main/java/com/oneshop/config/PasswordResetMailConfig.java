package com.oneshop.config;

import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PasswordResetMailConfig {
    @Bean
    ThreadPoolTaskExecutor passwordResetMailExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); executor.setMaxPoolSize(2); executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("password-reset-mail-");
        return executor;
    }
}
