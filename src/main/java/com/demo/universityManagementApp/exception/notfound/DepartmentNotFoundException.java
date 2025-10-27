package com.demo.universityManagementApp.exception.notfound;

public class DepartmentNotFoundException extends NotFoundException {

    public DepartmentNotFoundException(String id) {
        super(String.format("Department with ID: (%s) not found", id));
    }

    public DepartmentNotFoundException(String message, Throwable reason) {
        super(message, reason);
    }
}
