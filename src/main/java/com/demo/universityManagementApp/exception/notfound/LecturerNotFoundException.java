package com.demo.universityManagementApp.exception.notfound;

public class LecturerNotFoundException extends NotFoundException {

    public LecturerNotFoundException(String id) {
        super(String.format("Lecturer with ID: (%s) not found", id));
    }

    public LecturerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
