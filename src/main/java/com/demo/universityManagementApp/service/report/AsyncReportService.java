package com.demo.universityManagementApp.service.report;

import com.demo.universityManagementApp.exception.Io.FileWriteException;
import com.demo.universityManagementApp.exception.domain.report.ReportGenerationException;
import com.demo.universityManagementApp.exception.notfound.StudentNotFoundException;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.StudentRepository;
import com.demo.universityManagementApp.repository.entity.Student;
import com.demo.universityManagementApp.rest.model.ReportFormat;
import com.demo.universityManagementApp.rest.model.request.BulkReportRequest;
import com.demo.universityManagementApp.rest.model.response.BulkReportResponse;
import com.demo.universityManagementApp.rest.model.response.ReportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * High-performance asynchronous report generation service leveraging virtual threads for non-blocking operations.
 * Provides bulk report processing capabilities with optimal resource utilization and horizontal scalability.
 * Supports both TXT and CSV report formats with comprehensive error handling and partial success reporting.
 * This service implements virtual thread-based concurrency for maximum throughput while maintaining
 * transactional boundaries for each report generation to ensure data consistency.
 * @see org.springframework.stereotype.Service
 * @see org.springframework.scheduling.annotation.Async
 * @see ReportService
 * @see BaseReportService
 * @see java.util.concurrent.CompletableFuture
 */
@Slf4j
@Service
public class AsyncReportService extends BaseReportService {

    private final AsyncTaskExecutor virtualThreadTaskExecutor;
    private final TransactionTemplate transactionTemplate;

    /**
     * Constructs an AsyncReportService with all required dependencies for asynchronous report processing.
     * Initializes the virtual thread executor for non-blocking operations and transaction template.
     *
     * @param studentRepository repository for student entity access and data retrieval
     * @param departmentRepository repository for department entity access and validation
     * @param virtualThreadTaskExecutor executor configured for virtual thread-based asynchronous task execution
     * @param transactionTemplate transaction management template for database operation consistency
     * @see org.springframework.core.task.AsyncTaskExecutor
     * @see org.springframework.transaction.support.TransactionTemplate
     */
    public AsyncReportService(final StudentRepository studentRepository, final DepartmentRepository departmentRepository,
                              @Qualifier("virtualThreadTaskExecutor") final AsyncTaskExecutor virtualThreadTaskExecutor,
                              final TransactionTemplate transactionTemplate) {
        super(studentRepository, departmentRepository);
        this.virtualThreadTaskExecutor = virtualThreadTaskExecutor;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Asynchronously generates reports for multiple students in bulk using virtual thread concurrency.
     * Each student report is processed independently in its own virtual thread, allowing non-blocking
     * execution.
     *
     * @param request {@link BulkReportRequest} containing the list of student external IDs and desired report format
     * @return {@link CompletableFuture} containing {@link BulkReportResponse} with generation statistics and file metadata
     * @throws ReportGenerationException if any individual report generation encounters unrecoverable errors
     * @see #generateStudentReportAsync(String, ReportFormat)
     * @see ReportService#generateStudentReport(String, ReportFormat)
     */
    public CompletableFuture<BulkReportResponse> generateBulkReportsAsync(final BulkReportRequest request) {
        log.info("Starting bulk report generation for {} students", request.getStudentIds().size());

        List<CompletableFuture<ReportResponse>> reports = request.getStudentIds().stream()
                .map(studentId -> virtualThreadTaskExecutor.submitCompletable(() ->
                        generateStudentReport(studentId, request.getFormat())))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(reports.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<ReportResponse> successfulReports = reports.stream()
                            .filter(future -> !future.isCompletedExceptionally())
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    log.info("Bulk generation completed: {}/{} successful", successfulReports.size(), request.getStudentIds().size());

                    return BulkReportResponse.builder()
                            .totalRequests(request.getStudentIds().size())
                            .successfulGenerations(successfulReports.size())
                            .failedGenerations(request.getStudentIds().size() - successfulReports.size())
                            .generatedAt(OffsetDateTime.now())
                            .fileNames(successfulReports.stream()
                                    .map(ReportResponse::getFileName)
                                    .collect(Collectors.toList()))
                            .build();
                });
    }

