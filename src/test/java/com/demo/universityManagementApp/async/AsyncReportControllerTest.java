package com.demo.universityManagementApp.async;

import com.demo.universityManagementApp.config.SecurityConfig;
import com.demo.universityManagementApp.ratelimit.RateLimitInterceptor;
import com.demo.universityManagementApp.rest.async.AsyncReportController;
import com.demo.universityManagementApp.rest.model.ReportFormat;
import com.demo.universityManagementApp.rest.model.request.BulkReportRequest;
import com.demo.universityManagementApp.rest.model.response.BulkReportResponse;
import com.demo.universityManagementApp.rest.model.response.ReportResponse;
import com.demo.universityManagementApp.service.report.AsyncReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AsyncReportController.class)
@Import(SecurityConfig.class)
@ExtendWith(SpringExtension.class)
class AsyncReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AsyncReportService asyncReportService;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    private ReportResponse reportResponse;
    private BulkReportResponse bulkReportResponse;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        reportResponse = ReportResponse.builder()
                .content("STUDENT REPORT\nStudent Details:\nID: STUD1\nName: Sanad Anwar Jarrad")
                .format("TXT")
                .entityType("STUDENT")
                .entityId("STUD1")
                .entityName("Sanad Anwar Jarrad")
                .fileName("student_report_STUD1_20241215_143022.txt")
                .fileSize(1024L)
                .build();

        bulkReportResponse = BulkReportResponse.builder()
                .totalRequests(3)
                .successfulGenerations(3)
                .failedGenerations(0)
                .generatedAt(OffsetDateTime.now())
                .fileNames(List.of(
                        "student_report_STUD1_20241215_143022.txt",
                        "student_report_STUD2_20241215_143023.txt",
                        "student_report_STUD3_20241215_143024.txt"
                ))
                .build();
    }

    private ResultActions asyncPerform(ResultActions mvcResult) throws Exception {
        return mockMvc.perform(asyncDispatch(mvcResult.andReturn()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void generateStudentReportAsync_ShouldReturnReport() throws Exception {
        when(asyncReportService.generateStudentReportAsync(anyString(), any(ReportFormat.class))).thenReturn(CompletableFuture.completedFuture(reportResponse));

        ResultActions mvcResult = mockMvc.perform(get("/reports/async/students/STUD1").param("format", "TXT"))
                .andExpect(request().asyncStarted());

        asyncPerform(mvcResult)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report generated successfully"))
                .andExpect(jsonPath("$.data.entityId").value("STUD1"))
                .andExpect(jsonPath("$.data.format").value("TXT"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void generateStudentReportAsync_WithDefaultFormat_ShouldUseTXT() throws Exception {
        when(asyncReportService.generateStudentReportAsync(anyString(), any(ReportFormat.class))).thenReturn(CompletableFuture.completedFuture(reportResponse));

        ResultActions mvcResult = mockMvc.perform(get("/reports/async/students/STUD1")).andExpect(request().asyncStarted());

        asyncPerform(mvcResult).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void generateBulkReportsAsync_ShouldReturnBulkResponse() throws Exception {
        when(asyncReportService.generateBulkReportsAsync(any(BulkReportRequest.class))).thenReturn(CompletableFuture.completedFuture(bulkReportResponse));

        ResultActions mvcResult = mockMvc.perform(post("/reports/async/students/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "studentIds": ["STUD1", "STUD2", "STUD3"],
                                "format": "TXT"
                            }
                            """))
                .andExpect(request().asyncStarted());

        asyncPerform(mvcResult)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bulk report generation completed"))
                .andExpect(jsonPath("$.data.totalRequests").value(3))
                .andExpect(jsonPath("$.data.successfulGenerations").value(3))
                .andExpect(jsonPath("$.data.failedGenerations").value(0))
                .andExpect(jsonPath("$.data.fileNames").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listAvailableReportsAsync_ShouldReturnReportList() throws Exception {
        List<String> reports = List.of("report1.txt", "report2.csv", "report3.txt");
        when(asyncReportService.listAvailableReportsAsync()).thenReturn(CompletableFuture.completedFuture(reports));

        ResultActions mvcResult = mockMvc.perform(get("/reports/async/list")).andExpect(request().asyncStarted());

        asyncPerform(mvcResult)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Available reports retrieved successfully"))
                .andExpect(jsonPath("$.data[0]").value("report1.txt"))
                .andExpect(jsonPath("$.data[1]").value("report2.csv"))
                .andExpect(jsonPath("$.data[2]").value("report3.txt"));
    }

    @Test
    void generateStudentReportAsync_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/reports/async/students/STUD1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void generateBulkReportsAsync_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/reports/async/students/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "studentIds": ["STUD1", "STUD2"],
                                "format": "TXT"
                            }
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void generateStudentReportAsync_WithCSVFormat_ShouldUseCSV() throws Exception {
        ReportResponse csvResponse = ReportResponse.builder()
                .content("Student ID,Name,Email...")
                .format("CSV")
                .entityType("STUDENT")
                .entityId("STUD1")
                .entityName("Sanad Anwar Jarrad")
                .fileName("student_report_STUD1_20241215_143022.csv")
                .fileSize(2048L)
                .build();

        when(asyncReportService.generateStudentReportAsync(anyString(), any(ReportFormat.class))).thenReturn(CompletableFuture.completedFuture(csvResponse));

        ResultActions mvcResult = mockMvc.perform(get("/reports/async/students/STUD1").param("format", "CSV"))
                .andExpect(request().asyncStarted());

        asyncPerform(mvcResult)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.format").value("CSV"))
                .andExpect(jsonPath("$.data.fileName").value("student_report_STUD1_20241215_143022.csv"));
    }
}
