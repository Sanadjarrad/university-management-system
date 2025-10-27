package com.demo.universityManagementApp.async;

import com.demo.universityManagementApp.exception.notfound.NotFoundException;
import com.demo.universityManagementApp.exception.notfound.StudentNotFoundException;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.StudentRepository;
import com.demo.universityManagementApp.repository.entity.*;
import com.demo.universityManagementApp.rest.model.ReportFormat;
import com.demo.universityManagementApp.rest.model.request.BulkReportRequest;
import com.demo.universityManagementApp.rest.model.response.BulkReportResponse;
import com.demo.universityManagementApp.rest.model.response.ReportResponse;
import com.demo.universityManagementApp.service.report.AsyncReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AsyncReportServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AsyncTaskExecutor virtualThreadTaskExecutor;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AsyncReportService asyncReportService;

    private Student testStudent;
    private Department testDepartment;
    private ClassSession testClassSession;
    private Course testCourse;
    private Lecturer testLecturer;

    @BeforeEach
    void setUp() {
        testDepartment = new Department("DEPT1", "Computer Science", "CS");

        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));

        testStudent = Student.builder()
                .externalId("STUD1")
                .name("Sanad Anwar Jarrad")
                .email("sajarrad24@university.com")
                .phone("0791234567")
                .department(testDepartment)
                .enrollmentYear(2024)
                .build();

        when(studentRepository.findByExternalIdWithDepartment("STUD1")).thenReturn(Optional.of(testStudent));

        when(studentRepository.findByExternalIdWithDepartment(argThat(id -> !id.equals("STUD1")))).thenReturn(Optional.empty());

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

        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0, TransactionCallback.class);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });


        when(virtualThreadTaskExecutor.submitCompletable(any(Callable.class))).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return callable.call();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
        });

        when(studentRepository.findByExternalIdWithDepartment(anyString())).thenAnswer(invocation -> {
            String studentId = invocation.getArgument(0);
            if ("STUD1".equals(studentId)) return Optional.of(testStudent);

            Student student = Student.builder()
                    .externalId(studentId)
                    .name("Student " + studentId)
                    .email(studentId + "@university.com")
                    .phone("0790000000")
                    .department(testDepartment)
                    .enrollmentYear(2024)
                    .build();
            return Optional.of(student);
        });
    }

    @Test
    void generateStudentReportAsync_TXT_Format() throws Exception {
        testStudent.addClassSession(testClassSession);

        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));

        CompletableFuture<ReportResponse> resultFuture = asyncReportService.generateStudentReportAsync("STUD1", ReportFormat.TXT);
        ReportResponse result = resultFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertTrue(result.getContent().contains("STUDENT REPORT"));
        assertTrue(result.getContent().contains("Student Details:"));
        assertTrue(result.getContent().contains("ID: STUD1"));
        assertTrue(result.getContent().contains("Name: Sanad Anwar Jarrad"));
        assertEquals(ReportFormat.TXT.name(), result.getFormat());
        assertEquals("STUDENT", result.getEntityType());
    }

    @Test
    void generateStudentReportAsync_CSV_Format() throws Exception {
        testStudent.addClassSession(testClassSession);

        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));

        CompletableFuture<ReportResponse> resultFuture = asyncReportService.generateStudentReportAsync("STUD1", ReportFormat.CSV);
        ReportResponse result = resultFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertTrue(result.getContent().contains("Student ID,Name,Email,Phone,Enrollment Year,Department"));
        assertTrue(result.getContent().contains("Course,Day,Time,Location,Lecturer,Enrollment"));
        assertEquals(ReportFormat.CSV.name(), result.getFormat());
    }

    @Test
    void generateStudentReportAsync_StudentNotFound() {
        when(studentRepository.findByExternalIdWithDepartment("UNKNOWN")).thenReturn(Optional.empty());

        CompletableFuture<ReportResponse> resultFuture = asyncReportService.generateStudentReportAsync("UNKNOWN", ReportFormat.TXT);

        ExecutionException executionException = assertThrows(ExecutionException.class, () -> resultFuture.get(5, TimeUnit.SECONDS));
        assertInstanceOf(RuntimeException.class, executionException.getCause());
    }


    @Test
    void generateStudentReportAsync_TXT_NoClasses() throws Exception {
        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));

        CompletableFuture<ReportResponse> resultFuture = asyncReportService.generateStudentReportAsync("STUD1", ReportFormat.TXT);
        ReportResponse result = resultFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertTrue(result.getContent().contains("No classes enrolled."));
    }

    @Test
    void generateBulkReportsAsync_MultipleStudents_ConcurrentExecution() throws Exception {
        Student student2 = Student.builder()
                .externalId("STUD2")
                .name("Jane Doe")
                .email("jane.doe@university.com")
                .phone("0797654321")
                .department(testDepartment)
                .enrollmentYear(2024)
                .build();

        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));
        when(studentRepository.findByExternalId("STUD2")).thenReturn(Optional.of(student2));

        BulkReportRequest request = new BulkReportRequest();
        request.setStudentIds(List.of("STUD1", "STUD2"));
        request.setFormat(ReportFormat.TXT);

        CompletableFuture<BulkReportResponse> resultFuture = asyncReportService.generateBulkReportsAsync(request);
        BulkReportResponse result = resultFuture.get(10, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(2, result.getTotalRequests());
        assertEquals(2, result.getSuccessfulGenerations());
        assertEquals(0, result.getFailedGenerations());
        assertEquals(2, result.getFileNames().size());
        assertTrue(result.getFileNames().get(0).contains("student_report_STUD1"));
        assertTrue(result.getFileNames().get(1).contains("student_report_STUD2"));
    }

    @Test
    void generateBulkReportsAsync_LargeNumberOfStudents() throws Exception {
        when(studentRepository.findByExternalId(anyString())).thenAnswer(invocation -> {
            String studentId = invocation.getArgument(0);
            Student student = Student.builder()
                    .externalId(studentId)
                    .name("Student " + studentId)
                    .email(studentId + "@university.com")
                    .phone("0790000000")
                    .department(testDepartment)
                    .enrollmentYear(2024)
                    .build();
            return Optional.of(student);
        });

        List<String> studentIds = List.of("STUD1", "STUD2", "STUD3", "STUD4", "STUD5",
                "STUD6", "STUD7", "STUD8", "STUD9", "STUD10");

        BulkReportRequest request = new BulkReportRequest();
        request.setStudentIds(studentIds);
        request.setFormat(ReportFormat.CSV);

        CompletableFuture<BulkReportResponse> resultFuture = asyncReportService.generateBulkReportsAsync(request);
        BulkReportResponse result = resultFuture.get(15, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(10, result.getTotalRequests());
        assertEquals(10, result.getSuccessfulGenerations());
        assertEquals(0, result.getFailedGenerations());
        assertEquals(10, result.getFileNames().size());
    }

    @Test
    void generateBulkReportsAsync_EmptyStudentList() throws Exception {
        BulkReportRequest request = new BulkReportRequest();
        request.setStudentIds(List.of());
        request.setFormat(ReportFormat.TXT);

        CompletableFuture<BulkReportResponse> resultFuture = asyncReportService.generateBulkReportsAsync(request);
        BulkReportResponse result = resultFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(0, result.getTotalRequests());
        assertEquals(0, result.getSuccessfulGenerations());
        assertEquals(0, result.getFailedGenerations());
        assertTrue(result.getFileNames().isEmpty());
    }

    @Test
    void generateBulkReportsAsync_UsesVirtualThreadExecutor() throws Exception {
        Student student1 = Student.builder()
                .externalId("STUD1")
                .name("Alice")
                .department(testDepartment)
                .build();

        Student student2 = Student.builder()
                .externalId("STUD2")
                .name("Bob")
                .department(testDepartment)
                .build();

        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(student1));
        when(studentRepository.findByExternalId("STUD2")).thenReturn(Optional.of(student2));

        BulkReportRequest request = new BulkReportRequest();
        request.setStudentIds(List.of("STUD1", "STUD2"));
        request.setFormat(ReportFormat.TXT);

        CompletableFuture<BulkReportResponse> resultFuture = asyncReportService.generateBulkReportsAsync(request);

        verify(virtualThreadTaskExecutor, times(2)).submitCompletable(any(Callable.class));

        BulkReportResponse result = resultFuture.get(10, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(2, result.getSuccessfulGenerations());
    }

    @Test
    void concurrentReportGeneration_IndependentStudents() throws Exception {
        Student student1 = Student.builder()
                .externalId("STUD1")
                .name("Alice")
                .department(testDepartment)
                .build();

        Student student2 = Student.builder()
                .externalId("STUD2")
                .name("Bob")
                .department(testDepartment)
                .build();

        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(student1));
        when(studentRepository.findByExternalId("STUD2")).thenReturn(Optional.of(student2));

        CompletableFuture<ReportResponse> future1 = asyncReportService.generateStudentReportAsync("STUD1", ReportFormat.TXT);
        CompletableFuture<ReportResponse> future2 = asyncReportService.generateStudentReportAsync("STUD2", ReportFormat.CSV);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2);
        allFutures.get(10, TimeUnit.SECONDS);

        assertTrue(future1.isDone() && !future1.isCompletedExceptionally());
        assertTrue(future2.isDone() && !future2.isCompletedExceptionally());

        ReportResponse result1 = future1.get();
        ReportResponse result2 = future2.get();

        assertNotNull(result1);
        assertNotNull(result2);

        assertEquals("STUD1", result1.getEntityId());
        assertEquals("STUD2", result2.getEntityId());

        assertNotEquals(result1.getFileName(), result2.getFileName());
        assertNotEquals(result1.getContent(), result2.getContent());
    }

    @Test
    void concurrentReportGeneration_WithDelay() throws Exception {
        Student student1 = Student.builder()
                .externalId("STUD1")
                .name("Alice")
                .department(testDepartment)
                .build();

        Student student2 = Student.builder()
                .externalId("STUD2")
                .name("Bob")
                .department(testDepartment)
                .build();

        when(studentRepository.findByExternalId(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            Thread.sleep(100);
            return Optional.of(id.equals("STUD1") ? student1 : student2);
        });

        CompletableFuture<ReportResponse> future1 = asyncReportService.generateStudentReportAsync("STUD1", ReportFormat.TXT);
        CompletableFuture<ReportResponse> future2 = asyncReportService.generateStudentReportAsync("STUD2", ReportFormat.CSV);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2);
        allFutures.get(10, TimeUnit.SECONDS);

        assertTrue(future1.isDone() && !future1.isCompletedExceptionally());
        assertTrue(future2.isDone() && !future2.isCompletedExceptionally());

        ReportResponse result1 = future1.get();
        ReportResponse result2 = future2.get();

        assertNotNull(result1);
        assertNotNull(result2);

        assertEquals("STUD1", result1.getEntityId());
        assertEquals("STUD2", result2.getEntityId());
    }

    @Test
    void generateStudentReportAsync_UsesTransactionTemplate() throws Exception {
        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));

        CompletableFuture<ReportResponse> resultFuture = asyncReportService.generateStudentReportAsync("STUD1", ReportFormat.TXT);
        resultFuture.get(5, TimeUnit.SECONDS);

        verify(transactionTemplate, atLeastOnce()).execute(any(TransactionCallback.class));
    }

    @Test
    void generateBulkReportsAsync_UsesTransactionTemplateForEachStudent() throws Exception {
        Student student1 = Student.builder()
                .externalId("STUD1")
                .name("Alice")
                .department(testDepartment)
                .build();

        Student student2 = Student.builder()
                .externalId("STUD2")
                .name("Bob")
                .department(testDepartment)
                .build();

        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(student1));
        when(studentRepository.findByExternalId("STUD2")).thenReturn(Optional.of(student2));

        BulkReportRequest request = new BulkReportRequest();
        request.setStudentIds(List.of("STUD1", "STUD2"));
        request.setFormat(ReportFormat.TXT);

        CompletableFuture<BulkReportResponse> resultFuture = asyncReportService.generateBulkReportsAsync(request);
        resultFuture.get(10, TimeUnit.SECONDS);

        verify(transactionTemplate, times(2)).execute(any(TransactionCallback.class));
    }

    @Test
    void listAvailableReportsAsync_Success() throws Exception {
        CompletableFuture<List<String>> resultFuture = asyncReportService.listAvailableReportsAsync();

        assertTrue(resultFuture.isDone());

        List<String> result = resultFuture.get();
        assertNotNull(result);
    }

    @Test
    void generateStudentReportAsync_TimeoutBehavior() {
        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));

        CompletableFuture<ReportResponse> resultFuture = asyncReportService.generateStudentReportAsync("STUD1", ReportFormat.TXT);

        assertDoesNotThrow(() -> resultFuture.get(30, TimeUnit.SECONDS));
    }

    @Test
    void generateStudentReportAsync_FileWriteIntegration() throws Exception {
        testStudent.addClassSession(testClassSession);

        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));

        CompletableFuture<ReportResponse> resultFuture = asyncReportService.generateStudentReportAsync("STUD1", ReportFormat.TXT);
        ReportResponse result = resultFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertNotNull(result.getFileName());
        assertTrue(result.getFileName().contains("student_report_STUD1"));
        assertTrue(result.getFileName().endsWith(".txt"));
        assertTrue(result.getFileSize() > 0);
    }
}

