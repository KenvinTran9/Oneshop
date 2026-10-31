package com.oneshop.controller;

import com.oneshop.service.AccountManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/users")
public class AccountManagementController {
    private final AccountManagementService service;
    public AccountManagementController(AccountManagementService service){this.service=service;}
    @GetMapping public String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model m){
        m.addAttribute("userPage",service.list(q,page));m.addAttribute("stores",service.stores());m.addAttribute("q",q);return "admin/users";
    }
    @PostMapping("/{id}") public String update(@PathVariable Long id,@RequestParam String role,@RequestParam(required=false) Long storeId,
            @RequestParam(defaultValue="false") boolean active,RedirectAttributes redirect){
        try{service.update(id,role,storeId,active);redirect.addFlashAttribute("successMessage","Đã cập nhật tài khoản. Người dùng cần đăng nhập lại nếu quyền hoặc trạng thái thay đổi.");}
        catch(IllegalArgumentException e){redirect.addFlashAttribute("errorMessage",e.getMessage());}
        return "redirect:/admin/users";
    }
}
