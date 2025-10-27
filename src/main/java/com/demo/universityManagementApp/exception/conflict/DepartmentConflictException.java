package com.demo.universityManagementApp.exception.conflict;

public class DepartmentConflictException extends ConflictException {
    public DepartmentConflictException(String departmentId) {
        super(String.format("Operation cannot be completed: department with ID: (%s) has dependent entities or conflicts.", departmentId));
    }

    public DepartmentConflictException(String departmentId, Throwable cause) {
        super(String.format("Operation cannot be completed: department with ID: (%s) has dependent entities or conflicts.", departmentId), cause);
    }
}
