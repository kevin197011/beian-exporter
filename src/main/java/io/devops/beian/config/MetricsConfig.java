package io.devops.beian.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义指标配置
 */
@Configuration
public class MetricsConfig {

    /**
     * 自定义 MeterRegistry 配置
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> beianMetricsCustomizer() {
        return registry -> {
            // 添加通用标签
            registry.config()
                    .commonTags("application", "beian-exporter")
                    .commonTags("version", "1.0.0");
            
            // 配置指标过滤器 - 只保留我们需要的指标
            registry.config()
                    .meterFilter(MeterFilter.accept(id -> {
                        String name = id.getName();
                        // 只保留beian相关的指标和基本的应用指标
                        return name.startsWith("beian_") || 
                               name.equals("application_ready_time") ||
                               name.equals("application_started_time");
                    }));
        };
    }
}