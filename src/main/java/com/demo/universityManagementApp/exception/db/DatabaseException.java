package com.demo.universityManagementApp.exception.db;

import com.demo.universityManagementApp.exception.BaseUniversityAppException;

public abstract class DatabaseException extends BaseUniversityAppException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
