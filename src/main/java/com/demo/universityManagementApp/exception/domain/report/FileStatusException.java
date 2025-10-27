package com.demo.universityManagementApp.exception.domain.report;

public class FileStatusException extends ReportException {
    public FileStatusException(String message) {
        super(message);
    }

    public FileStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
