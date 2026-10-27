package com.oneshop.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"prod & dev", "production & dev"})
public class ProductionProfileGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionProfileGuard.class);

    @PostConstruct
    void rejectDevelopmentProfileInProduction() {
        String message = "Unsafe profile combination: dev must not be active with prod or production";
        LOGGER.error(message);
        throw new IllegalStateException(message);
    }
}
