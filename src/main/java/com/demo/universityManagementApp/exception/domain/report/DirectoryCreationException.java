package com.demo.universityManagementApp.exception.domain.report;

public class DirectoryCreationException extends ReportException{
    public DirectoryCreationException(String message) {
        super(message);
    }

    public DirectoryCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
