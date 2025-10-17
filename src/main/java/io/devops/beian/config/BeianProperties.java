package io.devops.beian.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 备案查询配置属性
 */
@Component
@ConfigurationProperties(prefix = "beian")
@Validated
public class BeianProperties {

    /**
     * 检查间隔（秒）
     */
    @Min(60)
    private int checkInterval = 21600; // 6小时

    /**
     * 请求超时（秒）
     */
    @Min(1)
    private int requestTimeout = 30;

    /**
     * 请求间隔（秒）
     */
    @Min(0)
    private int requestDelay = 2;

    /**
     * 最大重试次数
     */
    @Min(0)
    private int maxRetries = 3;

    /**
     * 限流配置
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * 要监控的域名列表
     */
    @NotEmpty(message = "域名列表不能为空")
    private List<@NotNull String> domains;

    /**
     * 限流配置类
     */
    public static class RateLimit {
        /**
         * 每分钟最大请求数
         */
        @Min(1)
        private int maxRequestsPerMinute = 10;

        /**
         * 突发请求数量
         */
        @Min(1)
        private int burstSize = 3;

        public int getMaxRequestsPerMinute() {
            return maxRequestsPerMinute;
        }

        public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
            this.maxRequestsPerMinute = maxRequestsPerMinute;
        }

        public int getBurstSize() {
            return burstSize;
        }

        public void setBurstSize(int burstSize) {
            this.burstSize = burstSize;
        }
    }

    // Getters and Setters
    public int getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public int getRequestDelay() {
        return requestDelay;
    }

    public void setRequestDelay(int requestDelay) {
        this.requestDelay = requestDelay;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }
}