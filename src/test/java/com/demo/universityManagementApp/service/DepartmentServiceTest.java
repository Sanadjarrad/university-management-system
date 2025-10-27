package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.db.DatabaseDeleteConflictException;
import com.demo.universityManagementApp.exception.notfound.DepartmentNotFoundException;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.repository.entity.Student;
import com.demo.universityManagementApp.rest.model.request.DepartmentRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateDepartmentRequest;
import com.demo.universityManagementApp.rest.model.response.DepartmentResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private Department testDepartment;
    private DepartmentRequest departmentRequest;

    @BeforeEach
    void setUp() {
        testDepartment = new Department("DEPT1", "Computer Science", "CS");

        departmentRequest = new DepartmentRequest();
        departmentRequest.setName("Computer Science");
        departmentRequest.setCode("CS");
    }

    @Test
    void createDepartment_Success() {
        when(departmentRepository.count()).thenReturn(0L);
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);

        DepartmentResponse expectedResponse = new DepartmentResponse();
        when(modelMapper.map(testDepartment, DepartmentResponse.class)).thenReturn(expectedResponse);

        DepartmentResponse result = departmentService.createDepartment(departmentRequest);

        assertNotNull(result);
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void getDepartmentByExternalId_Found() {
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));

        DepartmentResponse expectedResponse = new DepartmentResponse();
        when(modelMapper.map(testDepartment, DepartmentResponse.class)).thenReturn(expectedResponse);

        DepartmentResponse result = departmentService.getDepartmentByExternalId("DEPT1");

        assertNotNull(result);
        verify(departmentRepository).findByExternalId("DEPT1");
    }

    @Test
    void getDepartmentByExternalId_NotFound() {
        when(departmentRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () -> departmentService.getDepartmentByExternalId("UNKNOWN"));
    }

    @Test
    void updateDepartment_Success() {
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(testDepartment);

        DepartmentResponse expectedResponse = new DepartmentResponse();
        when(modelMapper.map(testDepartment, DepartmentResponse.class)).thenReturn(expectedResponse);

        UpdateDepartmentRequest updateRequest = new UpdateDepartmentRequest();
        updateRequest.setName("Computer Science and Engineering");
        updateRequest.setCode("CSE");

        DepartmentResponse result = departmentService.updateDepartment("DEPT1", updateRequest);

        assertNotNull(result);
        assertEquals("Computer Science and Engineering", testDepartment.getName());
        assertEquals("CSE", testDepartment.getCode());
    }

    @Test
    void updateDepartment_NotFound() {
        when(departmentRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        UpdateDepartmentRequest updateRequest = new UpdateDepartmentRequest();

        assertThrows(DepartmentNotFoundException.class, () -> departmentService.updateDepartment("UNKNOWN", updateRequest));
    }

    @Test
    void deleteDepartment_Success() {
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));

        departmentService.deleteDepartment("DEPT1");

        verify(departmentRepository).delete(testDepartment);
    }

    @Test
    void deleteDepartment_WithStudents() {
        Department departmentWithStudents = new Department("DEPT1", "Computer Science", "CS");
        departmentWithStudents.addStudent(Student.builder()
                .externalId("STUD1")
                .name("Test Student")
                .email("test@university.com")
                .phone("0771234567")
                .department(departmentWithStudents)
                .enrollmentYear(2024)
                .build());

        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(departmentWithStudents));

        assertThrows(DatabaseDeleteConflictException.class, () -> departmentService.deleteDepartment("DEPT1"));
    }

    @Test
    void getAllDepartments() {
        Page<Department> departmentPage = new PageImpl<>(Collections.singletonList(testDepartment));
        when(departmentRepository.findAll(any(Pageable.class))).thenReturn(departmentPage);

        DepartmentResponse expectedResponse = new DepartmentResponse();
        when(modelMapper.map(testDepartment, DepartmentResponse.class)).thenReturn(expectedResponse);

        Page<DepartmentResponse> result = departmentService.getAllDepartments(0, 10, "name");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
