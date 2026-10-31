package com.oneshop.controller;

import com.oneshop.service.ProductWatchService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account/watches")
public class ProductWatchController {
    private final ProductWatchService watches;
    public ProductWatchController(ProductWatchService watches){this.watches=watches;}
    @GetMapping public String list(Principal principal,Model model){model.addAttribute("watches",watches.list(principal.getName()));return "pages/product-watches";}
    @PostMapping public String subscribe(Principal principal,@RequestParam Long productId,@RequestParam(defaultValue="false") boolean emailEnabled,RedirectAttributes redirect){
        try{watches.subscribe(principal.getName(),productId,emailEnabled);redirect.addFlashAttribute("watchSuccess",emailEnabled ? "Đã theo dõi: nhận thông báo trên web và qua email tài khoản khi có hàng." : "Đã theo dõi: nhận thông báo qua biểu tượng chuông trên web. Không gửi email.");}
        catch(IllegalArgumentException failure){redirect.addFlashAttribute("watchError",failure.getMessage());}
        return "redirect:/account/watches";
    }
    @PostMapping("/{id}/cancel") public String cancel(Principal principal,@PathVariable Long id,RedirectAttributes redirect){
        watches.cancel(principal.getName(),id);redirect.addFlashAttribute("watchSuccess","Đã bỏ theo dõi sản phẩm.");return "redirect:/account/watches";
    }
    @GetMapping("/notifications") @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String,Long>> notifications(Principal principal){
        return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(java.util.Map.of("unread",watches.unread(principal.getName())));
    }
    @PostMapping("/{id}/read") public String read(Principal principal,@PathVariable Long id){watches.read(principal.getName(),id);return "redirect:/account/watches";}
    @PostMapping("/{id}/preferences") public String preferences(Principal principal,@PathVariable Long id,
            @RequestParam(defaultValue="false") boolean emailEnabled,RedirectAttributes redirect){
        watches.preferences(principal.getName(),id,emailEnabled);redirect.addFlashAttribute("watchSuccess",emailEnabled?"Đã bật nhận thêm email có hàng.":"Đã tắt email; bạn vẫn nhận thông báo trên web.");return "redirect:/account/watches";
    }

}