    /**
     * Generates a report for a single student within a transactional boundary to ensure data consistency.
     * Prevents LazyInitializationException by managing entity access within proper transactional context.
     * The report is written to the filesystem with atomic file operations and error handling.
     *
     * @param studentId the external ID of the student for whom the report is generated
     * @param format the {@link ReportFormat} (TXT, CSV) specifying the output format and structure
     * @return {@link ReportResponse} containing metadata about the generated report file and content
     * @throws ReportGenerationException if any I/O, transactional, or data mapping error occurs during generation
     * @see #generateStudentReportAsync(String, ReportFormat)
     * @see org.springframework.transaction.support.TransactionTemplate
     */
    private ReportResponse generateStudentReport(final String studentId, final ReportFormat format) {
        return transactionTemplate.execute(status -> {
            log.info("Generating report for Student with external ID: {} on virtual thread: {} with Format: {}", studentId, Thread.currentThread(), format);

            try {
                Student student = studentRepository.findByExternalId(studentId).orElseThrow(() -> new StudentNotFoundException(studentId));
                String content = generateStudentReportContent(student, format);
                String fileName = generateFileName(studentId, format);

                ensureReportsDirectoryExists();
                Path filePath = REPORTS_DIR.resolve(fileName);

                Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                ReportResponse response = buildReportResponse(student, content, format, fileName, filePath);
                log.info("Successfully generated report for student {}: {}", studentId, fileName);
                return response;

            } catch (Exception e) {
                log.error("Failed to generate report for student: {}", studentId, e);
                throw new ReportGenerationException("Error occured while generating report", e);
            }
        });
    }

    /**
     * Generates a student report asynchronously using virtual threads to prevent blocking operations.
     * Executes the report generation task on a dedicated virtual thread, allowing the calling thread
     * to continue processing other requests while the report is being generated.
     *
     * @param studentId student external ID for entity lookup and data retrieval
     * @param format {@link ReportFormat} specifying the desired output format (TXT or CSV)
     * @return {@link CompletableFuture} containing the {@link ReportResponse} of the generated report
     * @see #generateBulkReportsAsync(BulkReportRequest)
     * @see ReportService#generateStudentReport(String, ReportFormat)
     */
    @Async("virtualThreadTaskExecutor")
    public CompletableFuture<ReportResponse> generateStudentReportAsync(final String studentId, final ReportFormat format) {
        log.info("Generating async report for Student with external ID: {} on virtual thread: {}", studentId, Thread.currentThread());

        try {
            ReportResponse response = generateStudentReport(studentId, format);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }


    /**
     * Lists available report files asynchronously by scanning the reports directory using virtual threads.
     * Provides non-blocking filesystem access to retrieve all existing report file names with proper
     * resource management and exception handling.
     *
     * @return {@link CompletableFuture} containing list of report file names present in the reports directory
     * @see ReportService#listAvailableReports()
     * @see java.nio.file.Files
     */
    @Async("virtualThreadTaskExecutor")
    public CompletableFuture<List<String>> listAvailableReportsAsync() {
        try {
            log.info("Listing available reports on virtual thread: {}", Thread.currentThread());
            ensureReportsDirectoryExists();
            try (var stream = Files.list(REPORTS_DIR)) {
                List<String> reports = stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
                return CompletableFuture.completedFuture(reports);
            }
        } catch (IOException e) {
            log.error("Failed to list available reports", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Builds a {@link ReportResponse} object for a generated student report.
     * Populates metadata fields including content, format, entity information, and file statistics
     *
     *
     * @param student the {@link Student} entity used for report generation
     * @param content the generated report content as a formatted string
     * @param format the {@link ReportFormat} used for content generation
     * @param fileName the generated file name for report persistence
     * @param filePath the absolute {@link Path} to the generated report file
     * @return {@link ReportResponse} containing all metadata, file size, and entity details
     * @see ReportResponse
     * @see Student
     */
    private ReportResponse buildReportResponse(final Student student, final String content, final ReportFormat format, final String fileName, final Path filePath) {
        return ReportResponse.builder()
                .content(content)
                .format(format.name())
                .entityType("STUDENT")
                .entityId(student.getExternalId())
                .entityName(student.getName())
                .fileName(fileName)
                .fileSize(getFileSize(filePath))
                .build();
    }

    private String generateFileName(final String studentId, final ReportFormat format) {
        return String.format("student_report_%s_%s.%s", studentId, OffsetDateTime.now().format(fileTimestampFormatter), format.name().toLowerCase());
    }

    @Override
    protected OffsetDateTime getCurrentDateTime() {
        return OffsetDateTime.now();
    }
}
