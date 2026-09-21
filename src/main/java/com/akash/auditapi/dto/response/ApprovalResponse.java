package com.akash.auditapi.dto.response;

/**
 * Exposes whether an approval row exists and its maker/checker usernames.
 * Missing tables or rows use a stable object with null usernames.
 */
public record ApprovalResponse(boolean approvalRecordPresent, String makerUsername,
                               String checkerUsername) {
    public static final ApprovalResponse ABSENT = new ApprovalResponse(false, null, null);

    public static ApprovalResponse present(String makerUsername, String checkerUsername) {
        return new ApprovalResponse(true, makerUsername, checkerUsername);
    }
}
