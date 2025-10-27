package com.demo.universityManagementApp.rest.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LecturerRequest {

    @NotBlank(message = "Lecturer Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s-]+$", message = "Name must contain only letters, spaces, and hyphens")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(077|078|079)\\d{7}$", message = "Phone number must be in format 077/078/079 followed by 7 digits")
    private String phone;

    @NotBlank(message = "Department ID is required")
    private String departmentId;
}
