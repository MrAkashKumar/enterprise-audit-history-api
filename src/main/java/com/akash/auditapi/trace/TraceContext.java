package com.akash.auditapi.trace;

import org.slf4j.MDC;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class TraceContext {
    public static final String HEADER_NAME = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";
    private static final int MAX_TRACE_ID_LENGTH = 64;
    private static final int GENERATED_TRACE_ID_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final Pattern VALID_TRACE_ID =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0," + (MAX_TRACE_ID_LENGTH - 1) + "}");

    private TraceContext() {
    }

    public static String resolve(String suppliedTraceId) {
        if (suppliedTraceId != null && VALID_TRACE_ID.matcher(suppliedTraceId).matches()) {
            return suppliedTraceId;
        }
        byte[] randomBytes = new byte[GENERATED_TRACE_ID_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return HEX_FORMAT.formatHex(randomBytes);
    }

    public static String currentTraceId() {
        return MDC.get(MDC_KEY);
    }
}
