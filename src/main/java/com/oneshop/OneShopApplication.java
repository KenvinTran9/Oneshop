package com.oneshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class OneShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(OneShopApplication.class, args);
    }
}
