package com.akash.auditapi.dao;

import com.akash.auditapi.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {}
