package com.demo.universityManagementApp.rest.model.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StudentRequest {

    @NotBlank(message = "Student Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s-]+$", message = "Name must contain only letters, spaces, and hyphens")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(077|078|079)\\d{7}$", message = "Phone number must be in format 077/078/079 followed by 7 digits")
    private String phone;

    @NotBlank(message = "Department ID is required")
    private String departmentId;

    @NotNull(message = "Enrollment year is required")
    @Min(value = 2000, message = "Enrollment year must be from 2000 onwards")
    @Max(value = 2025, message = "Enrollment year cannot be past the current year")
    private Integer enrollmentYear;
}
