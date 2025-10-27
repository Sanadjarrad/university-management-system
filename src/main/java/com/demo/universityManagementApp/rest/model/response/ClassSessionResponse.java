package com.demo.universityManagementApp.rest.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"externalId", "courseId", "courseName", "lecturerId", "lecturerName", "startTime", "endTime", "day", "location", "maxCapacity", "enrolledCount", "availableSeats", "isFull", "studentIds"})
public class ClassSessionResponse {
    //private Long id;
    @JsonProperty("ID")
    private String externalId;
    private String courseId;
    private String courseName;
    private String lecturerId;
    private String lecturerName;
    private LocalTime startTime;
    private LocalTime endTime;
    private DayOfWeek day;
    private String location;
    private Integer maxCapacity;
    @JsonProperty("Enrollment Count")
    private Integer enrolledCount;
    private Integer availableSeats;
    private Boolean isFull;
    private Set<String> studentIds;
}
