package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.conflict.AssignmentConflictException;
import com.demo.universityManagementApp.exception.conflict.ScheduleConflictException;
import com.demo.universityManagementApp.exception.domain.classSession.ClassCapacityException;
import com.demo.universityManagementApp.exception.notfound.ClassSessionNotFoundException;
import com.demo.universityManagementApp.exception.notfound.CourseNotFoundException;
import com.demo.universityManagementApp.exception.notfound.LecturerNotFoundException;
import com.demo.universityManagementApp.repository.ClassSessionRepository;
import com.demo.universityManagementApp.repository.CourseRepository;
import com.demo.universityManagementApp.repository.LecturerRepository;
import com.demo.universityManagementApp.repository.entity.*;
import com.demo.universityManagementApp.rest.model.request.ClassSessionRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateClassSessionRequest;
import com.demo.universityManagementApp.rest.model.response.ClassSessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTest {

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LecturerRepository lecturerRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ClassSessionService classSessionService;

    private Course testCourse;
    private Lecturer testLecturer;
    private ClassSession testClassSession;
    private ClassSessionRequest classSessionRequest;

    @BeforeEach
    void setUp() {
        Department department = new Department("DEPT1", "Computer Science", "CS");

        testCourse = new Course("COURSE1", "Mathematics", "MATH101", department);
        testLecturer = Lecturer.builder()
                .externalId("LECT1")
                .name("Sanad Jarrad")
                .email("sanad.jarrad@university.com")
                .phone("0791234567")
                .department(department)
                .build();
        testLecturer.addCourse(testCourse);

        TimeSlot timeSlot = new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 30), DayOfWeek.MONDAY);
        testClassSession = new ClassSession("CLASS1", testCourse, testLecturer, timeSlot, "Room 101", 30);

        classSessionRequest = new ClassSessionRequest();
        classSessionRequest.setCourseId("COURSE1");
        classSessionRequest.setLecturerId("LECT1");
        classSessionRequest.setStartTime(LocalTime.of(9, 0));
        classSessionRequest.setEndTime(LocalTime.of(10, 30));
        classSessionRequest.setDay(DayOfWeek.MONDAY);
        classSessionRequest.setLocation("Room 101");
        classSessionRequest.setMaxCapacity(30);
    }

    @Test
    void createClassSession_Success() {
        when(courseRepository.findByExternalId("COURSE1")).thenReturn(Optional.of(testCourse));
        when(lecturerRepository.findByExternalId("LECT1")).thenReturn(Optional.of(testLecturer));
        when(classSessionRepository.findByLecturerExternalId("LECT1")).thenReturn(Collections.emptyList());
        when(classSessionRepository.count()).thenReturn(0L);
        when(classSessionRepository.save(any(ClassSession.class))).thenReturn(testClassSession);

        ClassSessionResponse expectedResponse = new ClassSessionResponse();
        when(modelMapper.map(testClassSession, ClassSessionResponse.class)).thenReturn(expectedResponse);

        ClassSessionResponse result = classSessionService.createClassSession(classSessionRequest);

        assertNotNull(result);
        verify(classSessionRepository).save(any(ClassSession.class));
    }

    @Test
    void createClassSession_CourseNotFound() {
        classSessionRequest.setCourseId("UNKNOWN");
        when(courseRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class, () -> classSessionService.createClassSession(classSessionRequest));
    }

    @Test
    void createClassSession_LecturerNotFound() {
        classSessionRequest.setLecturerId("UNKNOWN");
        when(courseRepository.findByExternalId("COURSE1")).thenReturn(Optional.of(testCourse));
        when(lecturerRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(LecturerNotFoundException.class, () -> classSessionService.createClassSession(classSessionRequest));
    }

    @Test
    void createClassSession_LecturerNotAssignedToCourse() {
        Lecturer unassignedLecturer = Lecturer.builder()
                .externalId("LECT2")
                .name("Jane Doe")
                .email("jane.doe@university.com")
                .phone("0781234567")
                .department(testCourse.getDepartment())
                .build();

        when(courseRepository.findByExternalId("COURSE1")).thenReturn(Optional.of(testCourse));
        when(lecturerRepository.findByExternalId("LECT2")).thenReturn(Optional.of(unassignedLecturer));

        classSessionRequest.setLecturerId("LECT2");

        assertThrows(AssignmentConflictException.class, () ->
                classSessionService.createClassSession(classSessionRequest));
    }

    @Test
    void createClassSession_LecturerScheduleConflict() {
        TimeSlot conflictingTimeSlot = new TimeSlot(LocalTime.of(9, 30), LocalTime.of(11, 0), DayOfWeek.MONDAY);
        ClassSession conflictingClass = new ClassSession("CLASS2", testCourse, testLecturer,
                conflictingTimeSlot, "Room 102", 25);

        when(courseRepository.findByExternalId("COURSE1")).thenReturn(Optional.of(testCourse));
        when(lecturerRepository.findByExternalId("LECT1")).thenReturn(Optional.of(testLecturer));
        when(classSessionRepository.findByLecturerExternalId("LECT1")).thenReturn(Collections.singletonList(conflictingClass));

        assertThrows(ScheduleConflictException.class, () ->
                classSessionService.createClassSession(classSessionRequest));
    }

    @Test
    void getClassSessionByExternalId_Found() {
        when(classSessionRepository.findByExternalId("CLASS1")).thenReturn(Optional.of(testClassSession));

        ClassSessionResponse expectedResponse = new ClassSessionResponse();
        when(modelMapper.map(testClassSession, ClassSessionResponse.class)).thenReturn(expectedResponse);

        ClassSessionResponse result = classSessionService.getClassSessionByExternalId("CLASS1");

        assertNotNull(result);
        verify(classSessionRepository).findByExternalId("CLASS1");
    }

    @Test
    void getClassSessionByExternalId_NotFound() {
        when(classSessionRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ClassSessionNotFoundException.class, () ->
                classSessionService.getClassSessionByExternalId("UNKNOWN"));
    }

    @Test
    void updateClassSession_Success() {
        when(classSessionRepository.findByExternalId("CLASS1")).thenReturn(Optional.of(testClassSession));
        when(classSessionRepository.save(any(ClassSession.class))).thenReturn(testClassSession);

        ClassSessionResponse expectedResponse = new ClassSessionResponse();
        when(modelMapper.map(testClassSession, ClassSessionResponse.class)).thenReturn(expectedResponse);

        UpdateClassSessionRequest updateRequest = new UpdateClassSessionRequest();
        updateRequest.setLocation("Room 201");
        updateRequest.setMaxCapacity(35);

        ClassSessionResponse result = classSessionService.updateClassSession("CLASS1", updateRequest);

        assertNotNull(result);
        assertEquals("Room 201", testClassSession.getLocation());
        assertEquals(35, testClassSession.getMaxCapacity());
    }

    @Test
    void updateClassSession_NotFound() {
        when(classSessionRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        UpdateClassSessionRequest updateRequest = new UpdateClassSessionRequest();

        assertThrows(ClassSessionNotFoundException.class, () ->
                classSessionService.updateClassSession("UNKNOWN", updateRequest));
    }

    @Test
    void updateClassSession_CapacityReducedBelowEnrollment() {
        Student student = Student.builder()
                .externalId("STUD1")
                .name("Test Student")
                .email("test@university.com")
                .phone("0771234567")
                .department(testCourse.getDepartment())
                .enrollmentYear(2024)
                .build();
        testClassSession.addStudent(student);

        when(classSessionRepository.findByExternalId("CLASS1")).thenReturn(Optional.of(testClassSession));

        UpdateClassSessionRequest updateRequest = new UpdateClassSessionRequest();
        updateRequest.setMaxCapacity(0);

        assertThrows(ClassCapacityException.class, () -> classSessionService.updateClassSession("CLASS1", updateRequest));
    }

    @Test
    void deleteClassSession_Success() {
        when(classSessionRepository.findByExternalId("CLASS1")).thenReturn(Optional.of(testClassSession));

        classSessionService.deleteClassSession("CLASS1");

        verify(classSessionRepository).delete(testClassSession);
    }

    @Test
    void getAllClassSessions() {
        Page<ClassSession> classSessionPage = new PageImpl<>(Collections.singletonList(testClassSession));
        when(classSessionRepository.findAll(any(Pageable.class))).thenReturn(classSessionPage);

        ClassSessionResponse expectedResponse = new ClassSessionResponse();
        when(modelMapper.map(testClassSession, ClassSessionResponse.class)).thenReturn(expectedResponse);

        Page<ClassSessionResponse> result = classSessionService.getAllClassSessions(0, 10, "id");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void hasAvailableSeats_True() {
        when(classSessionRepository.findByExternalId("CLASS1")).thenReturn(Optional.of(testClassSession));

        boolean result = classSessionService.hasAvailableSeats("CLASS1");

        assertTrue(result);
    }

    @Test
    void getAvailableSeats() {
        when(classSessionRepository.findByExternalId("CLASS1")).thenReturn(Optional.of(testClassSession));

        int result = classSessionService.getAvailableSeats("CLASS1");

        assertEquals(30, result);
    }
}
