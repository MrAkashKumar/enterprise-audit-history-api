package com.akash.auditapi.dto;

/**
 * Represents the narrow approval projection loaded for one source entity.
 * Approval payloads and comments are intentionally excluded from the public workflow.
 */
public record ApprovalRecord(Object id, String makerUsername, String checkerUsername) {
}
