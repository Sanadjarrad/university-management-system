package com.demo.universityManagementApp.exception.domain;

import com.demo.universityManagementApp.exception.BaseUniversityAppException;

public abstract class DomainException extends BaseUniversityAppException {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
