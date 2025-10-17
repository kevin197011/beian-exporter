package io.devops.beian.service;

import io.devops.beian.config.BeianProperties;
import io.devops.beian.model.BeianResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 备案检查调度服务
 */
@Service
public class BeianScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BeianScheduler.class);

    private final BeianProperties beianProperties;
    private final BeianChecker beianChecker;
    private final BeianMetricsService metricsService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public BeianScheduler(BeianProperties beianProperties, 
                          BeianChecker beianChecker, 
                          BeianMetricsService metricsService) {
        this.beianProperties = beianProperties;
        this.beianChecker = beianChecker;
        this.metricsService = metricsService;
    }

    /**
     * 定时执行备案检查
     * 使用 Spring 的 @Scheduled 注解，间隔时间从配置文件读取
     */
    @Scheduled(fixedDelayString = "#{${beian.check-interval:21600} * 1000}")
    public void scheduleCheck() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("上一次检查仍在进行中，跳过本次检查");
            return;
        }

        try {
            logger.info("开始检查所有域名备案状态，共 {} 个域名", beianProperties.getDomains().size());
            logger.info("为避免被封禁，将串行检查，每个域名间隔 {} 秒", beianProperties.getRequestDelay());
            
            // 改为串行处理，避免并发请求被封禁
            Flux.fromIterable(beianProperties.getDomains())
                    .concatMap(domain -> {
                        // 每个域名检查前先等待配置的延迟时间
                        return Mono.delay(Duration.ofSeconds(beianProperties.getRequestDelay()))
                                .then(this.checkDomainWithRetry(domain));
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnComplete(() -> {
                        logger.info("所有域名检查完成");
                        isRunning.set(false);
                    })
                    .doOnError(error -> {
                        logger.error("批量检查过程中发生错误", error);
                        isRunning.set(false);
                    })
                    .subscribe();
                    
        } catch (Exception e) {
            logger.error("启动批量检查时发生错误", e);
            isRunning.set(false);
        }
    }

    /**
     * 带重试机制的域名检查
     */
    private Mono<Void> checkDomainWithRetry(String domain) {
        return checkDomain(domain)
                .retryWhen(reactor.util.retry.Retry.backoff(beianProperties.getMaxRetries(), Duration.ofSeconds(5))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(retrySignal -> {
                            long retryCount = retrySignal.totalRetries() + 1;
                            Throwable failure = retrySignal.failure();
                            logger.warn("域名 {} 第 {} 次重试 (共{}次)，失败原因: {}，等待 {} 秒后重试", 
                                    domain, retryCount, beianProperties.getMaxRetries(), 
                                    failure.getMessage(), 5 * retryCount);
                        })
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            logger.error("域名 {} 重试 {} 次后仍然失败，放弃重试", 
                                    domain, beianProperties.getMaxRetries());
                            return retrySignal.failure();
                        }))
                .onErrorResume(error -> {
                    logger.error("域名 {} 最终查询失败: {}", domain, error.getMessage());
                    
                    // 记录错误指标
                    metricsService.updateMetrics(domain, BeianResult.error(error.getMessage()));
                    return Mono.empty();
                });
    }

    /**
     * 检查单个域名
     */
    private Mono<Void> checkDomain(String domain) {
        Instant startTime = Instant.now();
        
        return beianChecker.checkAsync(domain, Duration.ofSeconds(beianProperties.getRequestTimeout()))
                .doOnNext(result -> {
                    // 记录耗时
                    Duration duration = Duration.between(startTime, Instant.now());
                    metricsService.recordCheckDuration(domain, duration);
                    
                    // 更新指标
                    metricsService.updateMetrics(domain, result);
                    
                    logger.info("域名 {} 检查完成: {}, 耗时: {}ms", 
                            domain, result.getStatus(), duration.toMillis());
                })
                .then();
    }

    /**
     * 手动触发检查（用于测试或立即检查）
     */
    public void triggerCheck() {
        logger.info("手动触发备案检查");
        scheduleCheck();
    }

    /**
     * 检查单个域名（同步方法，用于API调用）
     */
    public BeianResult checkSingleDomain(String domain) {
        logger.info("手动检查单个域名: {} (最多重试{}次)", domain, beianProperties.getMaxRetries());
        
        Instant startTime = Instant.now();
        BeianResult result = null;
        
        // 手动实现重试逻辑
        for (int attempt = 1; attempt <= beianProperties.getMaxRetries() + 1; attempt++) {
            try {
                result = beianChecker.check(domain, Duration.ofSeconds(beianProperties.getRequestTimeout()));
                
                // 如果查询成功或者是业务层面的失败（如未备案），则不重试
                if (result != null && !result.getStatus().equals(BeianResult.Status.ERROR)) {
                    if (attempt > 1) {
                        logger.info("域名 {} 在第 {} 次尝试后成功", domain, attempt);
                    }
                    break;
                }
                
                // 如果是错误状态且还有重试机会
                if (attempt <= beianProperties.getMaxRetries()) {
                    logger.warn("域名 {} 第 {} 次查询失败: {}，等待 {} 秒后重试", 
                            domain, attempt, result != null ? result.getError() : "未知错误", 3 * attempt);
                    Thread.sleep(3000 * attempt); // 递增延迟
                }
                
            } catch (Exception e) {
                logger.warn("域名 {} 第 {} 次查询异常: {}", domain, attempt, e.getMessage());
                
                if (attempt <= beianProperties.getMaxRetries()) {
                    try {
                        Thread.sleep(3000 * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    result = BeianResult.error("查询异常: " + e.getMessage());
                }
            }
        }
        
        if (result == null) {
            result = BeianResult.error("查询失败: 超过最大重试次数");
        }
        
        Duration duration = Duration.between(startTime, Instant.now());
        metricsService.recordCheckDuration(domain, duration);
        metricsService.updateMetrics(domain, result);
        
        logger.info("域名 {} 查询完成: {}, 总耗时: {}ms", domain, result.getStatus(), duration.toMillis());
        return result;
    }
}