package com.akash.auditapi.enums;

public enum RevisionOperation {
    INSERT(0), UPDATE(1), DELETE(2), UNKNOWN(-1);

    private final int code;

    RevisionOperation(int code) { this.code = code; }

    public int code() { return code; }

    public static RevisionOperation from(Object value) {
        if (value instanceof Number number) {
            int code = number.intValue();
            for (RevisionOperation operation : values()) {
                if (operation.code == code) {
                    return operation;
                }
            }
            return UNKNOWN;
        }
        if (value != null) {
            try {
                return from(Integer.parseInt(value.toString()));
            } catch (NumberFormatException ignored) {
                // A nonnumeric custom REVTYPE remains visible as UNKNOWN and in raw data.
            }
        }
        return UNKNOWN;
    }
}
