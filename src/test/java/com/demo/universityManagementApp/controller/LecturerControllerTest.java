package com.demo.universityManagementApp.controller;

import com.demo.universityManagementApp.config.SecurityConfig;
import com.demo.universityManagementApp.exception.GlobalExceptionHandler;
import com.demo.universityManagementApp.ratelimit.BucketService;
import com.demo.universityManagementApp.ratelimit.RateLimitInterceptor;
import com.demo.universityManagementApp.rest.ClassSessionController;
import com.demo.universityManagementApp.rest.LecturerController;
import com.demo.universityManagementApp.rest.model.request.LecturerRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateLecturerRequest;
import com.demo.universityManagementApp.rest.model.response.APIResponse;
import com.demo.universityManagementApp.rest.model.response.LecturerResponse;
import com.demo.universityManagementApp.service.LecturerService;
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
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LecturerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
@ExtendWith(SpringExtension.class)
class LecturerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LecturerService lecturerService;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    private LecturerResponse lecturerResponse;
    private LecturerRequest lecturerRequest;
    private UpdateLecturerRequest updateLecturerRequest;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        lecturerResponse = LecturerResponse.builder()
                .externalId("LECT1")
                .name("Sanad Jarrad")
                .email("sanad@example.com")
                .phone("0791234567")
                .departmentId("DEPT1")
                .departmentName("Computer Science")
                .courseCount(3)
                .classCount(5)
                .courseIds(Set.of("COURSE1", "COURSE2", "COURSE3"))
                .classIds(Set.of("CLASS1", "CLASS2", "CLASS3", "CLASS4", "CLASS5"))
                .build();

        lecturerRequest = new LecturerRequest();
        lecturerRequest.setName("Sanad Jarrad");
        lecturerRequest.setPhone("0791234567");
        lecturerRequest.setDepartmentId("DEPT1");

        updateLecturerRequest = new UpdateLecturerRequest();
        updateLecturerRequest.setName("Sanad Anwar Jarrad");
        updateLecturerRequest.setPhone("0781234567");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createLecturer_AsAdmin_ShouldReturnCreated() throws Exception {
        when(lecturerService.createLecturer(any(LecturerRequest.class))).thenReturn(lecturerResponse);

        mockMvc.perform(post("/lecturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lecturerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lecturer created successfully"))
                .andExpect(jsonPath("$.data.ID").value("LECT1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createLecturer_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/lecturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lecturerRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllLecturers_ShouldReturnPage() throws Exception {
        Page<LecturerResponse> page = new PageImpl<>(Collections.singletonList(lecturerResponse));
        when(lecturerService.getAllLecturers(anyInt(), anyInt(), anyString())).thenReturn(page);

        mockMvc.perform(get("/lecturers/all")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].ID").value("LECT1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getLecturerById_ShouldReturnLecturer() throws Exception {
        when(lecturerService.getLecturerByExternalId("LECT1")).thenReturn(lecturerResponse);

        mockMvc.perform(get("/lecturers/LECT1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ID").value("LECT1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getLecturersByDepartment_ShouldReturnPage() throws Exception {
        Page<LecturerResponse> page = new PageImpl<>(Collections.singletonList(lecturerResponse));
        when(lecturerService.getLecturersByDepartment("DEPT1", 0, 20)).thenReturn(page);

        mockMvc.perform(get("/lecturers/department/DEPT1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].departmentId").value("DEPT1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getLecturerByName_ShouldReturnLecturer() throws Exception {
        when(lecturerService.getLecturerByName("Sanad Jarrad")).thenReturn(lecturerResponse);

        mockMvc.perform(get("/lecturers/search/Sanad Jarrad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sanad Jarrad"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateLecturer_ShouldReturnUpdatedLecturer() throws Exception {
        when(lecturerService.updateLecturer(anyString(), any(UpdateLecturerRequest.class))).thenReturn(lecturerResponse);

        mockMvc.perform(put("/lecturers/LECT1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLecturerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lecturer updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Sanad Jarrad"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteLecturer_AsAdmin_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/lecturers/LECT1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lecturer deleted successfully"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteLecturer_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/lecturers/LECT1")).andExpect(status().isForbidden());
    }
}

