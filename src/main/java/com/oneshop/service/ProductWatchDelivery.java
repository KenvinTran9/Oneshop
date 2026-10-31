package com.oneshop.service;

import com.oneshop.entity.ProductStatus;
import com.oneshop.repository.*;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ProductWatchDelivery {
    private final ProductWatchRepository watches; private final StoreStockRepository stocks; private final OrderMailService mail;
    public ProductWatchDelivery(ProductWatchRepository watches,StoreStockRepository stocks,OrderMailService mail){this.watches=watches;this.stocks=stocks;this.mail=mail;}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void send(Long id){
        var watch=watches.lock(id).orElse(null);
        if(watch==null||!watch.isActive()||(watch.getAvailableAt()!=null&&(!watch.isEmailEnabled()||watch.getNotifiedAt()!=null))||!watch.getUser().isActive())return;
        if(watch.getLastAttemptAt()!=null&&watch.getLastAttemptAt().isAfter(LocalDateTime.now().minusMinutes(5)))return;
        var p=watch.getProduct();
        if(!p.isActive()||p.getStatus()!=ProductStatus.ACTIVE||stocks.sumAvailableQuantityByProductId(p.getId())<=0)return;
        if(watch.getAvailableAt()==null)watch.setAvailableAt(LocalDateTime.now());
        if(!watch.isEmailEnabled())return;
        watch.setLastAttemptAt(LocalDateTime.now());
        try {
            if(mail.sendRestock(watch.getUser().getEmail(),watch.getUser().getFullName(),p.getName(),p.getSlug(),watch.getDeliveryId()))
                watch.setNotifiedAt(LocalDateTime.now());
        }catch(Exception failure){
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Restock notification failed for subscription {} ({})",id,failure.getClass().getSimpleName());
        }
    }
}
