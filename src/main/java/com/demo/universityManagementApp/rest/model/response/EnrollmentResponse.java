package com.demo.universityManagementApp.rest.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private boolean success;
    private String message;
    private String studentId;
    private String classSessionId;
    private String studentName;
    private String courseName;
    private Integer availableSeats;
}
