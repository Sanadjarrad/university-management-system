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
@JsonPropertyOrder({"externalId", "name", "email", "phone", "departmentId", "departmentName", "enrollmentYear", "enrolledClassCount", "classIds"})
public class StudentResponse {
    //private String id;
    @JsonProperty("ID")
    private String externalId;
    private String name;
    private String email;
    private String phone;
    private String departmentId;
    private String departmentName;
    private Integer enrollmentYear;
    private Integer enrolledClassCount;
    private Set<String> classIds;
}
