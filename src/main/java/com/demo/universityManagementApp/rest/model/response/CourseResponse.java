package com.demo.universityManagementApp.rest.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"externalId", "name", "code", "departmentId", "departmentName", "lecturerCount", "classCount", "lecturerIds", "classIds"})
public class CourseResponse {
    //private Long id;
    @JsonProperty("ID")
    private String externalId;
    private String name;
    private String code;
    private String departmentId;
    private String departmentName;
    private Integer lecturerCount;
    @JsonProperty("Enrollment Count")
    private Integer classCount;
    private Set<String> lecturerIds;
    private Set<String> classIds;
}
