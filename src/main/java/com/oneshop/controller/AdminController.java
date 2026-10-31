package com.oneshop.controller;

import com.oneshop.dto.response.AdminDashboardStatsResponse;
import com.oneshop.dto.request.AdminProductForm;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.exception.DuplicateProductFieldException;
import com.oneshop.exception.InvalidProductReferenceException;
import com.oneshop.exception.ProductImageUploadException;
import com.oneshop.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String dashboard(Model model) {
        AdminDashboardStatsResponse stats = adminService.getDashboardStats();
        model.addAttribute("productCount", stats.productCount());
        model.addAttribute("storeCount", stats.storeCount());
        model.addAttribute("orderCount", stats.orderCount());
        model.addAttribute("customerCount", stats.customerCount());
        return "admin/dashboard";
    }

    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("productPage", adminService.findProducts(PageRequest.of(
                Math.max(page, 0), 20, Sort.by(Sort.Direction.DESC, "id"))));
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        populateForm(model, adminService.newProductForm(), null);
        return "admin/product-form";
    }

    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute("productForm") AdminProductForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        validateSalePrice(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateForm(model, form, null);
            return "admin/product-form";
        }
        try {
            Product product = adminService.saveProduct(null, form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo sản phẩm" + (form.getStockQuantity() != null && form.getStockQuantity() > 0 ? " và nhập kho " + form.getStockQuantity() + " sản phẩm." : "."));
            return "redirect:/admin/products/" + product.getId();
        } catch (DuplicateProductFieldException exception) {
            bindingResult.rejectValue(exception.getField(), "product.duplicate", exception.getMessage());
        } catch (ProductImageUploadException exception) {
            bindingResult.rejectValue("image", "product.image.upload", exception.getMessage());
        } catch (InvalidProductReferenceException exception) {
            bindingResult.rejectValue(exception.getField(), "product.reference.invalid", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("stockQuantity", "product.stock.invalid", exception.getMessage());
        }
        populateForm(model, form, null);
        return "admin/product-form";
    }

    @GetMapping("/products/{id}")
    public String productDetails(@PathVariable Long id, Model model) {
        model.addAttribute("product", adminService.findProduct(id));
        return "admin/product-detail";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        populateForm(model, adminService.editProductForm(id), id);
        return "admin/product-form";
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("productForm") AdminProductForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        validateSalePrice(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateForm(model, form, id);
            return "admin/product-form";
        }
        try {
            adminService.saveProduct(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật sản phẩm.");
            return "redirect:/admin/products/" + id;
        } catch (DuplicateProductFieldException exception) {
            bindingResult.rejectValue(exception.getField(), "product.duplicate", exception.getMessage());
        } catch (ProductImageUploadException exception) {
            bindingResult.rejectValue("image", "product.image.upload", exception.getMessage());
        } catch (InvalidProductReferenceException exception) {
            bindingResult.rejectValue(exception.getField(), "product.reference.invalid", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("stockQuantity", "product.stock.invalid", exception.getMessage());
        }
        populateForm(model, form, id);
        return "admin/product-form";
    }

    @PostMapping("/products/{id}/status")
    public String toggleProductStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Product product = adminService.toggleBusinessStatus(id);
        redirectAttributes.addFlashAttribute("successMessage",
                product.getStatus() == ProductStatus.ACTIVE ? "Đã mở kinh doanh sản phẩm." : "Đã ngừng kinh doanh sản phẩm.");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/stock-status")
    public String stockStatus(@PathVariable Long id,@RequestParam boolean soldOut,RedirectAttributes redirect){
        try{adminService.markStockStatus(id,soldOut);redirect.addFlashAttribute("successMessage",soldOut
            ? "Đã đánh dấu hết hàng. Khách vẫn xem được và có thể theo dõi sản phẩm."
            : "Đã bỏ đánh dấu hết hàng. Khách theo dõi sẽ nhận email khi sản phẩm có tồn kho tại cửa hàng đang hoạt động.");}
        catch(IllegalArgumentException failure){redirect.addFlashAttribute("errorMessage",failure.getMessage());}
        return "redirect:/admin/products";
    }
    @PostMapping("/products/{id}/delete")
    public String removeProduct(@PathVariable Long id,RedirectAttributes redirect){
        adminService.deactivateProduct(id);redirect.addFlashAttribute("successMessage","Đã xóa sản phẩm khỏi shop. Lịch sử đơn hàng được giữ nguyên.");
        return "redirect:/admin/products";
    }
    @PostMapping("/products/{id}/deactivate")
    public String deactivateProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deactivateProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã đưa sản phẩm về trạng thái ngừng kinh doanh; dữ liệu không bị xóa.");
        return "redirect:/admin/products/" + id;
    }

    private void populateForm(Model model, AdminProductForm form, Long productId) {
        model.addAttribute("productForm", form);
        model.addAttribute("productId", productId);
        model.addAttribute("formTitle", productId == null ? "Thêm sản phẩm" : "Cập nhật sản phẩm");
        model.addAttribute("formAction", productId == null ? "/admin/products" : "/admin/products/" + productId);
        model.addAttribute("categories", adminService.activeCategories());
        model.addAttribute("stockStores", adminService.activeStores());
        model.addAttribute("brands", adminService.activeBrands());
        model.addAttribute("currentImageUrl", productId == null ? null : adminService.findProduct(productId).getImageUrl());
    }

    private void validateSalePrice(AdminProductForm form, BindingResult bindingResult) {
        if (form.getPrice() != null && form.getSalePrice() != null
                && form.getSalePrice().compareTo(form.getPrice()) > 0) {
            bindingResult.rejectValue("salePrice", "product.salePrice.greaterThanPrice",
                    "Giá khuyến mãi không được cao hơn giá bán.");
        }
    }
}
