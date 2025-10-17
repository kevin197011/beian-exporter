package io.devops.beian.service;

import io.devops.beian.model.BeianInfo;
import io.devops.beian.model.BeianResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 备案信息查询服务
 */
@Service
public class BeianChecker {

    private static final Logger logger = LoggerFactory.getLogger(BeianChecker.class);
    private static final String BASE_URL = "https://www.beianx.cn/search/";
    private static final Random random = new Random();
    
    private final WebClient webClient;

    public BeianChecker() {
        this.webClient = WebClient.builder()
                .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .defaultHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .defaultHeader("Cache-Control", "no-cache")
                .defaultHeader("Connection", "keep-alive")
                .defaultHeader("Pragma", "no-cache")
                .defaultHeader("Referer", "https://www.beianx.cn/search/")
                .defaultHeader("Sec-Fetch-Dest", "document")
                .defaultHeader("Sec-Fetch-Mode", "navigate")
                .defaultHeader("Sec-Fetch-Site", "same-origin")
                .defaultHeader("Sec-Fetch-User", "?1")
                .defaultHeader("Upgrade-Insecure-Requests", "1")
                .defaultHeader("User-Agent", generateRandomUserAgent())
                .defaultHeader("sec-ch-ua", generateRandomSecChUa())
                .defaultHeader("sec-ch-ua-mobile", "?0")
                .defaultHeader("sec-ch-ua-platform", "\"Windows\"")
                .build();
    }

    /**
     * 查询单个域名的备案信息
     */
    public Mono<BeianResult> checkAsync(String domain, Duration timeout) {
        logger.debug("开始查询域名: {} (添加随机延迟避免被封)", domain);
        
        // 添加随机延迟 1-3秒，避免请求过于规律
        long randomDelay = 1000 + (long)(Math.random() * 2000);
        
        return Mono.delay(Duration.ofMillis(randomDelay))
                .then(webClient.get()
                        .uri(BASE_URL + domain)
                        .header("Cookie", generateRandomCookie())
                        .header("User-Agent", generateRandomUserAgent())
                        .header("sec-ch-ua", generateRandomSecChUa())
                        .header("X-Forwarded-For", generateRandomIP())
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(timeout)
                        .map(this::parseResponse)
                        .doOnSuccess(result -> {
                            if (result.getStatus() == BeianResult.Status.SUCCESS) {
                                logger.info("域名 {} 查询成功: 找到 {} 条备案信息", domain, result.getData().size());
                            } else if (result.getStatus() == BeianResult.Status.NOT_FOUND) {
                                logger.info("域名 {} 查询完成: 未找到备案信息", domain);
                            } else {
                                logger.warn("域名 {} 查询异常: {}", domain, result.getMessage());
                            }
                        })
                        .doOnError(error -> {
                            logger.error("查询域名 {} 网络请求失败: {}", domain, error.getMessage());
                            // 检查是否是超时或网络错误，这些情况适合重试
                            if (error.getMessage().contains("timeout") || 
                                error.getMessage().contains("connection") ||
                                error.getMessage().contains("ConnectException")) {
                                logger.debug("域名 {} 遇到网络问题，适合重试", domain);
                            }
                        })
                        .onErrorReturn(BeianResult.error("查询失败: " + getErrorMessage(domain))));
    }
    
    /**
     * 获取友好的错误信息
     */
    private String getErrorMessage(String domain) {
        return "网络错误或超时，域名: " + domain;
    }

    /**
     * 生成随机Cookie
     */
    private String generateRandomCookie() {
        String antiforgeryToken = generateRandomToken(88);
        String sessionToken = generateRandomSessionToken();
        String macString = UUID.randomUUID().toString();
        String acwTc = generateRandomHexString(32);
        
        return String.format(
            ".AspNetCore.Antiforgery.OGq99nrNx5I=%s; " +
            ".AspNetCore.Session=%s; " +
            "mac_string=%s; " +
            "acw_tc=%s",
            antiforgeryToken,
            sessionToken,
            macString,
            acwTc
        );
    }

