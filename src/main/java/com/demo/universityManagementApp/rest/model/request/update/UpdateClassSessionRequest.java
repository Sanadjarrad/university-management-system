package com.demo.universityManagementApp.rest.model.request.update;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class UpdateClassSessionRequest {

    private LocalTime startTime;
    private LocalTime endTime;
    private DayOfWeek day;
    private String location;

    @Min(value = 1, message = "Updated Max capacity must be at least 1")
    @Max(value = 500, message = "Updated Max capacity cannot exceed 500")
    private Integer maxCapacity;
}
