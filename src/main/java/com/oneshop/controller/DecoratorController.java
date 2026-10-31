package com.oneshop.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * SiteMesh includes decorators using the original form's HTTP method.
 * Spring's default static resource handler rejects POST, hiding validation errors.
 */
@Controller
public class DecoratorController {
    @ResponseBody
    @RequestMapping(value = "/decorators/main.html", produces = MediaType.TEXT_HTML_VALUE,
            method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST})
    public Resource storefront() {
        return new ClassPathResource("static/decorators/main.html");
    }

    @ResponseBody
    @RequestMapping(value = "/decorators/admin.html", produces = MediaType.TEXT_HTML_VALUE,
            method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST})
    public Resource admin() {
        return new ClassPathResource("static/decorators/admin.html");
    }
    @ResponseBody
    @RequestMapping(value = "/decorators/branch.html", produces = MediaType.TEXT_HTML_VALUE,
            method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST})
    public Resource branch() { return new ClassPathResource("static/decorators/branch.html"); }
}
