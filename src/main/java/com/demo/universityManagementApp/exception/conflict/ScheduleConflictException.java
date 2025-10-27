package com.demo.universityManagementApp.exception.conflict;

import com.demo.universityManagementApp.exception.domain.DomainException;

public class ScheduleConflictException extends ConflictException {
    public ScheduleConflictException(String message) {
        super(message);
    }

    public ScheduleConflictException(String entityType, String entityId, String conflictDescription) {
        super(String.format("Scheduling conflict for %s (ID: %s): %s", entityType, entityId, conflictDescription));
    }

    public ScheduleConflictException(String entityType, String conflictDescription) {
        super(String.format("Scheduling conflict for %s: %s", entityType, conflictDescription));
    }
}
