package com.demo.universityManagementApp.exception;

public abstract class BaseUniversityAppException extends RuntimeException {

    public BaseUniversityAppException(String message) {
        super(message);
    }

    public BaseUniversityAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
