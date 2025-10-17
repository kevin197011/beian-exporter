package io.devops.beian.service;

import io.devops.beian.config.BeianProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流服务
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final BeianProperties beianProperties;
    private final ConcurrentLinkedQueue<Instant> requestTimes = new ConcurrentLinkedQueue<>();
    private final AtomicInteger currentRequests = new AtomicInteger(0);

    public RateLimitService(BeianProperties beianProperties) {
        this.beianProperties = beianProperties;
    }

    /**
     * 检查是否允许发送请求
     * @return true 如果允许，false 如果需要等待
     */
    public boolean tryAcquire() {
        cleanOldRequests();
        
        int maxRequests = beianProperties.getRateLimit().getMaxRequestsPerMinute();
        int burstSize = beianProperties.getRateLimit().getBurstSize();
        
        // 检查突发限制
        if (currentRequests.get() >= burstSize) {
            logger.debug("达到突发限制，当前请求数: {}", currentRequests.get());
            return false;
        }
        
        // 检查每分钟限制
        if (requestTimes.size() >= maxRequests) {
            logger.debug("达到每分钟限制，当前请求数: {}", requestTimes.size());
            return false;
        }
        
        // 记录请求
        requestTimes.offer(Instant.now());
        currentRequests.incrementAndGet();
        
        return true;
    }

    /**
     * 请求完成后调用
     */
    public void release() {
        currentRequests.decrementAndGet();
    }

    /**
     * 等待直到可以发送请求
     */
    public void waitForPermit() throws InterruptedException {
        while (!tryAcquire()) {
            logger.debug("等待限流释放...");
            Thread.sleep(1000); // 等待1秒后重试
        }
    }

    /**
     * 清理超过1分钟的旧请求记录
     */
    private void cleanOldRequests() {
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        
        while (!requestTimes.isEmpty() && requestTimes.peek().isBefore(oneMinuteAgo)) {
            requestTimes.poll();
        }
    }

    /**
     * 获取当前请求统计
     */
    public RequestStats getStats() {
        cleanOldRequests();
        return new RequestStats(
                requestTimes.size(),
                currentRequests.get(),
                beianProperties.getRateLimit().getMaxRequestsPerMinute(),
                beianProperties.getRateLimit().getBurstSize()
        );
    }

    /**
     * 请求统计信息
     */
    public static class RequestStats {
        private final int requestsInLastMinute;
        private final int currentBurstRequests;
        private final int maxRequestsPerMinute;
        private final int maxBurstSize;

        public RequestStats(int requestsInLastMinute, int currentBurstRequests, 
                           int maxRequestsPerMinute, int maxBurstSize) {
            this.requestsInLastMinute = requestsInLastMinute;
            this.currentBurstRequests = currentBurstRequests;
            this.maxRequestsPerMinute = maxRequestsPerMinute;
            this.maxBurstSize = maxBurstSize;
        }

        public int getRequestsInLastMinute() {
            return requestsInLastMinute;
        }

        public int getCurrentBurstRequests() {
            return currentBurstRequests;
        }

        public int getMaxRequestsPerMinute() {
            return maxRequestsPerMinute;
        }

        public int getMaxBurstSize() {
            return maxBurstSize;
        }

        @Override
        public String toString() {
            return String.format("RequestStats{requests/min: %d/%d, burst: %d/%d}", 
                    requestsInLastMinute, maxRequestsPerMinute, 
                    currentBurstRequests, maxBurstSize);
        }
    }
}