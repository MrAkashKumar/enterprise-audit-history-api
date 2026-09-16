package com.akash.auditapi.dao;

import com.akash.auditapi.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides standard JPA persistence operations for {@link Holiday} records.
 * It is used only by the typed Holiday feature.
 */
public interface HolidayRepository extends JpaRepository<Holiday, Long> {}
