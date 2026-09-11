package com.akash.auditapi;

import com.akash.auditapi.config.AuditApiProperties;
import com.akash.auditapi.security.ApiKeyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AuditApiProperties.class, ApiKeyProperties.class})
public class AuditApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditApiApplication.class, args);
    }
}
