package com.demo.universityManagementApp.rest.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnrollmentRequest {

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Class session ID is required")
    private String classSessionId;
}
