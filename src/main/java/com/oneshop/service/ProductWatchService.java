package com.oneshop.service;

import com.oneshop.entity.*;
import com.oneshop.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional
public class ProductWatchService {
    public record View(Long id,String name,String slug,String image,boolean notified,boolean discontinued,boolean available,boolean unread,boolean emailEnabled){}
    private final ProductWatchRepository watches; private final UserRepository users;
    private final ProductRepository products; private final StoreStockRepository stocks;
    public ProductWatchService(ProductWatchRepository watches,UserRepository users,ProductRepository products,StoreStockRepository stocks){
        this.watches=watches;this.users=users;this.products=products;this.stocks=stocks;
    }
    public void subscribe(String email,Long productId){ subscribe(email,productId,false); }
    public void subscribe(String email,Long productId,boolean emailEnabled){
        User user=users.findForCartChange(email).filter(User::isActive).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Product product=products.findById(productId).filter(p->p.isActive()&&(p.getStatus()==ProductStatus.ACTIVE||p.getStatus()==ProductStatus.OUT_OF_STOCK))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(product.getStatus()==ProductStatus.ACTIVE&&stocks.sumAvailableQuantityByProductId(productId)>0)
            throw new IllegalArgumentException("Sản phẩm đang có hàng. Bạn có thể thêm vào giỏ ngay.");
        ProductWatch watch=watches.findByUserIdAndProductId(user.getId(),productId).orElse(null);
        if(watch!=null&&watch.isActive()&&watch.getAvailableAt()==null){
            if(!watch.isEmailEnabled()&&emailEnabled)watch.setLastAttemptAt(null);
            watch.setEmailEnabled(emailEnabled); return;
        }
        if((watch==null||!watch.isActive())&&watches.countByUserIdAndActiveTrue(user.getId())>=50)
            throw new IllegalArgumentException("Bạn có thể theo dõi tối đa 50 sản phẩm. Hãy bỏ theo dõi một sản phẩm trước.");
        if(watch==null){watch=new ProductWatch();watch.setUser(user);watch.setProduct(product);}
        watch.setActive(true);watch.setEmailEnabled(emailEnabled);watch.setAvailableAt(null);watch.setReadAt(null);watch.setNotifiedAt(null);watch.setLastAttemptAt(null);watch.setDeliveryId(UUID.randomUUID().toString());
        watches.saveAndFlush(watch);
    }
    @Transactional(readOnly=true)
    public List<View> list(String email){return watches.findByUserEmailIgnoreCaseAndActiveTrueOrderByUpdatedAtDesc(email).stream()
        .map(w->new View(w.getId(),w.getProduct().getName(),w.getProduct().getSlug(),w.getProduct().getImageUrl(),w.getNotifiedAt()!=null,
            !w.getProduct().isActive()||w.getProduct().getStatus()==ProductStatus.DISCONTINUED,w.getAvailableAt()!=null,w.getAvailableAt()!=null&&w.getReadAt()==null,w.isEmailEnabled())).toList();}
    public void cancel(String email,Long id){
        var watch=watches.lock(id).filter(w->w.getUser().getEmail().equalsIgnoreCase(email))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        watch.setActive(false);
    }
    @Transactional(readOnly=true)
    public long unread(String email){return watches.countByUserEmailIgnoreCaseAndActiveTrueAndAvailableAtIsNotNullAndReadAtIsNull(email);}
    private ProductWatch owned(String email,Long id){return watches.lock(id).filter(w->w.isActive()&&w.getUser().getEmail().equalsIgnoreCase(email))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    public void read(String email,Long id){var w=owned(email,id);if(w.getAvailableAt()!=null&&w.getReadAt()==null)w.setReadAt(LocalDateTime.now());}
    public void preferences(String email,Long id,boolean emailEnabled){
        var w=owned(email,id);if(!w.isEmailEnabled()&&emailEnabled)w.setLastAttemptAt(null);w.setEmailEnabled(emailEnabled);
    }

}
