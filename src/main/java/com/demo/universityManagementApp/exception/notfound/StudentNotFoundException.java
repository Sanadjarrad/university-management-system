package com.demo.universityManagementApp.exception.notfound;

public class StudentNotFoundException extends NotFoundException {
    public StudentNotFoundException(String id) {
        super(String.format("Student with ID: (%s) not found", id));
    }

    public StudentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
