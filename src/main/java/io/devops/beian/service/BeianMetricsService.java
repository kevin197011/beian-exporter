package io.devops.beian.service;

import io.devops.beian.model.BeianInfo;
import io.devops.beian.model.BeianResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 备案监控指标服务
 */
@Service
public class BeianMetricsService {

    private final MeterRegistry meterRegistry;
    
    // 存储各域名的状态值
    private final ConcurrentHashMap<String, AtomicLong> beianStatusGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> lastCheckTimeGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> beianInfoGauges = new ConcurrentHashMap<>();
    
    // 计数器缓存
    private final ConcurrentHashMap<String, Counter> errorCounters = new ConcurrentHashMap<>();
    
    // 计时器缓存 - 已禁用
    // private final ConcurrentHashMap<String, Timer> durationTimers = new ConcurrentHashMap<>();

    public BeianMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 更新备案状态指标
     * @param domain 域名
     * @param status 状态 (1=已备案, 0=未备案, -1=查询错误)
     */
    public void updateBeianStatus(String domain, int status) {
        AtomicLong gauge = beianStatusGauges.computeIfAbsent(domain, d -> {
            AtomicLong atomicLong = new AtomicLong(status);
            Gauge.builder("beian_status", atomicLong, AtomicLong::doubleValue)
                    .description("备案状态 (1=已备案, 0=未备案, -1=查询错误)")
                    .tag("domain", d)
                    .register(meterRegistry);
            return atomicLong;
        });
        gauge.set(status);
    }

    /**
     * 更新备案详细信息指标
     */
    public void updateBeianInfo(String domain, BeianResult result) {
        if (!result.isSuccess()) {
            return;
        }
        
        BeianInfo info = result.getData().get(0);
        
        // 为每个域名创建独立的指标
        AtomicLong gauge = beianInfoGauges.computeIfAbsent(domain, d -> {
            AtomicLong atomicLong = new AtomicLong(1);
            Gauge.builder("beian_info", atomicLong, AtomicLong::doubleValue)
                    .description("备案详细信息")
                    .tag("domain", d)
                    .tag("company_name", sanitizeLabel(info.getCompanyName()))
                    .tag("company_type", sanitizeLabel(info.getCompanyType()))
                    .tag("beian_number", sanitizeLabel(info.getBeianNumber()))
                    .tag("website_name", sanitizeLabel(info.getWebsiteName()))
                    .tag("website_url", sanitizeLabel(info.getWebsiteUrl()))
                    .tag("approval_date", sanitizeLabel(info.getApprovalDate()))
                    .register(meterRegistry);
            return atomicLong;
        });
        gauge.set(1);
    }
    
    /**
     * 清理标签值，确保符合Prometheus规范
     */
    private String sanitizeLabel(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        // 移除可能导致Prometheus解析问题的字符
        return value.trim()
                .replace("\"", "")
                .replace("\\", "")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ");
    }

    /**
     * 记录查询耗时 - 已禁用
     */
    public void recordCheckDuration(String domain, Duration duration) {
        // 不再记录查询耗时指标
        // Timer timer = durationTimers.computeIfAbsent(domain, d ->
        //         Timer.builder("beian_check_duration_seconds")
        //                 .description("备案查询耗时")
        //                 .tag("domain", d)
        //                 .register(meterRegistry)
        // );
        // timer.record(duration);
    }

    /**
     * 增加错误计数
     */
    public void incrementError(String domain, String errorType) {
        String key = domain + ":" + errorType;
        Counter counter = errorCounters.computeIfAbsent(key, k ->
                Counter.builder("beian_check_errors_total")
                        .description("备案查询错误次数")
                        .tag("domain", domain)
                        .tag("error_type", errorType)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 更新最后检查时间
     */
    public void updateLastCheckTime(String domain) {
        long currentTime = System.currentTimeMillis() / 1000; // Unix timestamp
        
        AtomicLong gauge = lastCheckTimeGauges.computeIfAbsent(domain, d -> {
            AtomicLong atomicLong = new AtomicLong(currentTime);
            Gauge.builder("beian_last_check_timestamp", atomicLong, AtomicLong::doubleValue)
                    .description("最后检查时间戳")
                    .tag("domain", d)
                    .register(meterRegistry);
            return atomicLong;
        });
        gauge.set(currentTime);
    }

    /**
     * 根据查询结果更新所有相关指标
     */
    public void updateMetrics(String domain, BeianResult result) {
        switch (result.getStatus()) {
            case SUCCESS:
                updateBeianStatus(domain, 1);
                updateBeianInfo(domain, result);
                break;
            case NOT_FOUND:
                updateBeianStatus(domain, 0);
                incrementError(domain, "not_found");
                break;
            case PARSE_ERROR:
                updateBeianStatus(domain, -1);
                incrementError(domain, "parse_error");
                break;
            case ERROR:
                updateBeianStatus(domain, -1);
                incrementError(domain, "query_error");
                break;
            default:
                updateBeianStatus(domain, -1);
                incrementError(domain, "unknown_error");
                break;
        }
        
        updateLastCheckTime(domain);
    }
}