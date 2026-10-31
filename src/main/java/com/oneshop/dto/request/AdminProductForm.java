package com.oneshop.dto.request;

import com.oneshop.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.web.multipart.MultipartFile;

public class AdminProductForm {

    @NotBlank(message = "Vui lòng nhập tên sản phẩm.")
    @Size(max = 180, message = "Tên sản phẩm tối đa 180 ký tự.")
    private String name;

    @NotBlank(message = "Vui lòng nhập slug.")
    @Size(max = 200, message = "Slug tối đa 200 ký tự.")
    @Pattern(regexp = "[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*", message = "Slug chỉ gồm chữ, số và dấu gạch nối.")
    private String slug;

    @NotBlank(message = "Vui lòng nhập SKU.")
    @Size(max = 80, message = "SKU tối đa 80 ký tự.")
    @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "SKU chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch nối.")
    private String sku;

    @Size(max = 300, message = "Mô tả ngắn tối đa 300 ký tự.")
    private String shortDescription;

    @Size(max = 10000, message = "Mô tả tối đa 10.000 ký tự.")
    private String description;

    @NotNull(message = "Vui lòng nhập giá bán.")
    @DecimalMin(value = "0.00", message = "Giá bán không được âm.")
    @Digits(integer = 10, fraction = 2, message = "Giá bán tối đa 10 chữ số nguyên và 2 chữ số thập phân.")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "Giá khuyến mãi không được âm.")
    @Digits(integer = 10, fraction = 2, message = "Giá khuyến mãi tối đa 10 chữ số nguyên và 2 chữ số thập phân.")
    private BigDecimal salePrice;

    @NotNull(message = "Vui lòng chọn danh mục.")
    private Long categoryId;

    private Long brandId;

    @NotNull(message = "Vui lòng chọn trạng thái.")
    private ProductStatus status = ProductStatus.ACTIVE;

    @jakarta.validation.constraints.Min(value=0,message="Số lượng không được âm.")
    @jakarta.validation.constraints.Max(value=100000,message="Mỗi lần nhập tối đa 100.000 sản phẩm.")
    private Integer stockQuantity = 0;
    private Long stockStoreId;
    @Pattern(regexp="[a-fA-F0-9-]{36}",message="Phiếu nhập chưa hợp lệ. Vui lòng tải lại trang.")
    private String stockRequestToken = java.util.UUID.randomUUID().toString();
    public Integer getStockQuantity(){return stockQuantity;} public void setStockQuantity(Integer value){stockQuantity=value;}
    public Long getStockStoreId(){return stockStoreId;} public void setStockStoreId(Long value){stockStoreId=value;}
    public String getStockRequestToken(){return stockRequestToken;} public void setStockRequestToken(String value){stockRequestToken=value;}

    private MultipartFile image;

    @Size(max = 10000, message = "Nội dung quá dài.")
    private String ingredients;
    public String getIngredients() { return ingredients; }
    public void setIngredients(String value) { ingredients = value; }

    @Size(max = 3000, message = "Nội dung quá dài.")
    private String suitableFor;
    public String getSuitableFor() { return suitableFor; }
    public void setSuitableFor(String value) { suitableFor = value; }

    @Size(max = 5000, message = "Nội dung quá dài.")
    private String directions;
    public String getDirections() { return directions; }
    public void setDirections(String value) { directions = value; }

    @Size(max = 3000, message = "Nội dung quá dài.")
    private String precautions;
    public String getPrecautions() { return precautions; }
    public void setPrecautions(String value) { precautions = value; }

    @Size(max = 150, message = "Nội dung quá dài.")
    private String packageSize;
    public String getPackageSize() { return packageSize; }
    public void setPackageSize(String value) { packageSize = value; }

    @Size(max = 5000, message = "Nội dung quá dài.")
    private String sizesText;
    public String getSizesText() { return sizesText; }
    public void setSizesText(String value) { sizesText = value; }

    @Size(max = 15000, message = "Nội dung quá dài.")
    private String shadesText;
    public String getShadesText() { return shadesText; }
    public void setShadesText(String value) { shadesText = value; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }

}
