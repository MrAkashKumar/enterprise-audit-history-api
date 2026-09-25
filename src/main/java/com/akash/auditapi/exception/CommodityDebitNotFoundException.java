package com.akash.auditapi.exception;

import com.akash.auditapi.enums.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

import static com.akash.auditapi.exception.ApiMessages.commodityDebitNotFound;

/**
 * Signals that the database row required for a commodity debit report does not exist.
 * The global exception handler converts it to the application's standard JSON error contract.
 */
public class CommodityDebitNotFoundException extends AuditApiException {
    public CommodityDebitNotFoundException(Long id) {
        super(ApiOutcomeCode.COMMODITY_DEBIT_NOT_FOUND,
                commodityDebitNotFound(id), HttpStatus.NOT_FOUND);
    }
}
