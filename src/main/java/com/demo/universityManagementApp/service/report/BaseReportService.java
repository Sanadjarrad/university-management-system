package com.demo.universityManagementApp.service.report;

import com.demo.universityManagementApp.exception.domain.report.DirectoryCreationException;
import com.demo.universityManagementApp.exception.notfound.DepartmentNotFoundException;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.StudentRepository;
import com.demo.universityManagementApp.repository.entity.Course;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.repository.entity.Lecturer;
import com.demo.universityManagementApp.repository.entity.Student;
import com.demo.universityManagementApp.rest.model.ReportFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Abstract base service providing reusable infrastructure for student report generation across synchronous
 * and asynchronous implementations. Implements the Template Method pattern for consistent report
 * formatting, filesystem management, and entity data retrieval.
 * Core features:
 * <ul>
 * <li> Report content generation in TXT and CSV formats </li>
 * <li> Filesystem management for report storage and retrieval </li>
 * <li> CSV injection prevention through proper field escaping </li>
 * <li> Student entity data enrichment and validation </li>
 * </ul>
 * This class provides the foundation for both {@link ReportService} and {@link AsyncReportService}
 * implementations while allowing customization of execution strategies.
 *
 * @see ReportService
 * @see AsyncReportService
 * @see java.nio.file.Path
 * @see java.time.format.DateTimeFormatter
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseReportService {

    protected final StudentRepository studentRepository;
    protected final DepartmentRepository departmentRepository;

    protected final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    protected final DateTimeFormatter fileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    protected final Path REPORTS_DIR = Paths.get("reports");

    /**
     * Generates comprehensive report content for a single student in the specified format.
     * Delegates to format-specific generators for TXT or CSV output while handling null-safe
     * data access and proper academic information formatting.
     *
     * @param student the {@link Student} entity containing enrollment, department, and class session data
     * @param format the desired {@link ReportFormat} (TXT or CSV) for output structure
     * @return fully formatted report content as a string ready for filesystem persistence
     * @see #generateStudentTxtReport(Student, StringBuilder)
     * @see #generateStudentCsvReport(Student, StringBuilder)
     * @see ReportFormat#TXT
     * @see ReportFormat#CSV
     */
    protected String generateStudentReportContent(final Student student, final ReportFormat format) {
        StringBuilder report = new StringBuilder();

        switch (format) {
            case TXT:
                generateStudentTxtReport(student, report);
                break;
            case CSV:
                generateStudentCsvReport(student, report);
                break;
        }

        return report.toString();
    }

    /**
     * Generates a structured TXT format report for a single student with academic details.
     * Retrieves department entity to ensure data consistency and provides human-readable formatting
     * with proper section headers and alignment for easy readability.
     *
     * @param student the {@link Student} entity containing all academic and personal information
     * @param report mutable {@link StringBuilder} to append formatted TXT content
     * @throws DepartmentNotFoundException if the student's department cannot be retrieved
     * @see #generateStudentCsvReport(Student, StringBuilder)
     * @see Student
     */
    protected void generateStudentTxtReport(final Student student, final StringBuilder report) {
        Department department = departmentRepository.findByExternalId(student.getDepartment().getExternalId())
                .orElseThrow(() -> new DepartmentNotFoundException(student.getDepartment().getExternalId()));

        report.append(String.format("STUDENT REPORT%n"))
                .append(String.format("Generated: %s%n", getCurrentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .append(String.format("==============%n%n"))
                .append(String.format("Student Details:%n"))
                .append(String.format("---------------%n"))
                .append(String.format("ID: %s%n", student.getExternalId()))
                .append(String.format("Name: %s%n", student.getName()))
                .append(String.format("Email: %s%n", student.getEmail()))
                .append(String.format("Phone: %s%n", student.getPhone()))
                .append(String.format("Enrollment Year: %d%n", student.getEnrollmentYear()))
                .append(String.format("Department: %s%n%n", department != null ? department.getName() : "N/A"))
                .append(String.format("Enrolled Classes:%n"))
                .append(String.format("----------------%n"));

        if (student.getClassSessions().isEmpty()) {
            report.append(String.format("No classes enrolled.%n"));
        } else {
            student.getClassSessions().forEach(classSession -> {
                Course course = classSession.getCourse();
                Lecturer lecturer = classSession.getLecturer();

                report.append(String.format(
                        "%n- %s (%s %s-%s) - %s - Taught by: %s - Seats: %d/%d%n",
                        course != null ? course.getName() : "Unknown Course",
                        classSession.getTimeSlot().getDay(),
                        classSession.getTimeSlot().getStartTime().format(timeFormatter),
                        classSession.getTimeSlot().getEndTime().format(timeFormatter),
                        classSession.getLocation(),
                        lecturer != null ? lecturer.getName() : "Unknown Lecturer",
                        classSession.getEnrolledCount(),
                        classSession.getMaxCapacity()
                ));
            });
        }
    }

    /**
     * Generates a CSV format report for a single student with proper field escaping.
     * suitable for spreadsheet applications and data processing systems.
     *
     * @param student the {@link Student} entity containing enrollment and academic data
     * @param report mutable {@link StringBuilder} to append properly formatted CSV content
     * @throws DepartmentNotFoundException if the student's department cannot be retrieved
     * @see #generateStudentTxtReport(Student, StringBuilder)
     * @see #escapeCsv(String)
     */
    protected void generateStudentCsvReport(final Student student, final StringBuilder report) {
        Department department = departmentRepository.findByExternalId(student.getDepartment().getExternalId())
                .orElseThrow(() -> new DepartmentNotFoundException(student.getDepartment().getExternalId()));

        report.append(String.format("Generated,%s%n", getCurrentDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .append(String.format("Student ID,Name,Email,Phone,Enrollment Year,Department%n"))
                .append(String.format("%s,%s,%s,%s,%d,%s%n%n",
                        student.getExternalId(),
                        escapeCsv(student.getName()),
                        student.getEmail(),
                        student.getPhone(),
                        student.getEnrollmentYear(),
                        escapeCsv(department != null ? department.getName() : "N/A")))
                .append(String.format("Course,Day,Time,Location,Lecturer,Enrollment%n"));

        student.getClassSessions().forEach(classSession -> {
            Course course = classSession.getCourse();
            Lecturer lecturer = classSession.getLecturer();

            report.append(String.format("%s,%s,%s,%s,%s,%d/%d%n",
                    escapeCsv(course != null ? course.getName() : "Unknown Course"),
                    classSession.getTimeSlot().getDay(),
                    classSession.getTimeSlot().getStartTime().format(timeFormatter) + "-" +
                            classSession.getTimeSlot().getEndTime().format(timeFormatter),
                    escapeCsv(classSession.getLocation()),
                    escapeCsv(lecturer != null ? lecturer.getName() : "Unknown Lecturer"),
                    classSession.getEnrolledCount(),
                    classSession.getMaxCapacity()));
        });
    }

    /**
     * Ensures that the reports directory exists in the filesystem with proper creation handling.
     * Creates the directory hierarchy if missing and provides consistent exception handling
     * for filesystem access issues across all report generation operations.
     *
     * @throws DirectoryCreationException if directory creation fails due to filesystem permissions or IO errors
     * @see java.nio.file.Files
     * @see java.nio.file.Paths
     */
    protected void ensureReportsDirectoryExists() {
        try {
            if (!Files.exists(REPORTS_DIR)) {
                log.info("Reports directory created at: {}", REPORTS_DIR.toAbsolutePath());
                Files.createDirectories(REPORTS_DIR);
            }
        } catch (IOException e) {
            log.error("Failed to create reports directory at: {}", REPORTS_DIR.toAbsolutePath(), e);
            throw new DirectoryCreationException(String.format("Failed to create reports directory: %s", REPORTS_DIR.toAbsolutePath()));
        }
    }

    /**
     * Retrieves the size of the specified file in bytes.
     * Performs existence check and returns 0 on error.
     *
     * @param filePath absolute or relative path to the target file
     * @return file size in bytes or 0 if file access fails
     */
    protected long getFileSize(final Path filePath) {
        ensureReportsDirectoryExists();

        try {
            return Files.size(filePath);
        } catch (IOException e) {
            log.warn("Failed to get file size for: {}", filePath, e);
            return 0L;
        }
    }

    /**
     * Escapes a string value for safe CSV output to prevent injection and formatting errors.
     * CSV escaping by wrapping values containing special characters in double quotes
     * and properly escaping internal quote characters.
     *
     * @param value raw string value requiring CSV-safe formatting
     * @return properly escaped CSV-safe string ready for inclusion in CSV output
     * @see #generateStudentCsvReport(Student, StringBuilder)
     */
    protected String escapeCsv(final String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    protected abstract OffsetDateTime getCurrentDateTime();
}
