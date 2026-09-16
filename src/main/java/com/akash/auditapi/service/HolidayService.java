package com.akash.auditapi.service;

import com.akash.auditapi.dao.HolidayRepository;
import com.akash.auditapi.dto.request.HolidayRequest;
import com.akash.auditapi.entity.Holiday;
import com.akash.auditapi.exception.HolidayNotFoundException;
import com.akash.auditapi.validation.PaginationValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements transactional CRUD operations for the typed Holiday feature.
 * It maps between validated request DTOs, JPA entities, and response DTOs.
 */
@Service
public class HolidayService {
    private final HolidayRepository repository;
    private final PaginationValidator paginationValidator;

    public HolidayService(HolidayRepository repository, PaginationValidator paginationValidator) {
        this.repository = repository;
        this.paginationValidator = paginationValidator;
    }

    @Transactional(readOnly = true)
    public Page<Holiday> findAll(int pageNo, int pageSize) {
        paginationValidator.validate(pageNo, pageSize);
        Pageable page = PageRequest.of(pageNo, pageSize, Sort.by("id").ascending());
        return repository.findAll(page);
    }

    @Transactional
    public Holiday create(HolidayRequest request) {
        return repository.save(new Holiday(request.id(), request.holidayDate(), request.calendarCode(),
                request.calendarName(), request.username()));
    }

    @Transactional
    public Holiday update(Long id, HolidayRequest request) {
        Holiday holiday = repository.findById(id).orElseThrow(() -> new HolidayNotFoundException(id));
        holiday.update(request.holidayDate(), request.calendarCode(), request.calendarName(), request.username());
        return holiday;
    }

    @Transactional
    public void delete(Long id) {
        Holiday holiday = repository.findById(id).orElseThrow(() -> new HolidayNotFoundException(id));
        repository.delete(holiday);
    }
}
