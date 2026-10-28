package com.oneshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.OrderColumn;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(length = 300)
    private String shortDescription;

    @Lob
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal salePrice;

    private String imageUrl;

    @ElementCollection
    @org.hibernate.annotations.BatchSize(size = 24)
    @CollectionTable(name = "product_shades", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "shade_order")
    private List<ProductShade> shades = new ArrayList<>();

    @ElementCollection
    @org.hibernate.annotations.BatchSize(size = 24)
    @CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size_label", length = 100)
    @OrderColumn(name = "size_order")
    private List<String> sizes = new ArrayList<>();

    @Column(length = 1000)
    private String priceSourceUrl;

    @Column(length = 1000)
    private String imageSourceUrl;

    private String priceVariant;

    private String priceCheckedOn;

    private boolean priceFrom;

    @Column(length = 3)
    private String priceCurrency = "VND";

    private String brandCountry;

    private String packageSize;

    @Lob
    private String ingredients;

    @Lob
    private String suitableFor;

    @Lob
    private String directions;

    @Lob
    private String precautions;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(length = 1500)
    private String imageSearchUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status = ProductStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<ProductShade> getShades() { return shades; }
    public void setShades(List<ProductShade> shades) { this.shades = new ArrayList<>(shades); }
    public List<String> getSizes() { return sizes; }
    public void setSizes(List<String> sizes) { this.sizes = new ArrayList<>(sizes); }
    public boolean isRequiresSelection() { return !shades.isEmpty() || sizes.size() > 1; }
    public BigDecimal getSellingPrice() { return salePrice != null ? salePrice : price; }

    public String getBrandCountry() { return brandCountry; }
    public void setBrandCountry(String brandCountry) { this.brandCountry = brandCountry; }

    public String getPackageSize() { return packageSize; }
    public void setPackageSize(String packageSize) { this.packageSize = packageSize; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getSuitableFor() { return suitableFor; }
    public void setSuitableFor(String suitableFor) { this.suitableFor = suitableFor; }

    public String getDirections() { return directions; }
    public void setDirections(String directions) { this.directions = directions; }

    public String getPrecautions() { return precautions; }
    public void setPrecautions(String precautions) { this.precautions = precautions; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getImageSearchUrl() { return imageSearchUrl; }
    public void setImageSearchUrl(String imageSearchUrl) { this.imageSearchUrl = imageSearchUrl; }

    public String getPriceSourceUrl() { return priceSourceUrl; }
    public void setPriceSourceUrl(String priceSourceUrl) { this.priceSourceUrl = priceSourceUrl; }

    public String getImageSourceUrl() { return imageSourceUrl; }
    public void setImageSourceUrl(String imageSourceUrl) { this.imageSourceUrl = imageSourceUrl; }

    public String getPriceVariant() { return priceVariant; }
    public void setPriceVariant(String priceVariant) { this.priceVariant = priceVariant; }

    public String getPriceCheckedOn() { return priceCheckedOn; }
    public void setPriceCheckedOn(String priceCheckedOn) { this.priceCheckedOn = priceCheckedOn; }

    public boolean isPriceFrom() { return priceFrom; }
    public void setPriceFrom(boolean priceFrom) { this.priceFrom = priceFrom; }

    public String getPriceCurrency() { return priceCurrency; }
    public void setPriceCurrency(String priceCurrency) { this.priceCurrency = priceCurrency; }

    public String getDisplayPrice() {
        BigDecimal amount = salePrice != null ? salePrice : price;
        if (amount == null) { return "Liên hệ"; }
        boolean vnd = priceCurrency == null || "VND".equals(priceCurrency);
        NumberFormat format = NumberFormat.getNumberInstance(vnd ? Locale.forLanguageTag("vi-VN") : Locale.UK);
        format.setMinimumFractionDigits(vnd ? 0 : 2);
        format.setMaximumFractionDigits(vnd ? 0 : 2);
        return format.format(amount) + (vnd ? " ₫" : " " + priceCurrency);
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }
}
