package com.akash.auditapi.security;

import com.akash.auditapi.exception.ApiError;
import com.akash.auditapi.exception.ApiErrorCode;
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
import java.time.Instant;
import java.util.List;

import static com.akash.auditapi.config.ApiPaths.V1_PREFIX;
import static com.akash.auditapi.exception.ApiMessages.VALID_API_KEY_REQUIRED;
import static com.akash.auditapi.trace.TraceContext.currentTraceId;
import static com.akash.auditapi.trace.TraceContext.HEADER_NAME;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private final ApiKeyProperties properties;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(ApiKeyProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
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
        String traceId = currentTraceId();
        if (traceId != null) {
            response.setHeader(HEADER_NAME, traceId);
        }
        ApiError error = new ApiError(Instant.now(), traceId, HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), ApiErrorCode.UNAUTHORIZED,
                VALID_API_KEY_REQUIRED, List.of());
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
