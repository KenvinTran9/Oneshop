package com.oneshop.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

@Configuration
public class FlashMapConfig {
    @Bean
    SessionFlashMapManager flashMapManager() {
        return new SessionFlashMapManager() {
            @Override
            protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                // Redirect notices belong to HTML requests, never images/scripts/styles or the JWT API.
                if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                        || path.startsWith("/webjars/") || path.startsWith("/api/") || path.equals("/healthz")) return null;
                return super.retrieveFlashMaps(request);
            }
        };
    }
}
