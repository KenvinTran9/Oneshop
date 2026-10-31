package com.oneshop.controller;

import com.oneshop.dto.request.InventoryChangeForm;
import com.oneshop.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/admin/inventory", "/branch/inventory"})
public class InventoryController {
    private final InventoryService inventory;
    public InventoryController(InventoryService inventory) { this.inventory = inventory; }
    @GetMapping
    public String page(@RequestParam(required = false) Long storeId, @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "false") boolean low, @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "0") int historyPage, HttpServletRequest request, Model model) {
        var stores = inventory.allowedStores();
        if (storeId == null && !stores.isEmpty()) storeId = stores.getFirst().getId();
        model.addAttribute("stores", stores); model.addAttribute("storeId",storeId);
        model.addAttribute("q",q); model.addAttribute("low",low);
        model.addAttribute("basePath",request.getRequestURI());
        if (storeId != null) {
            model.addAttribute("stockPage",inventory.inventory(storeId,q,low,page));
            model.addAttribute("history",inventory.history(storeId,historyPage));
            model.addAttribute("products",inventory.products());
            model.addAttribute("requestToken",UUID.randomUUID().toString());
        }
        return "admin/inventory";
    }
    @PostMapping
    public String change(@Valid @ModelAttribute InventoryChangeForm form, BindingResult errors,
                         HttpServletRequest request, RedirectAttributes redirect) {
        try {
            if (errors.hasErrors()) throw new IllegalArgumentException("Vui lòng kiểm tra cửa hàng, sản phẩm, số lượng và lý do.");
            inventory.change(form); redirect.addFlashAttribute("successMessage","Đã ghi nhận phiếu kho.");
        } catch (IllegalArgumentException failure) { redirect.addFlashAttribute("errorMessage",failure.getMessage()); }
        redirect.addAttribute("storeId",form.getStoreId());
        return "redirect:"+request.getRequestURI();
    }
}
