package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.notfound.DepartmentNotFoundException;
import com.demo.universityManagementApp.exception.notfound.LecturerNotFoundException;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.LecturerRepository;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.repository.entity.Lecturer;
import com.demo.universityManagementApp.rest.model.request.LecturerRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateLecturerRequest;
import com.demo.universityManagementApp.rest.model.response.LecturerResponse;
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
class LecturerServiceTest {

    @Mock
    private LecturerRepository lecturerRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private LecturerService lecturerService;

    private Department testDepartment;
    private Lecturer testLecturer;
    private LecturerRequest lecturerRequest;

    @BeforeEach
    void setUp() {
        testDepartment = new Department("DEPT1", "Computer Science", "CS");

        testLecturer = Lecturer.builder()
                .externalId("LECT1")
                .name("Sanad Jarrad")
                .email("sanad.jarrad@university.com")
                .phone("0791234567")
                .department(testDepartment)
                .build();

        lecturerRequest = new LecturerRequest();
        lecturerRequest.setName("Sanad Jarrad");
        lecturerRequest.setPhone("0791234567");
        lecturerRequest.setDepartmentId("DEPT1");
    }

    @Test
    void createLecturer_Success() {
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));
        when(lecturerRepository.count()).thenReturn(0L);
        when(lecturerRepository.save(any(Lecturer.class))).thenReturn(testLecturer);

        LecturerResponse expectedResponse = new LecturerResponse();
        when(modelMapper.map(testLecturer, LecturerResponse.class)).thenReturn(expectedResponse);

        LecturerResponse result = lecturerService.createLecturer(lecturerRequest);

        assertNotNull(result);
        verify(lecturerRepository).save(any(Lecturer.class));
    }

    @Test
    void createLecturer_DepartmentNotFound() {
        lecturerRequest.setDepartmentId("UNKNOWN");
        when(departmentRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () -> lecturerService.createLecturer(lecturerRequest));
    }

    @Test
    void getLecturerByExternalId_Found() {
        when(lecturerRepository.findByExternalId("LECT1")).thenReturn(Optional.of(testLecturer));

        LecturerResponse expectedResponse = new LecturerResponse();
        when(modelMapper.map(testLecturer, LecturerResponse.class)).thenReturn(expectedResponse);

        LecturerResponse result = lecturerService.getLecturerByExternalId("LECT1");

        assertNotNull(result);
        verify(lecturerRepository).findByExternalId("LECT1");
    }

    @Test
    void getLecturerByExternalId_NotFound() {
        when(lecturerRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(LecturerNotFoundException.class, () -> lecturerService.getLecturerByExternalId("UNKNOWN"));
    }

    @Test
    void updateLecturer_Success() {
        when(lecturerRepository.findByExternalId("LECT1")).thenReturn(Optional.of(testLecturer));
        when(lecturerRepository.save(any(Lecturer.class))).thenReturn(testLecturer);

        LecturerResponse expectedResponse = new LecturerResponse();
        when(modelMapper.map(testLecturer, LecturerResponse.class)).thenReturn(expectedResponse);

        UpdateLecturerRequest updateRequest = new UpdateLecturerRequest();
        updateRequest.setName("Sanad Anwar Jarrad");
        updateRequest.setPhone("0781234567");

        LecturerResponse result = lecturerService.updateLecturer("LECT1", updateRequest);

        assertNotNull(result);
        assertEquals("Sanad Anwar Jarrad", testLecturer.getName());
        assertEquals("0781234567", testLecturer.getPhone());
    }

    @Test
    void updateLecturer_NotFound() {
        when(lecturerRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        UpdateLecturerRequest updateRequest = new UpdateLecturerRequest();

        assertThrows(LecturerNotFoundException.class, () -> lecturerService.updateLecturer("UNKNOWN", updateRequest));
    }

    @Test
    void deleteLecturer_Success() {
        when(lecturerRepository.findByExternalId("LECT1")).thenReturn(Optional.of(testLecturer));

        lecturerService.deleteLecturer("LECT1");

        verify(lecturerRepository).delete(testLecturer);
    }

    @Test
    void getAllLecturers() {
        Page<Lecturer> lecturerPage = new PageImpl<>(Collections.singletonList(testLecturer));
        when(lecturerRepository.findAll(any(Pageable.class))).thenReturn(lecturerPage);

        LecturerResponse expectedResponse = new LecturerResponse();
        when(modelMapper.map(testLecturer, LecturerResponse.class)).thenReturn(expectedResponse);

        Page<LecturerResponse> result = lecturerService.getAllLecturers(0, 10, "name");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getLecturersByDepartment() {
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));

        Page<Lecturer> lecturerPage = new PageImpl<>(Collections.singletonList(testLecturer));
        when(lecturerRepository.findByDepartmentExternalId(eq("DEPT1"), any(Pageable.class))).thenReturn(lecturerPage);

        LecturerResponse expectedResponse = new LecturerResponse();
        when(modelMapper.map(testLecturer, LecturerResponse.class)).thenReturn(expectedResponse);

        Page<LecturerResponse> result = lecturerService.getLecturersByDepartment("DEPT1", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
