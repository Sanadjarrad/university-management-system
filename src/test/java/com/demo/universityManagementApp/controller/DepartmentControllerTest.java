package com.demo.universityManagementApp.controller;

import com.demo.universityManagementApp.config.SecurityConfig;
import com.demo.universityManagementApp.ratelimit.RateLimitInterceptor;
import com.demo.universityManagementApp.rest.DepartmentController;
import com.demo.universityManagementApp.rest.model.request.DepartmentRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateDepartmentRequest;
import com.demo.universityManagementApp.rest.model.response.DepartmentResponse;
import com.demo.universityManagementApp.service.DepartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;


import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
@ExtendWith(SpringExtension.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentService departmentService;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    private DepartmentResponse departmentResponse;
    private DepartmentRequest departmentRequest;
    private UpdateDepartmentRequest updateDepartmentRequest;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        departmentResponse = DepartmentResponse.builder()
                .externalId("DEPT1")
                .name("Computer Science")
                .code("CS")
                .studentCount(100)
                .lecturerCount(15)
                .courseCount(20)
                .build();

        departmentRequest = new DepartmentRequest();
        departmentRequest.setName("Computer Science");
        departmentRequest.setCode("CS");

        updateDepartmentRequest = new UpdateDepartmentRequest();
        updateDepartmentRequest.setName("Computer Science and Engineering");
        updateDepartmentRequest.setCode("CSE");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDepartment_AsAdmin_ShouldReturnCreated() throws Exception {
        when(departmentService.createDepartment(any(DepartmentRequest.class))).thenReturn(departmentResponse);

        mockMvc.perform(post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(departmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Department created successfully"))
                .andExpect(jsonPath("$.data.ID").value("DEPT1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createDepartment_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(departmentRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllDepartments_ShouldReturnPage() throws Exception {
        Page<DepartmentResponse> page = new PageImpl<>(Collections.singletonList(departmentResponse));
        when(departmentService.getAllDepartments(anyInt(), anyInt(), anyString())).thenReturn(page);

        mockMvc.perform(get("/departments/all")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].ID").value("DEPT1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getDepartmentById_ShouldReturnDepartment() throws Exception {
        when(departmentService.getDepartmentByExternalId("DEPT1")).thenReturn(departmentResponse);

        mockMvc.perform(get("/departments/DEPT1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ID").value("DEPT1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getDepartmentByName_ShouldReturnDepartment() throws Exception {
        when(departmentService.getDepartmentByName("Computer Science")).thenReturn(departmentResponse);

        mockMvc.perform(get("/departments/search/Computer Science"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Computer Science"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateDepartment_AsAdmin_ShouldReturnUpdatedDepartment() throws Exception {
        when(departmentService.updateDepartment(anyString(), any(UpdateDepartmentRequest.class))).thenReturn(departmentResponse);

        mockMvc.perform(put("/departments/DEPT1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDepartmentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Department updated successfully"))
                .andExpect(jsonPath("$.data.ID").value("DEPT1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateDepartment_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/departments/DEPT1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDepartmentRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteDepartment_AsAdmin_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/departments/DEPT1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Department deleted successfully"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteDepartment_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/departments/DEPT1")).andExpect(status().isForbidden());
    }
}

