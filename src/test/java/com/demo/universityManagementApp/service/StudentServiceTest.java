package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.notfound.DepartmentNotFoundException;
import com.demo.universityManagementApp.exception.notfound.StudentNotFoundException;
import com.demo.universityManagementApp.repository.ClassSessionRepository;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.StudentRepository;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.repository.entity.Student;
import com.demo.universityManagementApp.rest.model.request.StudentRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateStudentRequest;
import com.demo.universityManagementApp.rest.model.response.StudentResponse;
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

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private StudentService studentService;

    private Department testDepartment;
    private Student testStudent;
    private StudentRequest studentRequest;

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

        studentRequest = new StudentRequest();
        studentRequest.setName("Sanad Anwar Jarrad");
        studentRequest.setPhone("0791234567");
        studentRequest.setDepartmentId("DEPT1");
        studentRequest.setEnrollmentYear(2024);
    }

    @Test
    void createStudent_Success() {
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));
        when(studentRepository.count()).thenReturn(0L);
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

        StudentResponse expectedResponse = new StudentResponse();
        when(modelMapper.map(testStudent, StudentResponse.class)).thenReturn(expectedResponse);

        StudentResponse result = studentService.createStudent(studentRequest);

        assertNotNull(result);
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void createStudent_DepartmentNotFound() {
        studentRequest.setDepartmentId("UNKNOWN");
        when(departmentRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () -> studentService.createStudent(studentRequest));
    }

    @Test
    void getStudentByExternalId_Found() {
        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));

        StudentResponse expectedResponse = new StudentResponse();
        when(modelMapper.map(testStudent, StudentResponse.class)).thenReturn(expectedResponse);

        StudentResponse result = studentService.getStudentByExternalId("STUD1");

        assertNotNull(result);
        verify(studentRepository).findByExternalId("STUD1");
    }

    @Test
    void getStudentByExternalId_NotFound() {
        when(studentRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class, () -> studentService.getStudentByExternalId("UNKNOWN"));
    }

    @Test
    void updateStudent_Success() {
        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

        StudentResponse expectedResponse = new StudentResponse();
        when(modelMapper.map(testStudent, StudentResponse.class)).thenReturn(expectedResponse);

        UpdateStudentRequest updateRequest = new UpdateStudentRequest();
        updateRequest.setName("Sanad Jarrad");
        updateRequest.setPhone("0781234567");

        StudentResponse result = studentService.updateStudent("STUD1", updateRequest);

        assertNotNull(result);
        assertEquals("Sanad Jarrad", testStudent.getName());
        assertEquals("0781234567", testStudent.getPhone());
    }

    @Test
    void updateStudent_NotFound() {
        when(studentRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        UpdateStudentRequest updateRequest = new UpdateStudentRequest();

        assertThrows(StudentNotFoundException.class, () -> studentService.updateStudent("UNKNOWN", updateRequest));
    }

    @Test
    void deleteStudent_Success() {
        when(studentRepository.findByExternalId("STUD1")).thenReturn(Optional.of(testStudent));

        studentService.deleteStudent("STUD1");

        verify(studentRepository).delete(testStudent);
    }

    @Test
    void getAllStudents() {
        Page<Student> studentPage = new PageImpl<>(Collections.singletonList(testStudent));
        when(studentRepository.findAll(any(Pageable.class))).thenReturn(studentPage);

        StudentResponse expectedResponse = new StudentResponse();
        when(modelMapper.map(testStudent, StudentResponse.class)).thenReturn(expectedResponse);

        Page<StudentResponse> result = studentService.getAllStudents(0, 10, "name");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStudentsByDepartment() {
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));

        Page<Student> studentPage = new PageImpl<>(Collections.singletonList(testStudent));
        when(studentRepository.findByDepartmentExternalId(eq("DEPT1"), any(Pageable.class))).thenReturn(studentPage);

        StudentResponse expectedResponse = new StudentResponse();
        when(modelMapper.map(testStudent, StudentResponse.class)).thenReturn(expectedResponse);

        Page<StudentResponse> result = studentService.getStudentsByDepartment("DEPT1", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStudentCount() {
        when(studentRepository.count()).thenReturn(5L);

        long result = studentService.getStudentCount();

        assertEquals(5L, result);
    }
}
