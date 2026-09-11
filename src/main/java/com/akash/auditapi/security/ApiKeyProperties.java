package com.akash.auditapi.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.akash.auditapi.exception.ApiMessages.API_KEY_CONFIGURATION_REQUIRED;
import static com.akash.auditapi.config.AuditDefaults.DEFAULT_API_KEY_HEADER;

@ConfigurationProperties(prefix = "audit-api.security")
public record ApiKeyProperties(boolean enabled, String headerName, String key) {
    public ApiKeyProperties {
        headerName = headerName == null || headerName.isBlank() ? DEFAULT_API_KEY_HEADER : headerName;
        key = key == null ? "" : key;
        if (enabled && key.isBlank()) {
            throw new IllegalArgumentException(API_KEY_CONFIGURATION_REQUIRED);
        }
    }
}
