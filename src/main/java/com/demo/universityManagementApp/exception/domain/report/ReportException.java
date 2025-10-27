package com.demo.universityManagementApp.exception.domain.report;

import com.demo.universityManagementApp.exception.domain.DomainException;

public class ReportException extends DomainException {
    public ReportException(String message) {
        super(message);
    }

    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
