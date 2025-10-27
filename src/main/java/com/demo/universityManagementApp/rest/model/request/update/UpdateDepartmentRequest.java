package com.demo.universityManagementApp.rest.model.request.update;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDepartmentRequest {

    @Size(min = 2, max = 100, message = "Updated Name must be between 2 and 100 characters")
    private String name;

    @Size(min = 2, max = 10, message = "Updated Code must be between 2 and 10 characters")
    private String code;
}
