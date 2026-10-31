package com.oneshop.controller;

import com.oneshop.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping({"/admin/reports","/branch/reports"})
public class SalesReportController {
    private final SalesReportService reports;
    private final InventoryService inventory;
    public SalesReportController(SalesReportService reports,InventoryService inventory){this.reports=reports;this.inventory=inventory;}
    @GetMapping public String page(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) Long storeId,Model m,HttpServletRequest req){
        LocalDate end=to==null?LocalDate.now():to, start=from==null?end.minusDays(29):from;
        m.addAttribute("stores",inventory.allowedStores());m.addAttribute("basePath",req.getRequestURI());
        m.addAttribute("report",reports.report(start,end,storeId));return "admin/reports";
    }
    @GetMapping("/export") @ResponseBody public ResponseEntity<byte[]> export(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) Long storeId){
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=OneShop_DoanhThu.xlsx")
                .body(SalesReportWorkbook.write(reports.report(from,to,storeId)));
    }
}
