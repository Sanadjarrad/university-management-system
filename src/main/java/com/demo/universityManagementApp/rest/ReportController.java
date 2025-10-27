package com.demo.universityManagementApp.rest;

import com.demo.universityManagementApp.rest.model.ReportFormat;
import com.demo.universityManagementApp.rest.model.response.APIResponse;
import com.demo.universityManagementApp.rest.model.response.ReportResponse;
import com.demo.universityManagementApp.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("reports")
@RequiredArgsConstructor
@Tag(name = "Report Generation", description = "Operations for generating student reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "Generates a student report",
            description = "Generates a student report in either CSV or TXT format (based on params) in a default directory (project directory)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Report generated successfully"),
                    @ApiResponse(responseCode = "404", description = "Student not found or does not exist")
            }
    )
    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<ReportResponse>> generateStudentReport(@PathVariable final String studentId,
                                                                             @RequestParam(defaultValue = "TXT") final ReportFormat format) throws IOException {

        ReportResponse report = reportService.generateStudentReport(studentId, format);
        return ResponseEntity.ok(APIResponse.success("Student report generated successfully", report));
    }

    @Operation(
            summary = "Lists available reports",
            description = "Generates a list of available report names from the default directory",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Available reports retrieved successfully"),
            }
    )
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<List<String>>> listAvailableReports() throws IOException {

        List<String> reports = reportService.listAvailableReports();
        return ResponseEntity.ok(APIResponse.success("Available reports retrieved successfully", reports));
    }

    @Operation(
            summary = "Returns supported report formats",
            description = "Generates an array of supported report formats (Currently TXT and CSV)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Supported formats retrieved successfully"),
            }
    )
    @GetMapping("/formats")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<ReportFormat[]>> getSupportedFormats() {
        return ResponseEntity.ok(APIResponse.success("Supported formats retrieved successfully", ReportFormat.values()));
    }
}
