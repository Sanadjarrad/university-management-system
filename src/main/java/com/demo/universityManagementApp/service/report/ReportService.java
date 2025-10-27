package com.demo.universityManagementApp.service.report;

import com.demo.universityManagementApp.exception.Io.FileWriteException;
import com.demo.universityManagementApp.exception.notfound.DepartmentNotFoundException;
import com.demo.universityManagementApp.exception.notfound.StudentNotFoundException;
import com.demo.universityManagementApp.repository.*;
import com.demo.universityManagementApp.repository.entity.Course;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.repository.entity.Lecturer;
import com.demo.universityManagementApp.repository.entity.Student;
import com.demo.universityManagementApp.rest.model.ReportFormat;
import com.demo.universityManagementApp.rest.model.response.ReportResponse;
import com.demo.universityManagementApp.util.FileHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Synchronous implementation of student report generation for immediate processing and real-time response requirements.
 * Provides blocking operations for scenarios where immediate feedback is necessary, such as web interface
 * downloads and real-time reporting dashboards.
 * Real-time report generation for web interfaces with immediate download
 * Small-scale reporting needs with guaranteed completion timing
 * This service shares core infrastructure with {@link AsyncReportService} but executes all operations
 * on the calling thread for deterministic completion timing.
 *
 * @see org.springframework.stereotype.Service
 * @see AsyncReportService
 * @see BaseReportService
 */
@Service
@Slf4j
public class ReportService extends BaseReportService {

    /**
     * Constructs a ReportService with repository dependencies for synchronous student report generation.
     * Initializes the base service with student and department repositories for entity access
     * and data retrieval operations.
     *
     * @param studentRepository repository for student entity access and academic data queries
     * @param departmentRepository repository for department entity access and validation
     * @see StudentRepository
     * @see DepartmentRepository
     */
    public ReportService(final StudentRepository studentRepository, final DepartmentRepository departmentRepository) {
        super(studentRepository, departmentRepository);
    }

    /**
     * Generates a student report synchronously on the calling thread with error handling.
     * Blocks until report generation completes.
     *
     * @param studentId unique external ID for the student entity lookup and data retrieval
     * @param format {@link ReportFormat} specifying the desired output format (TXT or CSV)
     * @return {@link ReportResponse} with generated content, file metadata, and student information
     * @throws IOException on filesystem access failures during directory creation or file writing
     * @throws FileWriteException on report file creation or content writing failures
     * @throws StudentNotFoundException when the specified student record does not exist
     * @see AsyncReportService#generateStudentReportAsync(String, ReportFormat)
     * @see #listAvailableReports()
     */
    public ReportResponse generateStudentReport(final String studentId, final ReportFormat format) throws IOException {
        log.info("Generating student report for Student with external ID: {} in format {}", studentId, format);
        Student student = studentRepository.findByExternalId(studentId).orElseThrow(() -> new StudentNotFoundException(studentId));

        String content = generateStudentReportContent(student, format);
        String fileName = String.format("student_report_%s_%s.%s", studentId, LocalDateTime.now().format(fileTimestampFormatter),
                format.name().toLowerCase());

        ensureReportsDirectoryExists();

        Path filePath = REPORTS_DIR.resolve(fileName);
        try {
            FileHelper.writeFile(filePath, content);
            log.info("Student report saved to: {}", filePath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Error writing report file for student: {} - File path: {}", student.getExternalId(), filePath.toAbsolutePath(), e);
            throw new FileWriteException(filePath.toString(), String.format("Error occured while generating report for Student: %s ID: %s", student.getName(), student.getExternalId()));
        }

        return buildReportResponse(student, content, format, fileName, filePath);
    }

    /**
     * Lists all available report files present in the reports directory with synchronous filesystem access.
     * Ensures file existence and provides non-blocking filesystem access to retrieve all existing report file names with proper.
     *
     * @return list of report file names currently available in the reports directory
     * @throws IOException if an I/O error occurs during directory scanning or file access
     * @see AsyncReportService#listAvailableReportsAsync()
     * @see java.nio.file.Files
     */
    public List<String> listAvailableReports() throws IOException {
        log.info("Listing all available reports in {}", REPORTS_DIR.toAbsolutePath());
        ensureReportsDirectoryExists();
        return Files.list(REPORTS_DIR)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
    }

    /**
     * Builds a {@link ReportResponse} for a generated student report with all metadata.
     * Populates content, format specifications, entity references, and file statistics
     *
     * @param student the {@link Student} entity used for report generation and metadata
     * @param content the generated report content as a formatted string
     * @param format the {@link ReportFormat} used for content structure and file extension
     * @param fileName the generated file name for report identification and retrieval
     * @param filePath the absolute {@link Path} to the generated report file for size calculation
     * @return {@link ReportResponse} containing all report metadata, content, and entity information
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

    @Override
    protected OffsetDateTime getCurrentDateTime() {
        return OffsetDateTime.now();
    }
}

