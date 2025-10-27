package com.demo.universityManagementApp.exception.domain.classSession;

import com.demo.universityManagementApp.exception.BaseUniversityAppException;

public abstract class ClassSessionException extends BaseUniversityAppException {
    public ClassSessionException(String message) {
        super(message);
    }

    public ClassSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
