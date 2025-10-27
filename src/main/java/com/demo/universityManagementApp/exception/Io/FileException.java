package com.demo.universityManagementApp.exception.Io;

import com.demo.universityManagementApp.exception.BaseUniversityAppException;

public abstract class FileException extends BaseUniversityAppException {

    public FileException(String message) {
        super(message);
    }

    public FileException(String message, Throwable cause) {
        super(message, cause);
    }
}
