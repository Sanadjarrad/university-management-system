package com.demo.universityManagementApp.rest.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentRequest {

    @NotBlank(message = "Department Name is required")
    @Size(min = 2, max = 100, message = "Department Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Department Code is required")
    @Size(min = 2, max = 10, message = "Department Code must be between 2 and 10 characters")
    private String code;
}
