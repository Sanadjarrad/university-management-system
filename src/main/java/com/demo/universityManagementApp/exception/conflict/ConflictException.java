package com.demo.universityManagementApp.exception.conflict;

import com.demo.universityManagementApp.exception.BaseUniversityAppException;

public class ConflictException extends BaseUniversityAppException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
