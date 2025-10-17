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
        try {
            model.addAttribute("version", "1.0.0");
            model.addAttribute("domainCount", beianProperties.getDomains().size());
            model.addAttribute("checkInterval", beianProperties.getCheckInterval());
            model.addAttribute("domains", beianProperties.getDomains());
            
            return "index";
        } catch (Exception e) {
            // 如果模板有问题，返回简单的HTML
            return "redirect:/simple";
        }
    }
    
    /**
     * 简单主页（用于调试）
     */
    @GetMapping("/simple")
    @org.springframework.web.bind.annotation.ResponseBody
    public String simpleHome() {
        return "<html><body><h1>Beian Exporter</h1><p>Application is running!</p>" +
               "<p>Monitoring " + beianProperties.getDomains().size() + " domains</p>" +
               "<p><a href='/prometheus'>Prometheus Metrics</a></p>" +
               "<p><a href='/health'>Health Check</a></p>" +
               "<p><a href='/api/config'>Configuration</a></p></body></html>";
    }
}