    /**
     * 生成随机Token
     */
    private String generateRandomToken(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        StringBuilder token = new StringBuilder("CfDJ8G8XS_ifn0dFsP7uazyotqy");
        
        for (int i = token.length(); i < length; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return token.toString();
    }

    /**
     * 生成随机Session Token
     */
    private String generateRandomSessionToken() {
        String baseToken = "CfDJ8G8XS%2Fifn0dFsP7uazyotqx";
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789%2F";
        StringBuilder token = new StringBuilder(baseToken);
        
        // 添加随机字符到固定长度
        for (int i = 0; i < 60; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return token.toString();
    }

    /**
     * 生成随机十六进制字符串
     */
    private String generateRandomHexString(int length) {
        String chars = "0123456789abcdef";
        StringBuilder hex = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            hex.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return hex.toString();
    }

    /**
     * 生成随机User-Agent
     */
    private String generateRandomUserAgent() {
        String[] versions = {"119.0.0.0", "120.0.0.0", "121.0.0.0", "122.0.0.0"};
        String[] platforms = {"Windows NT 10.0; Win64; x64", "Windows NT 11.0; Win64; x64"};
        
        String version = versions[random.nextInt(versions.length)];
        String platform = platforms[random.nextInt(platforms.length)];
        
        return String.format("Mozilla/5.0 (%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Safari/537.36", 
                platform, version);
    }

    /**
     * 生成随机sec-ch-ua
     */
    private String generateRandomSecChUa() {
        String[] versions = {"119", "120", "121", "122"};
        String version = versions[random.nextInt(versions.length)];
        
        return String.format("\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"%s\", \"Google Chrome\";v=\"%s\"", 
                version, version);
    }

    /**
     * 生成随机IP地址（用于X-Forwarded-For）
     */
    private String generateRandomIP() {
        // 生成一些常见的公网IP段
        String[] ipPrefixes = {
            "114.114.", "8.8.", "1.1.", "223.5.", "180.76.", "202.96."
        };
        
        String prefix = ipPrefixes[random.nextInt(ipPrefixes.length)];
        int third = random.nextInt(255) + 1;
        int fourth = random.nextInt(255) + 1;
        
        return prefix + third + "." + fourth;
    }

    /**
     * 同步查询方法（用于测试）
     */
    public BeianResult check(String domain, Duration timeout) {
        return checkAsync(domain, timeout).block();
    }

    /**
     * 解析HTML响应
     */
    private BeianResult parseResponse(String htmlContent) {
        try {
            List<BeianInfo> beianInfoList = new ArrayList<>();
            
            // 使用正则表达式解析表格数据（保持与Ruby版本一致）
            Pattern rowPattern = Pattern.compile(
                "<tr>.*?<td[^>]*>(\\d+)</td>.*?<td[^>]*>([^<]+)</td>.*?<td[^>]*>([^<]+)</td>.*?<td[^>]*>([^<]+)</td>.*?<td[^>]*>([^<]+)</td>.*?<div>([^<]+)<i.*?<div[^>]*>(\\d{4}-\\d{2}-\\d{2})",
                Pattern.DOTALL | Pattern.MULTILINE
            );
            
            Matcher matcher = rowPattern.matcher(htmlContent);
            
            while (matcher.find()) {
                BeianInfo info = new BeianInfo(
                    matcher.group(1).trim(),  // 序号
                    matcher.group(2).trim(),  // 主办单位名称
                    matcher.group(3).trim(),  // 主办单位性质
                    matcher.group(4).trim(),  // 网站备案号
                    matcher.group(5).trim(),  // 网站名称
                    matcher.group(6).trim(),  // 网站首页地址
                    matcher.group(7).trim()   // 审核日期
                );
                beianInfoList.add(info);
            }
            
            if (beianInfoList.isEmpty()) {
                if (htmlContent.contains("没有找到") || 
                    htmlContent.contains("未找到") || 
                    htmlContent.contains("无记录")) {
                    return BeianResult.notFound("该域名未备案");
                } else {
                    return BeianResult.parseError("解析失败，可能网站结构已变化");
                }
            } else {
                return BeianResult.success(beianInfoList);
            }
            
        } catch (Exception e) {
            logger.error("解析HTML内容时发生错误", e);
            return BeianResult.parseError("解析HTML时发生错误: " + e.getMessage());
        }
    }
}