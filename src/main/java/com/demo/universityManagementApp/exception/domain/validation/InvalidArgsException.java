package com.demo.universityManagementApp.exception.domain.validation;

import com.demo.universityManagementApp.exception.BaseUniversityAppException;

public abstract class InvalidArgsException extends BaseUniversityAppException {
    public InvalidArgsException(String message) {
        super(message);
    }

    public InvalidArgsException(String message, Throwable cause) {
        super(message, cause);
    }
}
