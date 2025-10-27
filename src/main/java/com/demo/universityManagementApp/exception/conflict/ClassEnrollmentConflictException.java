package com.demo.universityManagementApp.exception.conflict;

import com.demo.universityManagementApp.exception.domain.classSession.ClassSessionException;

public class ClassEnrollmentConflictException extends ConflictException {
    public ClassEnrollmentConflictException(String studentId, String conflictingClassId) {
        super(String.format("Student '%s' cannot enroll due to a scheduling conflict with class session with ID: (%s).", studentId, conflictingClassId));
    }

    public ClassEnrollmentConflictException(String studentId, String conflictingClassId, Throwable cause) {
        super(String.format("Student '%s' cannot enroll due to a scheduling conflict with class session with ID: (%s).", studentId, conflictingClassId), cause);
    }
}
