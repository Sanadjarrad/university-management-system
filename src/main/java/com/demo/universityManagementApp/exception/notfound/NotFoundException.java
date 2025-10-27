package com.demo.universityManagementApp.exception.notfound;

import com.demo.universityManagementApp.exception.BaseUniversityAppException;

public class NotFoundException extends BaseUniversityAppException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
