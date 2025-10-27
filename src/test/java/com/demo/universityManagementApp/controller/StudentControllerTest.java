package com.demo.universityManagementApp.controller;

import com.demo.universityManagementApp.config.SecurityConfig;
import com.demo.universityManagementApp.ratelimit.RateLimitInterceptor;
import com.demo.universityManagementApp.rest.StudentController;
import com.demo.universityManagementApp.rest.model.request.EnrollmentRequest;
import com.demo.universityManagementApp.rest.model.request.StudentRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateStudentRequest;
import com.demo.universityManagementApp.rest.model.response.EnrollmentResponse;
import com.demo.universityManagementApp.rest.model.response.StudentResponse;
import com.demo.universityManagementApp.service.StudentService;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
@ExtendWith(SpringExtension.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudentService studentService;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    private StudentResponse studentResponse;
    private StudentRequest studentRequest;
    private UpdateStudentRequest updateStudentRequest;
    private EnrollmentRequest enrollmentRequest;
    private EnrollmentResponse enrollmentResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        studentResponse = StudentResponse.builder()
                .externalId("STUD1")
                .name("Sanad Anwar Jarrad")
                .email("sajarrad24@university.com")
                .phone("0791234567")
                .departmentId("DEPT1")
                .departmentName("Computer Science")
                .enrollmentYear(2024)
                .enrolledClassCount(3)
                .classIds(Set.of("CLASS1", "CLASS2", "CLASS3"))
                .build();

        studentRequest = new StudentRequest();
        studentRequest.setName("Sanad Anwar Jarrad");
        studentRequest.setPhone("0791234567");
        studentRequest.setDepartmentId("DEPT1");
        studentRequest.setEnrollmentYear(2024);

        updateStudentRequest = new UpdateStudentRequest();
        updateStudentRequest.setName("Sanad Jarrad");
        updateStudentRequest.setPhone("0781234567");

        enrollmentRequest = new EnrollmentRequest();
        enrollmentRequest.setStudentId("STUD1");
        enrollmentRequest.setClassSessionId("CLASS1");

        enrollmentResponse = EnrollmentResponse.builder()
                .success(true)
                .message("Enrollment successful")
                .studentId("STUD1")
                .classSessionId("CLASS1")
                .studentName("Sanad Anwar Jarrad")
                .courseName("Mathematics")
                .availableSeats(25)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createStudent_AsAdmin_ShouldReturnCreated() throws Exception {
        when(studentService.createStudent(any(StudentRequest.class))).thenReturn(studentResponse);

        mockMvc.perform(post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Student created successfully"))
                .andExpect(jsonPath("$.data.ID").value("STUD1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createStudent_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllStudents_ShouldReturnPage() throws Exception {
        Page<StudentResponse> page = new PageImpl<>(Collections.singletonList(studentResponse));
        when(studentService.getAllStudents(anyInt(), anyInt(), anyString())).thenReturn(page);

        mockMvc.perform(get("/students/all")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].ID").value("STUD1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getStudentById_ShouldReturnStudent() throws Exception {
        when(studentService.getStudentByExternalId("STUD1")).thenReturn(studentResponse);

        mockMvc.perform(get("/students/STUD1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ID").value("STUD1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getStudentByName_ShouldReturnStudent() throws Exception {
        when(studentService.getStudentByName("Sanad Anwar Jarrad")).thenReturn(studentResponse);

        mockMvc.perform(get("/students/search/Sanad Anwar Jarrad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sanad Anwar Jarrad"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getStudentByNameContaining_ShouldReturnStudent() throws Exception {
        when(studentService.getStudentByNameContaining("Sanad")).thenReturn(studentResponse);

        mockMvc.perform(get("/students/search")
                        .param("name", "Sanad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Sanad Anwar Jarrad"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getStudentsByDepartment_ShouldReturnPage() throws Exception {
        Page<StudentResponse> page = new PageImpl<>(Collections.singletonList(studentResponse));
        when(studentService.getStudentsByDepartment(anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/students/department/DEPT1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].departmentId").value("DEPT1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getStudentsByClassSession_ShouldReturnPage() throws Exception {
        Page<StudentResponse> page = new PageImpl<>(Collections.singletonList(studentResponse));
        when(studentService.getStudentsByClassSession(anyString(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/students/classSession/CLASS1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].ID").value("STUD1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateStudent_ShouldReturnUpdatedStudent() throws Exception {
        when(studentService.updateStudent(anyString(), any(UpdateStudentRequest.class))).thenReturn(studentResponse);

        mockMvc.perform(put("/students/STUD1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStudentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Student updated successfully"))
                .andExpect(jsonPath("$.data.ID").value("STUD1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteStudent_AsAdmin_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/students/STUD1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Student deleted successfully"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteStudent_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/students/STUD1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void enrollInClass_ShouldReturnEnrollmentResponse() throws Exception {
        when(studentService.enrollInClass(anyString(), anyString())).thenReturn(enrollmentResponse);

        mockMvc.perform(post("/students/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Enrollment successful"))
                .andExpect(jsonPath("$.data.studentId").value("STUD1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getStudentCount_ShouldReturnCount() throws Exception {
        when(studentService.getStudentCount()).thenReturn(150L);

        mockMvc.perform(get("/students/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(150));
    }
}



