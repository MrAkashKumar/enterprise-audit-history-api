package com.akash.auditapi.holiday;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "HOLIDAY_CALENDAR")
public class Holiday {
    @Id
    private Long id;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_ON")
    private LocalDateTime createdOn;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @Column(name = "UPDATED_ON")
    private LocalDateTime updatedOn;

    @Version
    @Column(name = "VERSION")
    private Long version;

    @Column(name = "CALENDAR_CODE", nullable = false)
    private String calendarCode;

    @Column(name = "CALENDAR_NAME", nullable = false)
    private String calendarName;

    @Column(name = "HOLIDAY_DATE", nullable = false)
    private LocalDate holidayDate;

    protected Holiday() {}

    public Holiday(Long id, LocalDate holidayDate, String calendarCode,
                   String calendarName, String username) {
        this.id = id;
        this.holidayDate = holidayDate;
        this.calendarCode = calendarCode;
        this.calendarName = calendarName;
        this.createdBy = username;
        this.updatedBy = username;
    }

    @PrePersist
    void created() {
        LocalDateTime now = LocalDateTime.now();
        createdOn = now;
        updatedOn = now;
    }

    @PreUpdate
    void updated() { updatedOn = LocalDateTime.now(); }

    public void update(LocalDate holidayDate, String calendarCode,
                       String calendarName, String username) {
        this.holidayDate = holidayDate;
        this.calendarCode = calendarCode;
        this.calendarName = calendarName;
        this.updatedBy = username;
    }

    public Long getId() { return id; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedOn() { return createdOn; }
    public String getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedOn() { return updatedOn; }
    public Long getVersion() { return version; }
    public String getCalendarCode() { return calendarCode; }
    public String getCalendarName() { return calendarName; }
    public LocalDate getHolidayDate() { return holidayDate; }
}
