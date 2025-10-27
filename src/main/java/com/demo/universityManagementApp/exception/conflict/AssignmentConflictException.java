package com.demo.universityManagementApp.exception.conflict;

import com.demo.universityManagementApp.exception.BaseUniversityAppException;

public class AssignmentConflictException extends ConflictException {
    public AssignmentConflictException(String message) {
        super(message);
    }

    public AssignmentConflictException(String entityType, String entityId, String conflictReason) {
        super(String.format("Assignment conflict for %s (ID: %s): %s", entityType, entityId, conflictReason));
    }
}
