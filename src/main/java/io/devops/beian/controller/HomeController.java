package io.devops.beian.controller;

import io.devops.beian.config.BeianProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 主页控制器
 */
@Controller
public class HomeController {

    private final BeianProperties beianProperties;

    public HomeController(BeianProperties beianProperties) {
        this.beianProperties = beianProperties;
    }

    /**
     * 主页
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("version", "1.0.0");
        model.addAttribute("domainCount", beianProperties.getDomains().size());
        model.addAttribute("checkInterval", beianProperties.getCheckInterval());
        model.addAttribute("domains", beianProperties.getDomains());
        
        return "index";
    }
}