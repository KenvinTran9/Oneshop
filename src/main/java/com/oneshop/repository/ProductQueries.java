package com.oneshop.repository;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface ProductQueries {
    Page<Product> searchCatalog(ProductStatus status, String category, String search, Pageable pageable);
    Page<Product> findByStatusAndCategory_Slug(ProductStatus status, String categorySlug, Pageable pageable);
    List<ProductRepository.HomeCategory> summarizeHomeCategories(ProductStatus status);
    List<ProductRepository.HomeBrand> summarizeHomeBrands(ProductStatus status, Pageable pageable);
    ProductRepository.HomeCounts summarizeHomeCounts(ProductStatus status);
}
