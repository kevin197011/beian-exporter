package io.devops.beian.controller;

import io.devops.beian.config.BeianProperties;
import io.devops.beian.model.BeianResult;
import io.devops.beian.service.BeianScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 备案查询控制器
 */
@RestController
@RequestMapping("/api")
public class BeianController {

    private final BeianScheduler beianScheduler;
    private final BeianProperties beianProperties;

    public BeianController(BeianScheduler beianScheduler, BeianProperties beianProperties) {
        this.beianScheduler = beianScheduler;
        this.beianProperties = beianProperties;
    }

    /**
     * 手动触发所有域名检查
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> triggerCheck() {
        beianScheduler.triggerCheck();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "备案检查已触发");
        response.put("domains", beianProperties.getDomains());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查单个域名
     */
    @GetMapping("/check/{domain}")
    public ResponseEntity<BeianResult> checkDomain(@PathVariable String domain) {
        BeianResult result = beianScheduler.checkSingleDomain(domain);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取配置信息
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("checkInterval", beianProperties.getCheckInterval());
        config.put("requestTimeout", beianProperties.getRequestTimeout());
        config.put("requestDelay", beianProperties.getRequestDelay());
        config.put("maxRetries", beianProperties.getMaxRetries());
        config.put("domains", beianProperties.getDomains());
        
        return ResponseEntity.ok(config);
    }
}