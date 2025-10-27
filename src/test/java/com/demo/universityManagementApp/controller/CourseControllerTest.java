package com.demo.universityManagementApp.controller;

import com.demo.universityManagementApp.config.SecurityConfig;
import com.demo.universityManagementApp.ratelimit.RateLimitInterceptor;
import com.demo.universityManagementApp.rest.CourseController;
import com.demo.universityManagementApp.rest.DepartmentController;
import com.demo.universityManagementApp.rest.model.request.CourseRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateCourseRequest;
import com.demo.universityManagementApp.rest.model.response.CourseResponse;
import com.demo.universityManagementApp.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
@ExtendWith(SpringExtension.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseService courseService;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    private CourseResponse courseResponse;
    private CourseRequest courseRequest;
    private UpdateCourseRequest updateCourseRequest;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        courseResponse = CourseResponse.builder()
                .externalId("COURSE1")
                .name("Mathematics")
                .code("MATH101")
                .departmentId("DEPT1")
                .departmentName("Computer Science")
                .lecturerCount(2)
                .classCount(3)
                .build();

        courseRequest = new CourseRequest();
        courseRequest.setName("Mathematics");
        courseRequest.setCode("MATH101");
        courseRequest.setDepartmentId("DEPT1");

        updateCourseRequest = new UpdateCourseRequest();
        updateCourseRequest.setName("Advanced Mathematics");
        updateCourseRequest.setCode("MATH201");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCourse_AsAdmin_ShouldReturnCreated() throws Exception {
        when(courseService.createCourse(any(CourseRequest.class))).thenReturn(courseResponse);

        mockMvc.perform(post("/courses")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Course created successfully"))
                .andExpect(jsonPath("$.data.ID").value("COURSE1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCourse_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/courses")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllCourses_ShouldReturnPage() throws Exception {
        Page<CourseResponse> page = new PageImpl<>(Collections.singletonList(courseResponse));
        when(courseService.getAllCourses(anyInt(), anyInt(), anyString())).thenReturn(page);

        mockMvc.perform(get("/courses/all")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].ID").value("COURSE1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCourseById_ShouldReturnCourse() throws Exception {
        when(courseService.getCourseByExternalId("COURSE1")).thenReturn(courseResponse);

        mockMvc.perform(get("/courses/COURSE1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ID").value("COURSE1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCourseByName_ShouldReturnCourse() throws Exception {
        when(courseService.getCourseByName("Mathematics")).thenReturn(courseResponse);

        mockMvc.perform(get("/courses/search/Mathematics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Mathematics"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCoursesByDepartment_ShouldReturnPage() throws Exception {
        Page<CourseResponse> page = new PageImpl<>(Collections.singletonList(courseResponse));
        when(courseService.getCoursesByDepartment(anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/courses/department/DEPT1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].departmentId").value("DEPT1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCourse_AsAdmin_ShouldReturnUpdatedCourse() throws Exception {
        when(courseService.updateCourse(anyString(), any(UpdateCourseRequest.class))).thenReturn(courseResponse);

        mockMvc.perform(put("/courses/COURSE1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateCourseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Course updated successfully"))
                .andExpect(jsonPath("$.data.ID").value("COURSE1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateCourse_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/courses/COURSE1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateCourseRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCourse_AsAdmin_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/courses/COURSE1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Course deleted successfully"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteCourse_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/courses/COURSE1")).andExpect(status().isForbidden());
    }
}
