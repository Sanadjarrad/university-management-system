package com.demo.universityManagementApp.rest.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class ClassSessionRequest {

    @NotBlank(message = "Course ID is required")
    private String courseId;

    @NotBlank(message = "Lecturer ID is required")
    private String lecturerId;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Day of week is required")
    private DayOfWeek day;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Max capacity is required")
    @Min(value = 1, message = "Max capacity must be at least 1")
    @Max(value = 500, message = "Max capacity cannot exceed 500")
    private Integer maxCapacity;
}
