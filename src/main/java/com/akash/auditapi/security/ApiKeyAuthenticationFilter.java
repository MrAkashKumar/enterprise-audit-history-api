package com.akash.auditapi.security;

import com.akash.auditapi.exception.ApiError;
import com.akash.auditapi.exception.ApiErrorCode;
import com.akash.auditapi.exception.ApiErrorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static com.akash.auditapi.config.ApiPaths.V1_PREFIX;
import static com.akash.auditapi.exception.ApiMessages.VALID_API_KEY_REQUIRED;
import static com.akash.auditapi.trace.TraceContext.HEADER_NAME;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private final ApiKeyProperties properties;
    private final ObjectMapper objectMapper;
    private final ApiErrorFactory errorFactory;

    public ApiKeyAuthenticationFilter(ApiKeyProperties properties, ObjectMapper objectMapper,
                                      ApiErrorFactory errorFactory) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.errorFactory = errorFactory;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !request.getRequestURI().startsWith(V1_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String suppliedKey = request.getHeader(properties.headerName());
        if (matches(suppliedKey, properties.key())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = errorFactory.create(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED,
                VALID_API_KEY_REQUIRED);
        if (error.getTraceId() != null) {
            response.setHeader(HEADER_NAME, error.getTraceId());
        }
        objectMapper.writeValue(response.getOutputStream(), error);
    }

    private boolean matches(String suppliedKey, String configuredKey) {
        if (suppliedKey == null) {
            return false;
        }
        return MessageDigest.isEqual(suppliedKey.getBytes(StandardCharsets.UTF_8),
                configuredKey.getBytes(StandardCharsets.UTF_8));
    }
}
