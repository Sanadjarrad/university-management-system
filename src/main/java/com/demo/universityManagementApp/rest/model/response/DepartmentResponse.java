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
@JsonPropertyOrder({"externalId", "name", "code", "studentCount", "lecturerCount", "courseCount", "studentIds", "lecturerIds", "courseIds"})
public class DepartmentResponse {
    //private Long id;
    @JsonProperty("ID")
    private String externalId;
    private String name;
    private String code;
    private Integer studentCount;
    private Integer lecturerCount;
    private Integer courseCount;
    private Set<String> studentIds;
    private Set<String> lecturerIds;
    private Set<String> courseIds;
}
