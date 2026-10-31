package com.oneshop.controller;

import com.oneshop.dto.request.StoreForm;
import com.oneshop.service.StoreManagementService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/stores")
public class StoreManagementController {
    private final StoreManagementService service;
    public StoreManagementController(StoreManagementService service){this.service=service;}
    @GetMapping public String list(Model m){m.addAttribute("stores",service.list());return "admin/stores";}
    @GetMapping({"/new","/{id}/edit"}) public String form(@PathVariable(required=false) Long id, Model m){
        m.addAttribute("storeForm",service.form(id));m.addAttribute("storeId",id);return "admin/store-form";
    }
    @PostMapping({"","/{id}"}) public String save(@PathVariable(required=false) Long id,@Valid @ModelAttribute("storeForm") StoreForm form,
            BindingResult errors,Model m,RedirectAttributes redirect){
        if(!errors.hasErrors()){
            try{service.save(id,form);redirect.addFlashAttribute("successMessage","Đã lưu cửa hàng.");return "redirect:/admin/stores";}
            catch(IllegalArgumentException e){errors.reject("store.invalid",e.getMessage());}
            catch(org.springframework.dao.DataIntegrityViolationException e){errors.reject("store.duplicate","Mã cửa hàng đã được sử dụng.");}
        }
        m.addAttribute("storeId",id);return "admin/store-form";
    }
}
