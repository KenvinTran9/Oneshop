package com.oneshop;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.oneshop.config.RoleInitializer;
import com.oneshop.dto.request.*;
import com.oneshop.entity.*;
import com.oneshop.repository.*;
import com.oneshop.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev","test"})
class ChainManagementTest extends MySqlIntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired RoleInitializer seed;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired StoreRepository stores;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired StoreStockRepository stocks;
    @Autowired StockMovementRepository movements;
    @Autowired InventoryService inventory;
    @Autowired OrderService orders;
    @Autowired CartService carts;
    @Autowired SalesReportService reports;
    @Autowired AccountManagementService accounts;
    @Autowired CustomerProfileService profiles;
    @Autowired PasswordResetAccountRepository credentials;
    @Autowired PasswordEncoder passwords;
    @Autowired AdminService admin;
    Store first,second;
    Product product;
    User customer;
    @BeforeEach void fixture(){
        seed.run();first=store("CN1");second=store("CN2");
        account("admin@chain.test","ADMIN",null);account("manager@chain.test","BRANCH_MANAGER",first);
        customer=account("buyer@chain.test","CUSTOMER",null);
        product=new Product();product.setName("Son thử nghiệm");product.setSku("CHAIN-001");product.setSlug("son-chain");product.setPrice(new BigDecimal("100000"));products.saveAndFlush(product);
        for(Store store:List.of(first,second)){StoreStock stock=new StoreStock();stock.setStore(store);stock.setProduct(product);stock.setQuantity(10);stocks.saveAndFlush(stock);}
    }
    Store store(String code){Store s=new Store();s.setCode(code);s.setName("OneShop "+code);s.setAddress("123 Nguyễn Huệ, TP.HCM");return stores.saveAndFlush(s);}
    User account(String email,String role,Store store){User u=new User();u.setEmail(email);u.setPassword(passwords.encode("Password123!"));u.setFullName("Khách thử");u.setPhone("0901234567");u.setRole(roles.findByName(role).orElseThrow());u.setStore(store);return users.saveAndFlush(u);}
    InventoryChangeForm receipt(Store s,int delta){var f=new InventoryChangeForm();f.setStoreId(s.getId());f.setProductId(product.getId());f.setDelta(delta);f.setReason("Nhập hàng kiểm thử");f.setRequestToken(UUID.randomUUID().toString());return f;}
    String place(){carts.addItem(customer.getEmail(),null,product.getId(),2,null,null);CheckoutForm f=new CheckoutForm();f.setCheckoutToken(UUID.randomUUID().toString());f.setCustomerName("Khách mua");f.setCustomerPhone("0901234567");f.setShippingAddress("123 Nguyễn Huệ, Quận 1, TP.HCM");return orders.place(customer.getEmail(),f);}
    void login(String email,String role){SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(email,"unused",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+role))));}

    @Test @WithMockUser(username="admin@chain.test",roles="ADMIN")
    void receiptIsIdempotentAndNegativeCorrectionRollsBack(){
        var f=receipt(first,5);inventory.change(f);inventory.change(f);
        assertThat(stocks.findByStoreIdAndProductId(first.getId(),product.getId()).orElseThrow().getQuantity()).isEqualTo(15);
        assertThat(movements.count()).isEqualTo(1);
        assertThatThrownBy(()->inventory.change(receipt(first,-16))).isInstanceOf(IllegalArgumentException.class);
        assertThat(stocks.findByStoreIdAndProductId(first.getId(),product.getId()).orElseThrow().getQuantity()).isEqualTo(15);
    }
    @Test @WithMockUser(username="admin@chain.test",roles="ADMIN")
    void newProductCanReceiveStockThenBePurchased(){
        Product p=new Product();p.setName("Kem dưỡng mới");p.setSku("NEW-001");p.setSlug("kem-moi");p.setPrice(new BigDecimal("200000"));products.saveAndFlush(p);
        var f=receipt(second,3);f.setProductId(p.getId());inventory.change(f);
        carts.addItem(customer.getEmail(),null,p.getId(),1,null,null);
        CheckoutForm form=new CheckoutForm();form.setCheckoutToken(UUID.randomUUID().toString());form.setCustomerName("Khách");form.setCustomerPhone("0901234567");form.setShippingAddress("123 Nguyễn Huệ, Quận 1");
        String code=orders.place(customer.getEmail(),form);assertThat(orders.forAdmin(code).storeName()).isEqualTo(second.getName());
        assertThat(stocks.findByStoreIdAndProductId(second.getId(),p.getId()).orElseThrow().getQuantity()).isEqualTo(2);
    }
    @Test @WithMockUser(username="manager@chain.test",roles="BRANCH_MANAGER")
    void managerCannotReadOrWriteAnotherStore(){
        assertThat(inventory.allowedStores()).extracting(Store::getId).containsExactly(first.getId());
        assertThatThrownBy(()->inventory.inventory(second.getId(),"",false,0)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->inventory.change(receipt(second,3))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->reports.report(LocalDate.now(),LocalDate.now(),second.getId())).isInstanceOf(AccessDeniedException.class);
    }
    @Test @WithMockUser(username="admin@chain.test",roles="ADMIN")
    void onlyCompletedOrdersAppearInRevenueAndExcelIsAWorkbook() throws Exception {
        String code=place();var today=LocalDate.now();assertThat(reports.report(today,today,null).revenue()).isEqualByComparingTo("0");
        for(OrderStatus next:List.of(OrderStatus.CONFIRMED,OrderStatus.PACKING,OrderStatus.SHIPPING,OrderStatus.COMPLETED))orders.updateStatus(code,next);
        var report=reports.report(today,today,null);assertThat(report.completed()).isEqualTo(1);assertThat(report.revenue()).isEqualByComparingTo("200000");assertThat(report.products().getFirst().quantity()).isEqualTo(2);
        assertThat(reports.report(today,today,second.getId()).completed()).isZero();
        byte[] bytes=SalesReportWorkbook.write(report);assertThat(bytes[0]).isEqualTo((byte)'P');assertThat(bytes[1]).isEqualTo((byte)'K');
        try(var zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))){boolean found=false;java.util.zip.ZipEntry e;while((e=zip.getNextEntry())!=null){if(e.getName().endsWith("sheet1.xml")){found=true;assertThat(new String(zip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).contains("200000");}}assertThat(found).isTrue();}
    }
    @Test @WithMockUser(username="admin@chain.test",roles="ADMIN")
    void assignmentAndLockInvalidateExistingCredentials(){
        accounts.update(customer.getId(),"BRANCH_MANAGER",second.getId(),true);
        assertThat(credentials.findCredentialVersion(customer.getEmail()).orElseThrow()).isEqualTo(1);
        accounts.update(customer.getId(),"BRANCH_MANAGER",second.getId(),false);
        assertThat(users.findById(customer.getId()).orElseThrow().isActive()).isFalse();
        assertThat(credentials.findCredentialVersion(customer.getEmail()).orElseThrow()).isEqualTo(2);
        assertThatThrownBy(()->accounts.update(users.findByEmail("admin@chain.test").orElseThrow().getId(),"CUSTOMER",null,false)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test @WithMockUser(username="buyer@chain.test")
    void savedAddressesAreOwnedAndPasswordChangeRevokesSessions(){
        profiles.addAddress(customer.getEmail(),new AddressForm("Nhà","Khách","0901234567","123 Nguyễn Huệ, Quận 1"));
        Long id=profiles.addresses(customer.getEmail()).getFirst().getId();
        assertThatThrownBy(()->profiles.address("manager@chain.test",id)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->profiles.changePassword(customer.getEmail(),new ChangePasswordForm("wrong","NewPassword123!","NewPassword123!"))).isInstanceOf(IllegalArgumentException.class);
        profiles.changePassword(customer.getEmail(),new ChangePasswordForm("Password123!","NewPassword123!","NewPassword123!"));
        assertThat(passwords.matches("NewPassword123!",users.findById(customer.getId()).orElseThrow().getPassword())).isTrue();assertThat(credentials.findCredentialVersion(customer.getEmail()).orElseThrow()).isEqualTo(1);
    }
    @Test @WithMockUser(username="admin@chain.test",roles="ADMIN")
    void adminPagesRenderAndMutationsRequireCsrf() throws Exception {
        for(String url:List.of("/admin/inventory","/admin/stores","/admin/stores/new","/admin/users","/admin/reports","/admin/products/new","/account/profile"))mvc.perform(get(url)).andExpect(status().isOk());
        mvc.perform(post("/admin/users/"+customer.getId()).param("role","CUSTOMER")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(username="manager@chain.test",roles="BRANCH_MANAGER")
    void branchPagesRenderAndAdminRoutesAreForbidden() throws Exception {
        for(String url:List.of("/branch/inventory","/branch/orders","/branch/reports"))mvc.perform(get(url)).andExpect(status().isOk());
        mvc.perform(get("/admin/users")).andExpect(status().isForbidden());
        mvc.perform(get("/branch/inventory").param("storeId",second.getId().toString())).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(username="admin@chain.test",roles="ADMIN")
    void cosmeticOptionsPersistAndInvalidImageUrlsAreRejected(){
        Category c=new Category();c.setName("Son môi");c.setSlug("son-moi");categories.saveAndFlush(c);
        AdminProductForm f=admin.editProductForm(product.getId());f.setCategoryId(c.getId());f.setSizesText("3g\n5g");f.setShadesText("Đỏ gạch | #AA3344 | https://example.com/red.jpg");f.setIngredients("Thành phần thử nghiệm");admin.saveProduct(product.getId(),f);
        assertThat(admin.editProductForm(product.getId()).getShadesText()).contains("Đỏ gạch");
        f.setShadesText("Đỏ | #AA3344 | javascript:alert(1)");assertThatThrownBy(()->admin.saveProduct(product.getId(),f)).isInstanceOf(com.oneshop.exception.InvalidProductReferenceException.class);
    }
    @Test @WithMockUser(username="buyer@chain.test")
    void addressAndProfileFormsBindAndCheckoutUsesOwnedAddress() throws Exception {
        mvc.perform(post("/account/profile").with(csrf()).param("fullName","Tên mới").param("phone","0901234567"))
                .andExpect(status().is3xxRedirection());
        login(customer.getEmail(),"CUSTOMER");
        assertThat(profiles.profile(customer.getEmail()).getFullName()).isEqualTo("Tên mới");
        mvc.perform(post("/account/profile/addresses").with(csrf()).param("label","Công ty").param("recipient","Người nhận")
                .param("phone","0901234567").param("address","123 Nguyễn Huệ, Quận 1"))
                .andExpect(status().is3xxRedirection());
        login(customer.getEmail(),"CUSTOMER");
        var address=profiles.addresses(customer.getEmail()).getFirst();carts.addItem(customer.getEmail(),null,product.getId(),1,null,null);
        mvc.perform(get("/checkout").param("addressId",address.getId().toString())).andExpect(status().isOk())
                .andExpect(model().attributeExists("savedAddresses"));
        mvc.perform(get("/checkout").with(user("manager@chain.test").roles("BRANCH_MANAGER"))
                .param("addressId",address.getId().toString())).andExpect(status().is3xxRedirection());
    }
    @Test @WithMockUser(username="admin@chain.test",roles="ADMIN")
    void managerCannotReadOrTransitionAnotherStoresOrder() throws Exception {
        String code=place();orders.assignStore(code,second.getId());
        mvc.perform(get("/branch/orders/"+code).with(user("manager@chain.test").roles("BRANCH_MANAGER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/branch/orders/"+code+"/status").with(user("manager@chain.test").roles("BRANCH_MANAGER")).with(csrf()).param("status","CONFIRMED"))
                .andExpect(status().isForbidden());
        login("admin@chain.test","ADMIN");
        assertThat(orders.forAdmin(code).status()).isEqualTo(OrderStatus.NEW);
        orders.assignStore(code,first.getId());
        mvc.perform(get("/branch/orders/"+code).with(user("manager@chain.test").roles("BRANCH_MANAGER"))).andExpect(status().isOk());
        mvc.perform(post("/branch/orders/"+code+"/status").with(user("manager@chain.test").roles("BRANCH_MANAGER")).with(csrf()).param("status","CONFIRMED"))
                .andExpect(status().is3xxRedirection());
        login("admin@chain.test","ADMIN");
        assertThat(orders.forAdmin(code).status()).isEqualTo(OrderStatus.CONFIRMED);
    }
    @Test @WithMockUser(username="admin@chain.test",roles="ADMIN")
    void concurrentReceiptsDoNotLoseStockAndDuplicateRequestsCountOnce() throws Exception {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        var pool=Executors.newFixedThreadPool(2);var f=receipt(first,3);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            Callable<Void> task=()->{SecurityContextHolder.getContext().setAuthentication(authentication);try{ready.countDown();start.await(5,TimeUnit.SECONDS);inventory.change(f);return null;}finally{SecurityContextHolder.clearContext();}};
            var one=pool.submit(task);var two=pool.submit(task);assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();start.countDown();one.get(15,TimeUnit.SECONDS);two.get(15,TimeUnit.SECONDS);
        } finally {pool.shutdownNow();}
        assertThat(stocks.findByStoreIdAndProductId(first.getId(),product.getId()).orElseThrow().getQuantity()).isEqualTo(13);
        assertThat(movements.count()).isEqualTo(1);
    }
}
