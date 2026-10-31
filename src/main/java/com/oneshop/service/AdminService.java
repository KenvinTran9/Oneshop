package com.oneshop.service;

import com.oneshop.dto.response.AdminDashboardStatsResponse;
import com.oneshop.dto.request.AdminProductForm;
import com.oneshop.entity.Brand;
import com.oneshop.entity.Category;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.exception.DuplicateProductFieldException;
import com.oneshop.exception.InvalidProductReferenceException;
import com.oneshop.exception.ProductImageUploadException;
import com.oneshop.exception.ProductNotFoundException;
import com.oneshop.repository.BrandRepository;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.OrderRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.StoreRepository;
import com.oneshop.repository.UserRepository;
import com.oneshop.service.CloudinaryService;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CloudinaryService cloudinaryService;
    private final InventoryService inventory;

    public AdminService(ProductRepository productRepository,
                        StoreRepository storeRepository,
                        OrderRepository orderRepository,
                        UserRepository userRepository,
                        CategoryRepository categoryRepository,
                        BrandRepository brandRepository,
                        CloudinaryService cloudinaryService, InventoryService inventory) {
        this.inventory = inventory;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public AdminDashboardStatsResponse getDashboardStats() {
        return new AdminDashboardStatsResponse(
                productRepository.count(),
                storeRepository.count(),
                orderRepository.count(),
                userRepository.count()
        );
    }

    public Page<Product> findProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public java.util.List<com.oneshop.entity.Store> activeStores() { return storeRepository.findByActiveTrueOrderByIdAsc(); }
    public Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(String.valueOf(id)));
    }

    public AdminProductForm newProductForm() {
        return new AdminProductForm();
    }

    public AdminProductForm editProductForm(Long id) {
        Product product = findProduct(id);
        AdminProductForm form = new AdminProductForm();
        form.setName(product.getName());
        form.setSlug(product.getSlug());
        form.setSku(product.getSku());
        form.setShortDescription(product.getShortDescription());
        form.setDescription(product.getDescription());
        form.setPrice(product.getPrice());
        form.setSalePrice(product.getSalePrice());
        form.setCategoryId(product.getCategory() == null ? null : product.getCategory().getId());
        form.setBrandId(product.getBrand() == null ? null : product.getBrand().getId());
        form.setStatus(product.getStatus());
        form.setIngredients(product.getIngredients());
        form.setSuitableFor(product.getSuitableFor());
        form.setDirections(product.getDirections());
        form.setPrecautions(product.getPrecautions());
        form.setPackageSize(product.getPackageSize());
        form.setSizesText(String.join("\n", product.getSizes()));
        form.setShadesText(product.getShades().stream().map(shade -> shade.getLabel() + " | " + (shade.getColor() == null ? "" : shade.getColor()) + " | " + (shade.getImageUrl() == null ? "" : shade.getImageUrl())).collect(java.util.stream.Collectors.joining("\n")));
        return form;
    }

    public List<Category> activeCategories() {
        return categoryRepository.findAll().stream()
                .filter(Category::isActive)
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<Brand> activeBrands() {
        return brandRepository.findAll().stream()
                .filter(Brand::isActive)
                .sorted(Comparator.comparing(Brand::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public Product saveProduct(Long id, AdminProductForm form) {
        String slug = form.getSlug().trim().toLowerCase(Locale.ROOT);
        String sku = form.getSku().trim().toUpperCase(Locale.ROOT);
        if (id == null ? productRepository.existsBySlugIgnoreCase(slug)
                : productRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new DuplicateProductFieldException("slug", "Slug đã được sử dụng.");
        }
        if (id == null ? productRepository.existsBySkuIgnoreCase(sku)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new DuplicateProductFieldException("sku", "SKU đã được sử dụng.");
        }

        int quantity = form.getStockQuantity() == null ? 0 : form.getStockQuantity();
        if (quantity < 0 || quantity > 100000 || (id == null && form.getStatus() == ProductStatus.ACTIVE && quantity == 0))
            throw new InvalidProductReferenceException("stockQuantity", "Nhập số lượng thực tế từ 1 đến 100.000 để mở bán sản phẩm mới, hoặc lưu Bản nháp.");
        if (quantity > 0 && (form.getStockStoreId() == null || storeRepository.findById(form.getStockStoreId()).filter(com.oneshop.entity.Store::isActive).isEmpty()))
            throw new InvalidProductReferenceException("stockStoreId", "Chọn chi nhánh nhận hàng đang hoạt động.");
        if (quantity > 0 && (form.getStockRequestToken() == null || !form.getStockRequestToken().matches("[a-fA-F0-9-]{36}")))
            throw new InvalidProductReferenceException("stockQuantity", "Phiếu nhập không hợp lệ. Vui lòng tải lại trang.");

        Category category = categoryRepository.findById(form.getCategoryId())
                .filter(Category::isActive)
                .orElseThrow(() -> new InvalidProductReferenceException("categoryId", "Danh mục không hợp lệ hoặc đã ngừng hoạt động."));
        Brand brand = form.getBrandId() == null ? null : brandRepository.findById(form.getBrandId())
                .filter(Brand::isActive)
                .orElseThrow(() -> new InvalidProductReferenceException("brandId", "Thương hiệu không hợp lệ hoặc đã ngừng hoạt động."));

        var sizes = parseSizes(form.getSizesText());
        var shades = parseShades(form.getShadesText());
        Product product = id == null ? new Product() : findProduct(id);
        // Preserve existing image metadata when the editor did not change this shade.
        var savedShades = shades.stream().map(edited -> product.getShades().stream()
                .filter(existing -> java.util.Objects.equals(existing.getLabel(), edited.getLabel())
                        && java.util.Objects.equals(existing.getColor(), edited.getColor())
                        && java.util.Objects.equals(existing.getImageUrl(), edited.getImageUrl()))
                .findFirst().orElse(edited)).toList();
        String uploadedImageUrl = uploadImageIfProvided(form.getImage());

        product.setName(form.getName().trim());
        product.setSlug(slug);
        product.setSku(sku);
        product.setShortDescription(trimToNull(form.getShortDescription()));
        product.setDescription(trimToNull(form.getDescription()));
        product.setIngredients(trimToNull(form.getIngredients()));
        product.setSuitableFor(trimToNull(form.getSuitableFor()));
        product.setDirections(trimToNull(form.getDirections()));
        product.setPrecautions(trimToNull(form.getPrecautions()));
        product.setPackageSize(trimToNull(form.getPackageSize()));
        product.getSizes().clear(); product.getSizes().addAll(sizes);
        product.getShades().clear(); product.getShades().addAll(savedShades);
        product.setPrice(form.getPrice());
        product.setSalePrice(form.getSalePrice());
        product.setCategory(category);
        product.setBrand(brand);
        product.setStatus(form.getStatus());
        product.setActive(form.getStatus() != ProductStatus.DISCONTINUED);
        if (uploadedImageUrl != null) {
            product.setImageUrl(uploadedImageUrl);
        }

        try {
            productRepository.saveAndFlush(product);
            if (quantity > 0) {
                var receipt = new com.oneshop.dto.request.InventoryChangeForm();
                receipt.setStoreId(form.getStockStoreId()); receipt.setProductId(product.getId()); receipt.setDelta(quantity);
                receipt.setReason(id == null ? "Nhập kho khi tạo sản phẩm" : "Nhập bổ sung từ trang sản phẩm");
                receipt.setRequestToken(form.getStockRequestToken()); inventory.change(receipt);
            }
            return product;
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateProductFieldException("sku", "SKU hoặc slug đã tồn tại. Vui lòng kiểm tra lại.");
        }
    }

    @Transactional
    public Product toggleBusinessStatus(Long id) {
        Product product = findProduct(id);
        boolean activate = !product.isActive() || (product.getStatus() != ProductStatus.ACTIVE && product.getStatus() != ProductStatus.OUT_OF_STOCK);
        product.setActive(activate);
        product.setStatus(activate ? ProductStatus.ACTIVE : ProductStatus.DISCONTINUED);
        return productRepository.saveAndFlush(product);
    }

    @Transactional
    public Product markStockStatus(Long id, boolean soldOut) {
        Product product = findProduct(id);
        if (!product.isActive() || product.getStatus() == ProductStatus.DISCONTINUED)
            throw new IllegalArgumentException("Mở bán lại sản phẩm trước khi đổi tình trạng tồn hàng.");
        product.setStatus(soldOut ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE);
        return productRepository.saveAndFlush(product);
    }
    @Transactional
    public Product deactivateProduct(Long id) {
        Product product = findProduct(id);
        product.setActive(false);
        product.setStatus(ProductStatus.DISCONTINUED);
        // Never hard-delete: order_items and carts may refer to this product.
        return productRepository.saveAndFlush(product);
    }

    private java.util.List<String> parseSizes(String text) {
        if (text == null || text.isBlank()) return java.util.List.of();
        var values = text.lines().map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (values.size() > 50 || values.stream().anyMatch(value -> value.length() > 100)
                || values.stream().map(value -> value.toLowerCase(Locale.ROOT)).distinct().count() != values.size())
            throw new InvalidProductReferenceException("sizesText", "Tối đa 50 quy cách, mỗi dòng tối đa 100 ký tự và không trùng nhau.");
        return values;
    }

    private java.util.List<com.oneshop.entity.ProductShade> parseShades(String text) {
        var result = new java.util.ArrayList<com.oneshop.entity.ProductShade>();
        var labels = new java.util.HashSet<String>();
        if (text == null || text.isBlank()) return result;
        for (String line : text.lines().filter(value -> !value.isBlank()).toList()) {
            String[] parts = line.split("\\|", -1);
            String label = parts[0].trim();
            String color = parts.length > 1 ? parts[1].trim() : "";
            String image = parts.length > 2 ? parts[2].trim() : "";
            if (parts.length > 3 || label.isBlank() || label.length() > 100 || !labels.add(label.toLowerCase(Locale.ROOT))
                    || (!color.isEmpty() && !color.matches("#[a-fA-F0-9]{6}")) || image.length() > 300)
                throw new InvalidProductReferenceException("shadesText", "Màu không hợp lệ hoặc bị trùng. Dùng mẫu: Tên màu | #RRGGBB | URL ảnh HTTPS.");
            if (!image.isEmpty()) {
                try {
                    var uri = java.net.URI.create(image);
                    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException();
                } catch (IllegalArgumentException invalid) {
                    throw new InvalidProductReferenceException("shadesText", "Ảnh màu phải là URL HTTPS hợp lệ.");
                }
            }
            result.add(new com.oneshop.entity.ProductShade(label, color.isEmpty() ? null : color,
                    image.isEmpty() ? null : image, label, image.isEmpty() ? null : "product"));
        }
        if (result.size() > 50) throw new InvalidProductReferenceException("shadesText", "Tối đa 50 màu cho một sản phẩm.");
        return result;
    }

    private String uploadImageIfProvided(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        if (image.getSize() > 5L * 1024 * 1024 || !hasSupportedImageSignature(image)) {
            throw new ProductImageUploadException("Ảnh phải là JPG, PNG, GIF hoặc WebP và không vượt quá 5 MB.");
        }
        try {
            return cloudinaryService.uploadProductImage(image);
        } catch (IOException | RuntimeException exception) {
            throw new ProductImageUploadException("Tải ảnh lên thất bại. Thông tin sản phẩm hiện tại chưa bị thay đổi.");
        }
    }

    private boolean hasSupportedImageSignature(MultipartFile image) {
        try (InputStream input = image.getInputStream()) {
            byte[] bytes = input.readNBytes(12);
            boolean jpeg = bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            boolean png = bytes.length >= 8 && (bytes[0] & 0xff) == 0x89
                    && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
            boolean gif = bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
            boolean webp = bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                    && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
                    && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            return jpeg || png || gif || webp;
        } catch (IOException exception) {
            return false;
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
