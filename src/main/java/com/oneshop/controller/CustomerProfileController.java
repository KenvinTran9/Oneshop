package com.oneshop.controller;

import com.oneshop.dto.request.*;
import com.oneshop.service.CustomerProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account/profile")
public class CustomerProfileController {
    private final CustomerProfileService service;
    public CustomerProfileController(CustomerProfileService service){this.service=service;}
    @GetMapping public String page(Principal p,Model m){m.addAttribute("profile",service.profile(p.getName()));m.addAttribute("addresses",service.addresses(p.getName()));return "pages/profile";}
    @PostMapping public String update(@Valid @ModelAttribute ProfileForm form,BindingResult errors,Principal p,RedirectAttributes r){
        if(errors.hasErrors()){r.addFlashAttribute("profileError","Họ tên tối đa 120 ký tự; điện thoại từ 8 đến 20 ký tự hợp lệ.");}
        else{service.update(p.getName(),form);r.addFlashAttribute("profileSuccess","Đã cập nhật hồ sơ.");}return "redirect:/account/profile";
    }
    @PostMapping("/addresses") public String add(@Valid @ModelAttribute AddressForm form,BindingResult errors,Principal p,RedirectAttributes r){
        try{if(errors.hasErrors())throw new IllegalArgumentException("Kiểm tra tên địa chỉ, người nhận, số điện thoại và địa chỉ giao hàng.");service.addAddress(p.getName(),form);r.addFlashAttribute("profileSuccess","Đã lưu địa chỉ.");}
        catch(IllegalArgumentException e){r.addFlashAttribute("profileError",e.getMessage());}return "redirect:/account/profile";
    }
    @PostMapping("/addresses/{id}/delete") public String remove(@PathVariable Long id,Principal p,RedirectAttributes r){service.removeAddress(p.getName(),id);r.addFlashAttribute("profileSuccess","Đã xóa địa chỉ.");return "redirect:/account/profile";}
    @PostMapping("/password") public String password(@Valid @ModelAttribute ChangePasswordForm form,BindingResult errors,Principal p,HttpServletRequest request,RedirectAttributes r){
        try{
            if(errors.hasErrors())throw new IllegalArgumentException("Mật khẩu mới từ 8 ký tự, tối đa 72 byte và xác nhận phải khớp.");
            service.changePassword(p.getName(),form);request.getSession().invalidate();org.springframework.security.core.context.SecurityContextHolder.clearContext();return "redirect:/login?passwordChanged";
        }catch(IllegalArgumentException e){r.addFlashAttribute("profileError",e.getMessage());return "redirect:/account/profile";}
    }
}
