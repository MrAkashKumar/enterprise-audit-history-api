package com.akash.auditapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Objects;

public abstract class BaseApiResponse<C> {
    private final Instant timestamp;
    private final int status;
    private final C code;
    private final String message;

    protected BaseApiResponse(Instant timestamp, int status, C code, String message) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.status = status;
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public C getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
