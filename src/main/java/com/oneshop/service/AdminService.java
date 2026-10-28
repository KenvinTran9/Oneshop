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
import org.springframework.web.multipart.MultipartFile;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CloudinaryService cloudinaryService;

    public AdminService(ProductRepository productRepository,
                        StoreRepository storeRepository,
                        OrderRepository orderRepository,
                        UserRepository userRepository,
                        CategoryRepository categoryRepository,
                        BrandRepository brandRepository,
                        CloudinaryService cloudinaryService) {
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

        Category category = categoryRepository.findById(form.getCategoryId())
                .filter(Category::isActive)
                .orElseThrow(() -> new InvalidProductReferenceException("categoryId", "Danh mục không hợp lệ hoặc đã ngừng hoạt động."));
        Brand brand = form.getBrandId() == null ? null : brandRepository.findById(form.getBrandId())
                .filter(Brand::isActive)
                .orElseThrow(() -> new InvalidProductReferenceException("brandId", "Thương hiệu không hợp lệ hoặc đã ngừng hoạt động."));

        Product product = id == null ? new Product() : findProduct(id);
        String uploadedImageUrl = uploadImageIfProvided(form.getImage());

        product.setName(form.getName().trim());
        product.setSlug(slug);
        product.setSku(sku);
        product.setShortDescription(trimToNull(form.getShortDescription()));
        product.setDescription(trimToNull(form.getDescription()));
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
            return productRepository.save(product);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateProductFieldException("sku", "SKU hoặc slug đã tồn tại. Vui lòng kiểm tra lại.");
        }
    }

    public Product toggleBusinessStatus(Long id) {
        Product product = findProduct(id);
        boolean activate = !product.isActive() || product.getStatus() != ProductStatus.ACTIVE;
        product.setActive(activate);
        product.setStatus(activate ? ProductStatus.ACTIVE : ProductStatus.DISCONTINUED);
        return productRepository.save(product);
    }

    public Product deactivateProduct(Long id) {
        Product product = findProduct(id);
        product.setActive(false);
        product.setStatus(ProductStatus.DISCONTINUED);
        // Never hard-delete: order_items and carts may refer to this product.
        return productRepository.save(product);
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
