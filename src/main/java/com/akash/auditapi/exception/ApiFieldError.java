package com.akash.auditapi.exception;

/**
 * Describes one invalid request field and its user-readable validation message.
 * Validation error responses return these entries in their details array.
 */
public record ApiFieldError(String field, String message) {
}
