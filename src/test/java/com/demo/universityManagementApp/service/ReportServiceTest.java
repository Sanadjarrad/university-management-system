package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.notfound.StudentNotFoundException;
import com.demo.universityManagementApp.repository.*;
import com.demo.universityManagementApp.repository.entity.*;
import com.demo.universityManagementApp.rest.model.ReportFormat;
import com.demo.universityManagementApp.rest.model.response.ReportResponse;
import com.demo.universityManagementApp.service.report.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;


@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LecturerRepository lecturerRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @InjectMocks
    private ReportService reportService;

    private Student testStudent;
    private Department testDepartment;
    private ClassSession testClassSession;
    private Course testCourse;
    private Lecturer testLecturer;

    @BeforeEach
    void setUp() {
        testDepartment = new Department("DEPT1", "Computer Science", "CS");

        testStudent = Student.builder()
                .externalId("STUD1")
                .name("Sanad Anwar Jarrad")
                .email("sajarrad24@university.com")
                .phone("0791234567")
                .department(testDepartment)
                .enrollmentYear(2024)
                .build();

        testCourse = new Course("COURSE1", "Mathematics", "MATH101", testDepartment);

        testLecturer = Lecturer.builder()
                .externalId("LECT1")
                .name("Sanad Jarrad")
                .email("sanad.jarrad@university.com")
                .phone("0781234567")
                .department(testDepartment)
                .build();

        TimeSlot timeSlot = new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 30), DayOfWeek.MONDAY);
        testClassSession = new ClassSession("CLASS1", testCourse, testLecturer, timeSlot, "Room 101", 30);
    }

    @Test
    void generateStudentReport_TXT_Format() throws IOException {
        testStudent.addClassSession(testClassSession);

        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));
        when(classSessionRepository.findByExternalId("CLASS1")).thenReturn(Optional.of(testClassSession));
        when(courseRepository.findByExternalId("COURSE1")).thenReturn(Optional.of(testCourse));
        when(lecturerRepository.findByExternalId("LECT1")).thenReturn(Optional.of(testLecturer));

        ReportResponse result = reportService.generateStudentReport("STUD1", ReportFormat.TXT);

        assertNotNull(result);
        assertTrue(result.getContent().contains("STUDENT REPORT"));
        assertTrue(result.getContent().contains("Student Details:"));
        assertTrue(result.getContent().contains("ID: STUD1"));
        assertTrue(result.getContent().contains("Name: Sanad Anwar Jarrad"));
        assertEquals(ReportFormat.TXT.name(), result.getFormat());
        assertEquals("STUDENT", result.getEntityType());
    }

    @Test
    void generateStudentReport_CSV_Format() throws IOException {
        testStudent.addClassSession(testClassSession);

        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));
        when(classSessionRepository.findByExternalId("CLASS1")).thenReturn(Optional.of(testClassSession));
        when(courseRepository.findByExternalId("COURSE1")).thenReturn(Optional.of(testCourse));
        when(lecturerRepository.findByExternalId("LECT1")).thenReturn(Optional.of(testLecturer));

        ReportResponse result = reportService.generateStudentReport("STUD1", ReportFormat.CSV);

        assertNotNull(result);
        assertTrue(result.getContent().contains("Student ID,Name,Email,Phone,Enrollment Year,Department"));
        assertTrue(result.getContent().contains("Course,Day,Time,Location,Lecturer,Enrollment"));
        assertEquals(ReportFormat.CSV.name(), result.getFormat());
    }

    @Test
    void generateStudentReport_StudentNotFound() {
        when(studentRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class, () -> reportService.generateStudentReport("UNKNOWN", ReportFormat.TXT));
    }

    @Test
    void generateStudentReport_TXT_NoClasses() throws IOException {
        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));

        ReportResponse result = reportService.generateStudentReport("STUD1", ReportFormat.TXT);

        assertNotNull(result);
        assertTrue(result.getContent().contains("No classes enrolled."));
    }
}
