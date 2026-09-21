package com.akash.auditapi.dto;

/**
 * Holds verified identifiers used to read maker/checker data safely.
 * Instances are created only after Oracle metadata validation succeeds.
 */
public record ApprovalTableDescriptor(String tableName, String idColumn,
                                      String makerUsernameColumn, String checkerUsernameColumn) {
}
