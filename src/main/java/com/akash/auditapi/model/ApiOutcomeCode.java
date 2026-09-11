package com.akash.auditapi.model;

public enum ApiOutcomeCode {
    SUCCESS("1000"),
    UNKNOWN_ERROR("0000");

    private final String internalCode;

    ApiOutcomeCode(String internalCode) {
        this.internalCode = internalCode;
    }

    public String internalCode() {
        return internalCode;
    }
}
