package com.oneshop;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.oneshop.entity.*;
import com.oneshop.repository.*;
import com.oneshop.service.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev","test"})
class ProductWatchFlowTest extends MySqlIntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired StoreStockRepository stocks;
    @Autowired ProductWatchRepository watches;
    @Autowired ProductWatchService watchService;
    @Autowired ProductWatchDelivery delivery;
    @Autowired ProductService catalog;
    @Autowired AdminService admin;
    @Autowired CartService carts;
    @MockitoBean OrderMailService mail;
    Product product; User user; Store store; StoreStock stock;
    String email="buyer@watch.test";
    @BeforeEach void fixture(){
        reset(mail);
        user=new User();user.setEmail(email);user.setFullName("Khách thử");user.setPassword("test-not-login");users.saveAndFlush(user);
        product=new Product();product.setSku("WATCH-1");product.setSlug("son-theo-doi");product.setName("Son theo dõi");
        product.setPrice(new BigDecimal("150000"));product.setStatus(ProductStatus.OUT_OF_STOCK);products.saveAndFlush(product);
        store=new Store();store.setCode("WATCH-S1");store.setName("Cửa hàng thử");store.setAddress("12 Nguyễn Huệ");stores.saveAndFlush(store);
        stock=new StoreStock();stock.setProduct(product);stock.setStore(store);stock.setQuantity(0);stocks.saveAndFlush(stock);
    }
    void available(int quantity){var p=products.findById(product.getId()).orElseThrow();p.setStatus(ProductStatus.ACTIVE);products.saveAndFlush(p);stock.setQuantity(quantity);stock=stocks.saveAndFlush(stock);}
    @Test void soldOutRemainsVisibleAndCannotBeAddedToCart(){
        var view=catalog.searchCatalog("","",PageRequest.of(0,12));
        assertThat(view.getContent()).singleElement().satisfies(p->assertThat(p.isSoldOut()).isTrue());
        assertThat(catalog.findActiveProductBySlug(product.getSlug()).getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
        assertThatThrownBy(()->carts.addItem(email,null,product.getId(),1,"","")).isInstanceOf(com.oneshop.exception.CartOperationException.class);
        available(0);assertThat(catalog.findActiveProductBySlug(product.getSlug()).isSoldOut()).isTrue();
        available(4);assertThat(catalog.findActiveProductBySlug(product.getSlug()).isSoldOut()).isFalse();
    }
    @Test void subscriptionUsesAccountEmailAndIsIdempotent(){
        watchService.subscribe(email,product.getId());watchService.subscribe(email,product.getId());
        assertThat(watches.count()).isEqualTo(1);assertThat(watchService.list(email)).hasSize(1);
        assertThat(watchService.list("other@watch.test")).isEmpty();
    }
    @Test void mailWaitsForActualStockAndSendsOncePerSubscription() throws Exception{
        watchService.subscribe(email,product.getId(),true);Long id=watchService.list(email).getFirst().id();
        available(0);delivery.send(id);verifyNoInteractions(mail);
        available(5);when(mail.sendRestock(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(true);
        delivery.send(id);delivery.send(id);
        verify(mail,times(1)).sendRestock(eq(email),eq(user.getFullName()),eq(product.getName()),eq(product.getSlug()),anyString());
        assertThat(watches.findById(id).orElseThrow().getNotifiedAt()).isNotNull();
        var p=products.findById(product.getId()).orElseThrow();p.setStatus(ProductStatus.OUT_OF_STOCK);products.saveAndFlush(p);
        String previous=watches.findById(id).orElseThrow().getDeliveryId();watchService.subscribe(email,product.getId(),true);
        assertThat(watches.findById(id).orElseThrow().getDeliveryId()).isNotEqualTo(previous);
    }
    @Test void failedEmailRemainsPendingAndRetriesWithSameDeliveryId() throws Exception{
        watchService.subscribe(email,product.getId(),true);Long id=watchService.list(email).getFirst().id();available(3);
        when(mail.sendRestock(anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new java.io.IOException("test"));
        delivery.send(id);var pending=watches.findById(id).orElseThrow();String deliveryId=pending.getDeliveryId();
        assertThat(pending.getNotifiedAt()).isNull();delivery.send(id);
        verify(mail,times(1)).sendRestock(anyString(),anyString(),anyString(),anyString(),eq(deliveryId));
        pending.setLastAttemptAt(LocalDateTime.now().minusMinutes(6));watches.saveAndFlush(pending);
        doReturn(true).when(mail).sendRestock(anyString(),anyString(),anyString(),anyString(),anyString());delivery.send(id);
        assertThat(watches.findById(id).orElseThrow().getNotifiedAt()).isNotNull();
    }
    @Test void cancelledSubscriptionNeverReceivesMail(){
        watchService.subscribe(email,product.getId());Long id=watchService.list(email).getFirst().id();
        watchService.cancel(email,id);available(2);delivery.send(id);verifyNoInteractions(mail);
    }
    @Test @WithMockUser(roles="ADMIN") void removeFromShopPreservesProductAndStopsNotifications(){
        watchService.subscribe(email,product.getId());Long id=watchService.list(email).getFirst().id();
        admin.deactivateProduct(product.getId());assertThat(products.findById(product.getId())).isPresent();
        assertThat(catalog.searchCatalog("","",PageRequest.of(0,12))).isEmpty();
        assertThatThrownBy(()->catalog.findActiveProductBySlug(product.getSlug())).isInstanceOf(com.oneshop.exception.ProductNotFoundException.class);
        delivery.send(id);verifyNoInteractions(mail);assertThat(watchService.list(email).getFirst().discontinued()).isTrue();
    }
    @Test void watchEndpointsRequireLoginCsrfAndOwnership() throws Exception{
        mvc.perform(post("/account/watches").with(user(email)).param("productId",product.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(get("/account/watches")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/account/watches").with(user(email)).with(csrf()).param("productId",product.getId().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("watchSuccess"));
        Long id=watchService.list(email).getFirst().id();
        mvc.perform(post("/account/watches/{id}/cancel",id).with(user("other@watch.test")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/products/{id}/stock-status",product.getId()).with(user(email)).with(csrf()).param("soldOut","false")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/products/{id}/delete",product.getId()).with(user(email)).with(csrf())).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(roles="ADMIN") void adminSoldOutAndRestoreKeepStockUntouchedAndNoFalseNotification(){
        available(5);admin.markStockStatus(product.getId(),true);
        assertThat(catalog.findActiveProductBySlug(product.getSlug()).isSoldOut()).isTrue();
        assertThat(stocks.findById(stock.getId()).orElseThrow().getQuantity()).isEqualTo(5);
        watchService.subscribe(email,product.getId());Long id=watchService.list(email).getFirst().id();delivery.send(id);verifyNoInteractions(mail);
        admin.markStockStatus(product.getId(),false);
        assertThat(watches.ready(LocalDateTime.now().minusMinutes(5),PageRequest.of(0,10))).contains(id);
    }
    @Test void webOnlySubscriptionCreatesUnreadNotificationWithoutSendingEmail() throws Exception{
        watchService.subscribe(email,product.getId());Long id=watchService.list(email).getFirst().id();available(2);delivery.send(id);delivery.send(id);
        verifyNoInteractions(mail);assertThat(watchService.unread(email)).isEqualTo(1);
        mvc.perform(get("/account/watches/notifications").with(user(email))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.unread").value(1));
        mvc.perform(get("/account/watches/notifications").with(user("other@watch.test"))).andExpect(jsonPath("$.unread").value(0));
        mvc.perform(post("/account/watches/{id}/read",id).with(user("other@watch.test")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/account/watches/{id}/read",id).with(user(email)).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(watchService.unread(email)).isZero();assertThat(watchService.list(email).getFirst().available()).isTrue();
    }
    @Test void emailPreferenceIsExplicitAndCanBeDisabled() throws Exception{
        watchService.subscribe(email,product.getId());Long id=watchService.list(email).getFirst().id();
        assertThat(watches.findById(id).orElseThrow().isEmailEnabled()).isFalse();
        mvc.perform(post("/account/watches/{id}/preferences",id).with(user(email)).with(csrf()).param("emailEnabled","true"))
                .andExpect(status().is3xxRedirection());
        assertThat(watches.findById(id).orElseThrow().isEmailEnabled()).isTrue();
        mvc.perform(post("/account/watches/{id}/preferences",id).with(user(email)).with(csrf())).andExpect(status().is3xxRedirection());
        available(2);delivery.send(id);verifyNoInteractions(mail);assertThat(watchService.unread(email)).isEqualTo(1);
        watchService.preferences(email,id,true);
        when(mail.sendRestock(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(true);delivery.send(id);
        verify(mail).sendRestock(eq(email),anyString(),anyString(),anyString(),anyString());
    }

}
