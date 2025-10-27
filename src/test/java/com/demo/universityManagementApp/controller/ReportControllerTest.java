package com.demo.universityManagementApp.controller;

import com.demo.universityManagementApp.config.SecurityConfig;
import com.demo.universityManagementApp.ratelimit.RateLimitInterceptor;
import com.demo.universityManagementApp.rest.ReportController;
import com.demo.universityManagementApp.rest.model.ReportFormat;
import com.demo.universityManagementApp.rest.model.response.ReportResponse;
import com.demo.universityManagementApp.service.report.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
//@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
@ExtendWith(SpringExtension.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    private ReportResponse reportResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        reportResponse = ReportResponse.builder()
                .content("STUDENT REPORT\nStudent Details:\nID: STUD1\nName: Sanad Anwar Jarrad")
                .format("TXT")
                .entityType("STUDENT")
                .entityId("STUD1")
                .entityName("Sanad Anwar Jarrad")
                .fileName("student_report_STUD1.txt")
                .fileSize(1024L)
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void generateStudentReport_ShouldReturnReport() throws Exception {
        when(reportService.generateStudentReport(anyString(), any(ReportFormat.class))).thenReturn(reportResponse);

        mockMvc.perform(get("/reports/students/STUD1")
                        .param("format", "TXT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Student report generated successfully"))
                .andExpect(jsonPath("$.data.entityId").value("STUD1"))
                .andExpect(jsonPath("$.data.format").value("TXT"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listAvailableReports_ShouldReturnReportList() throws Exception {
        List<String> reports = Arrays.asList("report1.txt", "report2.csv");
        when(reportService.listAvailableReports()).thenReturn(reports);

        mockMvc.perform(get("/reports/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Available reports retrieved successfully"))
                .andExpect(jsonPath("$.data[0]").value("report1.txt"))
                .andExpect(jsonPath("$.data[1]").value("report2.csv"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listAvailableReports_WhenIOException_ShouldReturnError() throws Exception {
        when(reportService.listAvailableReports()).thenThrow(new IOException("File system error"));

        mockMvc.perform(get("/reports/list")).andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getSupportedFormats_ShouldReturnFormats() throws Exception {
        mockMvc.perform(get("/reports/formats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Supported formats retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void generateStudentReport_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/reports/students/STUD1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void generateStudentReport_WithDefaultFormat_ShouldUseTXT() throws Exception {
        when(reportService.generateStudentReport(anyString(), any(ReportFormat.class))).thenReturn(reportResponse);

        mockMvc.perform(get("/reports/students/STUD1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
