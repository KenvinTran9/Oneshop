package com.oneshop.service;

import com.oneshop.dto.CartSelection;
import com.oneshop.dto.response.CartItemView;
import com.oneshop.dto.response.CartView;
import com.oneshop.entity.Cart;
import com.oneshop.entity.CartItem;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.entity.User;
import com.oneshop.exception.CartOperationException;
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
import org.springframework.stereotype.Service;

@Service
public class CartService {
    public static final String GUEST_CART_SESSION_ATTRIBUTE = "oneshop.guestCart";
    public static final int MAX_QUANTITY_PER_SKU = 99;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final StoreStockRepository storeStockRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository, StoreStockRepository storeStockRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.storeStockRepository = storeStockRepository;
        this.userRepository = userRepository;
    }

    public CartView getCart(String email, HttpSession session) {
        if (email != null) {
            return cartRepository.findByUserEmailIgnoreCase(email)
                    .map(cart -> buildCart(cart.getItems().stream()
                            .map(item -> new ProductQuantity(item.getProduct(), selection(item), item.getQuantity()))
                            .toList())).orElseGet(this::emptyCart);
        }
        List<ProductQuantity> items = new ArrayList<>();
        guestItems(session).forEach((key, count) -> productRepository.findById(key.productId())
                .ifPresent(product -> items.add(new ProductQuantity(product, key, count))));
        return buildCart(items);
    }

    public long getItemCount(String email, HttpSession session) {
        return email != null ? cartRepository.findByUserEmailIgnoreCase(email).map(cart -> cart.getItems().stream().mapToLong(CartItem::getQuantity).sum()).orElse(0L)
                : guestItems(session).values().stream().mapToLong(Integer::longValue).sum();
    }

    public void addItem(String email, HttpSession session, Long productId, int quantity, String size, String shade) {
        validateRequestedQuantity(quantity);
        Product product = getProduct(productId);
        assertSellable(product);
        CartSelection key = checkedSelection(product, size, shade);
        if (email == null) {
            var items = guestItems(session);
            int next = items.getOrDefault(key, 0) + quantity;
            validateRequestedQuantity(next);
            assertAvailable(product, totalForProduct(items, productId) + quantity);
            items.put(key, next);
            saveGuestItems(session, items);
            return;
        }
        changeCart(email, cart -> {
            CartItem item = findItem(cart, key).orElseGet(() -> newCartItem(cart, product, key));
            int next = item.getQuantity() + quantity;
            validateRequestedQuantity(next);
            assertAvailable(product, totalForProduct(cart, productId) + quantity);
            item.setQuantity(next);
        });
    }

    public void updateQuantity(String email, HttpSession session, Long productId, int quantity, String size, String shade) {
        validateRequestedQuantity(quantity);
        CartSelection key = lineKey(productId, size, shade);
        Product product = getProduct(productId);
        assertSellable(product);
        checkedSelection(product, size, shade);
        if (email == null) {
            var items = guestItems(session);
            Integer previous = items.get(key);
            if (previous == null) throw missingItem();
            assertAvailable(product, totalForProduct(items, productId) - previous + quantity);
            items.put(key, quantity);
            saveGuestItems(session, items);
            return;
        }
        changeCart(email, cart -> {
            CartItem item = findItem(cart, key).orElseThrow(this::missingItem);
            assertAvailable(product, totalForProduct(cart, productId) - item.getQuantity() + quantity);
            item.setQuantity(quantity);
        });
    }

    public void removeItem(String email, HttpSession session, Long productId, String size, String shade) {
        // Removal must remain possible after a shade is retired or the product is discontinued.
        CartSelection key = lineKey(productId, size, shade);
        if (email == null) {
            var items = guestItems(session);
            if (items.remove(key) == null) throw missingItem();
            saveGuestItems(session, items);
            return;
        }
        changeCart(email, cart -> {
            if (!cart.getItems().removeIf(item -> selection(item).equals(key))) throw missingItem();
        });
    }

    public void clearCart(String email, HttpSession session) {
        if (email == null) {
            if (session != null) session.removeAttribute(GUEST_CART_SESSION_ATTRIBUTE);
        } else {
            cartRepository.findByUserEmailIgnoreCase(email)
                    .ifPresent(cart -> changeCart(email, current -> current.getItems().clear()));
        }
    }

    public void mergeGuestCart(String email, HttpSession session) {
        var guest = guestItems(session);
        if (email == null || guest.isEmpty()) return;
        // All choices are merged in one versioned cart document; no partial multi-document merge.
        changeCart(email, cart -> guest.forEach((key, count) -> productRepository.findById(key.productId()).ifPresent(product -> {
            CartItem item = findItem(cart, key).orElseGet(() -> newCartItem(cart, product, key));
            item.setQuantity(Math.min(MAX_QUANTITY_PER_SKU, item.getQuantity() + count));
        })));
    }

    private CartView buildCart(List<ProductQuantity> items) {
        Map<Long, Long> totals = new LinkedHashMap<>();
        items.forEach(item -> totals.merge(item.product().getId(), (long) item.quantity(), Long::sum));
        List<CartItemView> views = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        long itemCount = 0;
        for (var item : items) {
            Product product = item.product();
            CartSelection key = item.selection();
            BigDecimal unitPrice = currentPrice(product);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
            long stock = Math.max(0, storeStockRepository.sumAvailableQuantityByProductId(product.getId()));
            String warning = warningFor(product, key, totals.get(product.getId()), stock);

            views.add(new CartItemView(product.getId(), product.getSku(), product.getName(), product.getSlug(),
                    key.size(), key.shade(), imageFor(product, key), item.quantity(), unitPrice, lineTotal, stock, warning));
            subtotal = subtotal.add(lineTotal);
            itemCount += item.quantity();
        }
        return new CartView(List.copyOf(views), subtotal, itemCount);
    }

    private String imageFor(Product product, CartSelection key) {
        return product.getShades().stream().filter(shade -> shade.getLabel().equals(key.shade()))
                .filter(shade -> "photo".equals(shade.getImageType()) && shade.getImageUrl() != null
                        && shade.getImageUrl().matches("/images/[a-zA-Z0-9/_.-]+"))
                .map(com.oneshop.entity.ProductShade::getImageUrl).findFirst().orElse(product.getImageUrl());
    }

    private CartSelection checkedSelection(Product product, String size, String shade) {
        CartSelection key = lineKey(product.getId(), size, shade);
        String chosenSize = choose(product.getSizes(), key.size(), "quy cách");
        String chosenShade = choose(product.getShades().stream().map(com.oneshop.entity.ProductShade::getLabel).toList(), key.shade(), "màu");
        return new CartSelection(product.getId(), chosenSize, chosenShade);
    }

    private String choose(List<String> allowed, String value, String label) {
        if (value.isEmpty()) {
            if (allowed.isEmpty()) return "";
            if (allowed.size() == 1) return allowed.getFirst();
            throw new CartOperationException("Vui lòng chọn " + label + " của sản phẩm trước khi thêm giỏ.");
        }
        if (!allowed.contains(value)) throw new CartOperationException("Lựa chọn " + label + " không còn hợp lệ. Vui lòng chọn lại.");
        return value;
    }

    private CartSelection lineKey(Long productId, String size, String shade) {
        CartSelection key = new CartSelection(productId, size, shade);
        if (productId == null || productId <= 0 || key.size().length() > 100 || key.shade().length() > 100)
            throw new CartOperationException("Lựa chọn sản phẩm không hợp lệ.");
        return key;
    }

    private String warningFor(Product product, CartSelection key, long total, long stock) {
        if (!product.isActive() || product.getStatus() == ProductStatus.DISCONTINUED)
            return "Sản phẩm đã ngừng kinh doanh. Vui lòng xóa sản phẩm khỏi giỏ hàng.";
        if (product.getStatus() != ProductStatus.ACTIVE)
            return "Sản phẩm hiện không được mở bán. Vui lòng xóa sản phẩm khỏi giỏ hàng.";
        try { checkedSelection(product, key.size(), key.shade()); }
        catch (CartOperationException exception) { return "Màu hoặc quy cách đã thay đổi. Vui lòng chọn lại sản phẩm."; }
        if (stock == 0) return "Sản phẩm hiện đã hết hàng tại các cửa hàng đang hoạt động.";
        if (total > stock) return "Tổng số lượng các lựa chọn của sản phẩm vượt tồn hiện tại. Hiện chỉ còn " + stock + " sản phẩm.";
        return null;
    }

    private BigDecimal currentPrice(Product product) {
        BigDecimal sale = product.getSalePrice();
        return sale != null && sale.signum() >= 0 && sale.compareTo(product.getPrice()) < 0 ? sale : product.getPrice();
    }

    private void assertSellable(Product product) {
        if (!product.isActive() || product.getStatus() != ProductStatus.ACTIVE)
            throw new CartOperationException("Sản phẩm hiện không được mở bán và chưa thể thêm vào giỏ.");
    }

    private void assertAvailable(Product product, long quantity) {
        long available = Math.max(0, storeStockRepository.sumAvailableQuantityByProductId(product.getId()));
        if (available == 0) throw new CartOperationException("Sản phẩm hiện đã hết hàng tại các cửa hàng đang hoạt động.");
        if (quantity > available) throw new CartOperationException("Tổng số lượng sản phẩm vượt tồn hiện tại. Hiện chỉ còn " + available + " sản phẩm.");
    }

    private Product getProduct(Long id) {
        if (id == null) throw new CartOperationException("Sản phẩm không hợp lệ.");
        return productRepository.findById(id).orElseThrow(() -> new CartOperationException("Sản phẩm không còn tồn tại."));
    }

    private Cart getOrCreateCart(String email) {
        return cartRepository.findByUserEmailIgnoreCase(email).orElseGet(() -> {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new CartOperationException("Không thể xác định tài khoản hiện tại."));
            Cart cart = new Cart(); cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    private void changeCart(String email, java.util.function.Consumer<Cart> change) {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                Cart cart = getOrCreateCart(email);
                change.accept(cart);
                cartRepository.save(cart);
                return;
            } catch (org.springframework.dao.OptimisticLockingFailureException | org.springframework.dao.DuplicateKeyException conflict) {
                // Reload before retrying; a failed versioned save did not apply the mutation.
            }
        }
        throw new CartOperationException("Giỏ hàng vừa được thay đổi ở nơi khác. Vui lòng thử lại.");
    }

    private java.util.Optional<CartItem> findItem(Cart cart, CartSelection key) {
        return cart.getItems().stream().filter(item -> selection(item).equals(key)).findFirst();
    }

    private CartItem newCartItem(Cart cart, Product product, CartSelection key) {
        CartItem item = new CartItem(); item.setProduct(product);
        item.setSelectedSize(key.size()); item.setSelectedShade(key.shade());
        cart.getItems().add(item);
        return item;
    }

    private CartSelection selection(CartItem item) {
        return new CartSelection(item.getProduct().getId(), item.getSelectedSize(), item.getSelectedShade());
    }

    private long totalForProduct(Map<CartSelection, Integer> items, Long productId) {
        return items.entrySet().stream().filter(entry -> productId.equals(entry.getKey().productId()))
                .mapToLong(Map.Entry::getValue).sum();
    }

    private long totalForProduct(Cart cart, Long productId) {
        return cart.getItems().stream().filter(item -> productId.equals(item.getProduct().getId()))
                .mapToLong(CartItem::getQuantity).sum();
    }

    private CartView emptyCart() { return new CartView(List.of(), BigDecimal.ZERO, 0); }
    private CartOperationException missingItem() { return new CartOperationException("Lựa chọn này không còn trong giỏ hàng."); }
    private void validateRequestedQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY_PER_SKU)
            throw new CartOperationException("Số lượng mỗi lựa chọn phải từ 1 đến " + MAX_QUANTITY_PER_SKU + ".");
    }

    private Map<CartSelection, Integer> guestItems(HttpSession session) {
        Map<CartSelection, Integer> items = new LinkedHashMap<>();
        if (session == null || !(session.getAttribute(GUEST_CART_SESSION_ATTRIBUTE) instanceof Map<?, ?> raw)) return items;
        raw.forEach((rawKey, rawCount) -> {
            // Read sessions from the previous product-only cart without dropping their contents.
            CartSelection key = rawKey instanceof CartSelection selection ? selection
                    : rawKey instanceof Long id ? new CartSelection(id, "", "") : null;
            if (key != null && key.productId() != null && key.productId() > 0 && key.size().length() <= 100
                    && key.shade().length() <= 100 && rawCount instanceof Integer count && count > 0)
                items.put(key, Math.min(count, MAX_QUANTITY_PER_SKU));
        });
        return items;
    }

    private void saveGuestItems(HttpSession session, Map<CartSelection, Integer> items) {
        if (session == null) throw new CartOperationException("Phiên giỏ hàng đã hết hạn. Vui lòng tải lại trang.");
        session.setAttribute(GUEST_CART_SESSION_ATTRIBUTE, new LinkedHashMap<>(items));
    }

    private record ProductQuantity(Product product, CartSelection selection, int quantity) { }
}
