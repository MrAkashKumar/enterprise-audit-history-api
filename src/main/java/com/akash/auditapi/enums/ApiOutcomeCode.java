package com.akash.auditapi.enums;

public enum ApiOutcomeCode {
    SUCCESS("2000"),
    REDIRECTION("3000"),
    INVALID_TABLE_NAME("4000"),
    INVALID_PAGE_NO("4001"),
    INVALID_PAGE_SIZE("4002"),
    AUDIT_TABLE_NOT_ACCEPTED("4003"),
    VALIDATION_FAILED("4004"),
    INVALID_REQUEST("4005"),
    TABLE_NOT_ALLOWED("4007"),
    TABLE_PAIR_NOT_FOUND("4008"),
    MISSING_REQUIRED_COLUMN("4009"),
    HOLIDAY_NOT_FOUND("4010"),
    INTERNAL_ERROR("5000"),
    DATABASE_ERROR("5001");

    private final String applicationCode;

    ApiOutcomeCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public String applicationCode() {
        return applicationCode;
    }
}
