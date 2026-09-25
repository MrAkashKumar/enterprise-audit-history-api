package com.akash.auditapi.dao;

import com.akash.auditapi.entity.CommodityDebit;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Retrieves commodity debit report data from Oracle through Spring Data JPA.
 * Report generation is read-only and uses the inherited {@code findById} operation.
 */
public interface CommodityDebitRepository extends JpaRepository<CommodityDebit, Long> {
}
