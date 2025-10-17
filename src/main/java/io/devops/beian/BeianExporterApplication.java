package io.devops.beian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Beian Exporter 主应用类
 * 基于 Spring Boot 的备案信息 Prometheus 导出器
 */
@SpringBootApplication
@EnableScheduling
public class BeianExporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeianExporterApplication.class, args);
    }
}