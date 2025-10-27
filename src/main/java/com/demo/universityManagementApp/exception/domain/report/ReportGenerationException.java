package com.demo.universityManagementApp.exception.domain.report;

public class ReportGenerationException extends ReportException{

    public ReportGenerationException(String message) {
        super(message);
    }

    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
