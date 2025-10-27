package com.demo.universityManagementApp.rest.model.request.update;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateStudentRequest {

    @Size(min = 2, max = 50, message = "Updated Name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s-]+$", message = "Name must contain only letters, spaces, and hyphens")
    private String name;

    @Pattern(regexp = "^(077|078|079)\\d{7}$", message = "Updated Phone number must be in format 077/078/079 followed by 7 digits")
    private String phone;
}
