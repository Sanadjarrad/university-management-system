package com.demo.universityManagementApp.controller;

import com.demo.universityManagementApp.config.SecurityConfig;
import com.demo.universityManagementApp.ratelimit.BucketService;
import com.demo.universityManagementApp.ratelimit.RateLimitInterceptor;
import com.demo.universityManagementApp.rest.ClassSessionController;
import com.demo.universityManagementApp.rest.model.request.ClassSessionRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateClassSessionRequest;
import com.demo.universityManagementApp.rest.model.response.APIResponse;
import com.demo.universityManagementApp.rest.model.response.ClassSessionResponse;
import com.demo.universityManagementApp.service.ClassSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClassSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
@ExtendWith(SpringExtension.class)
class ClassSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClassSessionService classSessionService;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    private ClassSessionResponse classSessionResponse;
    private ClassSessionRequest classSessionRequest;
    private UpdateClassSessionRequest updateClassSessionRequest;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        classSessionResponse = ClassSessionResponse.builder()
                .externalId("CLASS1")
                .courseId("COURSE1")
                .courseName("Mathematics")
                .lecturerId("LECT1")
                .lecturerName("Sanad Jarrad")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .day(DayOfWeek.MONDAY)
                .location("Room 101")
                .maxCapacity(30)
                .enrolledCount(10)
                .availableSeats(20)
                .isFull(false)
                .build();

        classSessionRequest = new ClassSessionRequest();
        classSessionRequest.setCourseId("COURSE1");
        classSessionRequest.setLecturerId("LECT1");
        classSessionRequest.setStartTime(LocalTime.of(9, 0));
        classSessionRequest.setEndTime(LocalTime.of(10, 30));
        classSessionRequest.setDay(DayOfWeek.MONDAY);
        classSessionRequest.setLocation("Room 101");
        classSessionRequest.setMaxCapacity(30);

        updateClassSessionRequest = new UpdateClassSessionRequest();
        updateClassSessionRequest.setLocation("Room 201");
        updateClassSessionRequest.setMaxCapacity(35);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createClassSession_AsAdmin_ShouldReturnCreated() throws Exception {
        when(classSessionService.createClassSession(any(ClassSessionRequest.class))).thenReturn(classSessionResponse);

        mockMvc.perform(post("/class-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(classSessionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Class session created successfully"))
                .andExpect(jsonPath("$.data.ID").value("CLASS1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createClassSession_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/class-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(classSessionRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createClassSession_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        ClassSessionRequest invalidRequest = new ClassSessionRequest();

        mockMvc.perform(post("/class-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllClassSessions_ShouldReturnPage() throws Exception {
        Page<ClassSessionResponse> page = new PageImpl<>(Collections.singletonList(classSessionResponse));
        when(classSessionService.getAllClassSessions(anyInt(), anyInt(), anyString())).thenReturn(page);

        mockMvc.perform(get("/class-sessions/all")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "timeSlot.startTime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].ID").value("CLASS1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getClassSessionById_ShouldReturnClassSession() throws Exception {
        when(classSessionService.getClassSessionByExternalId("CLASS1")).thenReturn(classSessionResponse);

        mockMvc.perform(get("/class-sessions/CLASS1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ID").value("CLASS1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getClassSessionsByCourse_ShouldReturnPage() throws Exception {
        Page<ClassSessionResponse> page = new PageImpl<>(Collections.singletonList(classSessionResponse));
        when(classSessionService.getClassSessionsByCourse(anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/class-sessions/course/COURSE1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].courseId").value("COURSE1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getClassSessionsByLecturer_ShouldReturnPage() throws Exception {
        Page<ClassSessionResponse> page = new PageImpl<>(Collections.singletonList(classSessionResponse));
        when(classSessionService.getClassSessionsByLecturer(anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/class-sessions/lecturer/LECT1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].lecturerId").value("LECT1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getClassSessionsByStudent_ShouldReturnPage() throws Exception {
        Page<ClassSessionResponse> page = new PageImpl<>(Collections.singletonList(classSessionResponse));
        when(classSessionService.getClassSessionsByStudent(anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/class-sessions/student/STUD1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getClassSessionsByDay_ShouldReturnPage() throws Exception {
        Page<ClassSessionResponse> page = new PageImpl<>(Collections.singletonList(classSessionResponse));
        when(classSessionService.getClassSessionsByDay(any(DayOfWeek.class), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/class-sessions/day/MONDAY")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateClassSession_ShouldReturnUpdatedClassSession() throws Exception {
        when(classSessionService.updateClassSession(anyString(), any(UpdateClassSessionRequest.class))).thenReturn(classSessionResponse);

        mockMvc.perform(put("/class-sessions/CLASS1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateClassSessionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Class session updated successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteClassSession_AsAdmin_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/class-sessions/CLASS1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Class session deleted successfully"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteClassSession_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/class-sessions/CLASS1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAvailableSeats_ShouldReturnSeatInfo() throws Exception {
        when(classSessionService.getAvailableSeats("CLASS1")).thenReturn(20);
        when(classSessionService.hasAvailableSeats("CLASS1")).thenReturn(true);

        mockMvc.perform(get("/class-sessions/CLASS1/available-seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableSeats").value(20))
                .andExpect(jsonPath("$.data.hasAvailableSeats").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getClassSessionCount_ShouldReturnCount() throws Exception {
        when(classSessionService.getClassSessionCount()).thenReturn(50L);

        mockMvc.perform(get("/class-sessions/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(50));
    }
}
