package com.demo.universityManagementApp.rest.async;

import com.demo.universityManagementApp.rest.model.ReportFormat;
import com.demo.universityManagementApp.rest.model.request.BulkReportRequest;
import com.demo.universityManagementApp.rest.model.response.APIResponse;
import com.demo.universityManagementApp.rest.model.response.BulkReportResponse;
import com.demo.universityManagementApp.rest.model.response.ReportResponse;
import com.demo.universityManagementApp.service.report.AsyncReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("reports/async")
@RequiredArgsConstructor
@Tag(name = "Async Report Generation", description = "Asynchronous operations for generating student reports")
public class AsyncReportController {

    private final AsyncReportService asyncReportService;

    @Operation(
            summary = "Generate student report asynchronously",
            description = "Generates a student report asynchronously in either CSV or TXT format (based on params) in a default directory (project directory) ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Report generated successfully"),
                    @ApiResponse(responseCode = "404", description = "Student not found or does not exist")
            }
    )
    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public CompletableFuture<ResponseEntity<APIResponse<ReportResponse>>> generateStudentReportAsync(@PathVariable final String studentId,
                                                                                                     @RequestParam(defaultValue = "TXT") final ReportFormat format) {

        return asyncReportService.generateStudentReportAsync(studentId, format).thenApply(report ->
                        ResponseEntity.ok(APIResponse.success("Report generated successfully", report)));
    }

    @Operation(
            summary = "Generate multiple reports asynchronously",
            description = "Generates multiple student reports asynchronously (batch-processing)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bulk report generation completed"),
            }
    )
    @PostMapping("/students/bulk")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public CompletableFuture<ResponseEntity<APIResponse<BulkReportResponse>>> generateBulkReportsAsync(@Valid @RequestBody final BulkReportRequest request) {

        return asyncReportService.generateBulkReportsAsync(request).thenApply(response ->
                ResponseEntity.ok(APIResponse.success("Bulk report generation completed", response)));
    }

    @Operation(
            summary = "Lists available reports asynchronously",
            description = "Generates a list of available report names from the default directory asynchronously",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Available reports retrieved successfully"),
            }
    )
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public CompletableFuture<ResponseEntity<APIResponse<List<String>>>> listAvailableReportsAsync() {
        return asyncReportService.listAvailableReportsAsync().thenApply(reports ->
                        ResponseEntity.ok(APIResponse.success("Available reports retrieved successfully", reports)));
    }
}
