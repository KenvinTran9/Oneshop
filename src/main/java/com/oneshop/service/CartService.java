package com.oneshop.service;

import com.oneshop.dto.response.CartItemView;
import com.oneshop.dto.response.CartView;
import com.oneshop.entity.Cart;
import com.oneshop.entity.CartItem;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.entity.User;
import com.oneshop.exception.CartOperationException;
import com.oneshop.repository.CartItemRepository;
import com.oneshop.repository.CartRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.StoreStockRepository;
import com.oneshop.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    public static final String GUEST_CART_SESSION_ATTRIBUTE = "oneshop.guestCart";
    public static final int MAX_QUANTITY_PER_SKU = 99;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final StoreStockRepository storeStockRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       StoreStockRepository storeStockRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.storeStockRepository = storeStockRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartView getCart(String authenticatedEmail, HttpSession session) {
        if (authenticatedEmail != null) {
            return cartRepository.findByUserEmailIgnoreCase(authenticatedEmail)
                    .map(cart -> buildCart(cart.getItems().stream()
                            .map(item -> new ProductQuantity(item.getProduct(), item.getQuantity()))
                            .toList()))
                    .orElseGet(this::emptyCart);
        }

        Map<Long, Integer> guestItems = guestItems(session);
        List<ProductQuantity> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : guestItems.entrySet()) {
            productRepository.findById(entry.getKey())
                    .ifPresent(product -> items.add(new ProductQuantity(product, entry.getValue())));
        }
        return buildCart(items);
    }

    @Transactional(readOnly = true)
    public long getItemCount(String authenticatedEmail, HttpSession session) {
        if (authenticatedEmail != null) {
            return cartItemRepository.sumQuantityByCartUserEmail(authenticatedEmail);
        }
        return guestItems(session).values().stream().mapToLong(Integer::longValue).sum();
    }

    @Transactional
    public void addItem(String authenticatedEmail, HttpSession session, Long productId, int quantity) {
        validateRequestedQuantity(quantity);
        Product product = getProduct(productId);
        assertSellable(product);

        if (authenticatedEmail == null) {
            Map<Long, Integer> guestItems = guestItems(session);
            int newQuantity = guestItems.getOrDefault(productId, 0) + quantity;
            validateRequestedQuantity(newQuantity);
            assertAvailable(product, newQuantity);
            guestItems.put(productId, newQuantity);
            saveGuestItems(session, guestItems);
            return;
        }

        Cart cart = getOrCreateCart(authenticatedEmail);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseGet(() -> newCartItem(cart, product));
        int newQuantity = item.getQuantity() + quantity;
        validateRequestedQuantity(newQuantity);
        assertAvailable(product, newQuantity);
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
    }

    @Transactional
    public void updateQuantity(String authenticatedEmail, HttpSession session, Long productId, int quantity) {
        validateRequestedQuantity(quantity);
        Product product = getProduct(productId);
        assertSellable(product);
        assertAvailable(product, quantity);

        if (authenticatedEmail == null) {
            Map<Long, Integer> guestItems = guestItems(session);
            if (!guestItems.containsKey(productId)) {
                throw new CartOperationException("Sản phẩm này không còn trong giỏ hàng.");
            }
            guestItems.put(productId, quantity);
            saveGuestItems(session, guestItems);
            return;
        }

        Cart cart = getCartForUser(authenticatedEmail);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new CartOperationException("Sản phẩm này không còn trong giỏ hàng."));
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Transactional
    public void removeItem(String authenticatedEmail, HttpSession session, Long productId) {
        if (authenticatedEmail == null) {
            Map<Long, Integer> guestItems = guestItems(session);
            if (guestItems.remove(productId) == null) {
                throw new CartOperationException("Sản phẩm này không còn trong giỏ hàng.");
            }
            saveGuestItems(session, guestItems);
            return;
        }

        Cart cart = getCartForUser(authenticatedEmail);
        if (cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId) == 0) {
            throw new CartOperationException("Sản phẩm này không còn trong giỏ hàng.");
        }
    }

    @Transactional
    public void clearCart(String authenticatedEmail, HttpSession session) {
        if (authenticatedEmail == null) {
            if (session != null) {
                session.removeAttribute(GUEST_CART_SESSION_ATTRIBUTE);
            }
            return;
        }

        cartRepository.findByUserEmailIgnoreCase(authenticatedEmail)
                .ifPresent(cart -> cartItemRepository.deleteAllByCartId(cart.getId()));
    }

    @Transactional
    public void mergeGuestCart(String authenticatedEmail, HttpSession session) {
        Map<Long, Integer> guestItems = guestItems(session);
        if (authenticatedEmail == null || guestItems.isEmpty()) {
            return;
        }

        Cart cart = getOrCreateCart(authenticatedEmail);
        for (Map.Entry<Long, Integer> entry : guestItems.entrySet()) {
            Long productId = entry.getKey();
            Integer guestQuantity = entry.getValue();
            if (productId == null || guestQuantity == null || guestQuantity < 1) {
                continue;
            }

            Optional<Product> productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                continue;
            }

            Product product = productOptional.orElseThrow();
            CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                    .orElseGet(() -> newCartItem(cart, product));
            item.setQuantity(Math.min(MAX_QUANTITY_PER_SKU, item.getQuantity() + guestQuantity));
            cartItemRepository.save(item);
        }
    }

    private CartView buildCart(List<ProductQuantity> products) {
        List<CartItemView> views = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        long itemCount = 0;

        for (ProductQuantity productQuantity : products) {
            Product product = productQuantity.product();
            int quantity = productQuantity.quantity();
            BigDecimal unitPrice = currentPrice(product);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            long availableStock = Math.max(0, storeStockRepository.sumAvailableQuantityByProductId(product.getId()));
            String warning = warningFor(product, quantity, availableStock);

            views.add(new CartItemView(
                    product.getId(),
                    product.getSku(),
                    product.getName(),
                    product.getImageUrl(),
                    quantity,
                    unitPrice,
                    lineTotal,
                    availableStock,
                    warning
            ));
            subtotal = subtotal.add(lineTotal);
            itemCount += quantity;
        }
        return new CartView(List.copyOf(views), subtotal, itemCount);
    }

    private CartView emptyCart() {
        return new CartView(List.of(), BigDecimal.ZERO, 0);
    }

    private BigDecimal currentPrice(Product product) {
        BigDecimal salePrice = product.getSalePrice();
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) >= 0
                && salePrice.compareTo(product.getPrice()) < 0) {
            return salePrice;
        }
        return product.getPrice();
    }

    private String warningFor(Product product, int quantity, long availableStock) {
        if (!product.isActive() || product.getStatus() == ProductStatus.DISCONTINUED) {
            return "Sản phẩm đã ngừng kinh doanh. Vui lòng xóa sản phẩm khỏi giỏ hàng.";
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            return "Sản phẩm hiện không được mở bán. Vui lòng xóa sản phẩm khỏi giỏ hàng.";
        }
        if (availableStock == 0) {
            return "Sản phẩm hiện đã hết hàng tại các cửa hàng đang hoạt động.";
        }
        if (quantity > availableStock) {
            return "Số lượng trong giỏ vượt tồn hiện tại. Hiện chỉ còn " + availableStock + " sản phẩm.";
        }
        return null;
    }

    private void assertSellable(Product product) {
        if (!product.isActive() || product.getStatus() != ProductStatus.ACTIVE) {
            throw new CartOperationException("Sản phẩm hiện không được mở bán và chưa thể thêm vào giỏ.");
        }
    }

    private void assertAvailable(Product product, int quantity) {
        long available = Math.max(0, storeStockRepository.sumAvailableQuantityByProductId(product.getId()));
        if (available == 0) {
            throw new CartOperationException("Sản phẩm hiện đã hết hàng tại các cửa hàng đang hoạt động.");
        }
        if (quantity > available) {
            throw new CartOperationException("Số lượng vượt tồn hiện tại. Hiện chỉ còn " + available + " sản phẩm.");
        }
    }

    private Product getProduct(Long productId) {
        if (productId == null) {
            throw new CartOperationException("Sản phẩm không hợp lệ.");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new CartOperationException("Sản phẩm không còn tồn tại."));
    }

    private Cart getOrCreateCart(String email) {
        return cartRepository.findByUserEmailIgnoreCase(email).orElseGet(() -> {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new CartOperationException("Không thể xác định tài khoản hiện tại."));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.saveAndFlush(cart);
        });
    }

    private Cart getCartForUser(String email) {
        return cartRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new CartOperationException("Sản phẩm này không còn trong giỏ hàng."));
    }

    private CartItem newCartItem(Cart cart, Product product) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        return item;
    }

    private void validateRequestedQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY_PER_SKU) {
            throw new CartOperationException("Số lượng mỗi SKU phải từ 1 đến " + MAX_QUANTITY_PER_SKU + ".");
        }
    }

    private Map<Long, Integer> guestItems(HttpSession session) {
        if (session == null) {
            return new LinkedHashMap<>();
        }
        Object sessionValue = session.getAttribute(GUEST_CART_SESSION_ATTRIBUTE);
        if (!(sessionValue instanceof Map<?, ?> rawItems)) {
            return new LinkedHashMap<>();
        }

        Map<Long, Integer> items = new TreeMap<>();
        for (Map.Entry<?, ?> entry : rawItems.entrySet()) {
            if (entry.getKey() instanceof Long productId
                    && entry.getValue() instanceof Integer quantity
                    && productId > 0 && quantity > 0) {
                items.put(productId, Math.min(quantity, MAX_QUANTITY_PER_SKU));
            }
        }
        return items;
    }

    private void saveGuestItems(HttpSession session, Map<Long, Integer> items) {
        if (session == null) {
            throw new CartOperationException("Phiên giỏ hàng đã hết hạn. Vui lòng tải lại trang.");
        }
        session.setAttribute(GUEST_CART_SESSION_ATTRIBUTE, new LinkedHashMap<>(items));
    }

    private record ProductQuantity(Product product, int quantity) {
    }
}
