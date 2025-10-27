package com.demo.universityManagementApp.rest.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private boolean success;
    private String message;
    private String lecturerId;
    private String courseId;
    private String lecturerName;
    private String courseName;
}
