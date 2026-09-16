package com.akash.auditapi;

import com.akash.auditapi.config.AuditApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Starts the Enterprise Audit History Spring Boot application.
 * It also enables binding and validation of the audit API configuration.
 */
@SpringBootApplication
@EnableConfigurationProperties(AuditApiProperties.class)
public class AuditApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditApiApplication.class, args);
    }
}
