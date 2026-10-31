package com.oneshop.config;

import com.oneshop.repository.ProductWatchRepository;
import com.oneshop.service.ProductWatchDelivery;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.data.domain.PageRequest;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="app.watches.enabled",havingValue="true",matchIfMissing=true)
public class ProductWatchWorker {
    private final ProductWatchRepository watches; private final ProductWatchDelivery delivery;
    public ProductWatchWorker(ProductWatchRepository watches,ProductWatchDelivery delivery){this.watches=watches;this.delivery=delivery;}
    @Scheduled(fixedDelay=60000,initialDelay=30000)
    public void poll(){
        for(Long id:watches.ready(LocalDateTime.now().minusMinutes(5),PageRequest.of(0,10))){
            try{delivery.send(id);}catch(Exception failure){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Restock job failed for subscription {} ({})",id,failure.getClass().getSimpleName());}
        }
    }
}
