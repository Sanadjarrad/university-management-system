package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.notfound.CourseNotFoundException;
import com.demo.universityManagementApp.exception.notfound.DepartmentNotFoundException;
import com.demo.universityManagementApp.repository.CourseRepository;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.entity.Course;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.rest.model.request.CourseRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateCourseRequest;
import com.demo.universityManagementApp.rest.model.response.CourseResponse;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CourseService courseService;

    private Department testDepartment;
    private Course testCourse;
    private CourseRequest courseRequest;

    @BeforeEach
    void setUp() {
        testDepartment = new Department("DEPT1", "Computer Science", "CS");
        testCourse = new Course("COURSE1", "Mathematics", "MATH101", testDepartment);

        courseRequest = new CourseRequest();
        courseRequest.setName("Mathematics");
        courseRequest.setCode("MATH101");
        courseRequest.setDepartmentId("DEPT1");
    }

    @Test
    void createCourse_Success() {
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));
        when(courseRepository.count()).thenReturn(0L);
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        CourseResponse expectedResponse = new CourseResponse();
        when(modelMapper.map(testCourse, CourseResponse.class)).thenReturn(expectedResponse);

        CourseResponse result = courseService.createCourse(courseRequest);

        assertNotNull(result);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void createCourse_DepartmentNotFound() {
        courseRequest.setDepartmentId("UNKNOWN");
        when(departmentRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () -> courseService.createCourse(courseRequest));
    }

    @Test
    void getCourseByExternalId_Found() {
        when(courseRepository.findByExternalId("COURSE1")).thenReturn(Optional.of(testCourse));

        CourseResponse expectedResponse = new CourseResponse();
        when(modelMapper.map(testCourse, CourseResponse.class)).thenReturn(expectedResponse);

        CourseResponse result = courseService.getCourseByExternalId("COURSE1");

        assertNotNull(result);
        verify(courseRepository).findByExternalId("COURSE1");
    }

    @Test
    void getCourseByExternalId_NotFound() {
        when(courseRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class, () -> courseService.getCourseByExternalId("UNKNOWN"));
    }

    @Test
    void updateCourse_Success() {
        when(courseRepository.findByExternalId("COURSE1")).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

        CourseResponse expectedResponse = new CourseResponse();
        when(modelMapper.map(testCourse, CourseResponse.class)).thenReturn(expectedResponse);

        UpdateCourseRequest updateRequest = new UpdateCourseRequest();
        updateRequest.setName("Advanced Mathematics");
        updateRequest.setCode("MATH201");

        CourseResponse result = courseService.updateCourse("COURSE1", updateRequest);

        assertNotNull(result);
        assertEquals("Advanced Mathematics", testCourse.getName());
        assertEquals("MATH201", testCourse.getCode());
    }

    @Test
    void updateCourse_NotFound() {
        when(courseRepository.findByExternalId("UNKNOWN")).thenReturn(Optional.empty());

        UpdateCourseRequest updateRequest = new UpdateCourseRequest();

        assertThrows(CourseNotFoundException.class, () -> courseService.updateCourse("UNKNOWN", updateRequest));
    }

    @Test
    void deleteCourse_Success() {
        when(courseRepository.findByExternalId("COURSE1")).thenReturn(Optional.of(testCourse));

        courseService.deleteCourse("COURSE1");

        verify(courseRepository).delete(testCourse);
    }

    @Test
    void getAllCourses() {
        Page<Course> coursePage = new PageImpl<>(Collections.singletonList(testCourse));
        when(courseRepository.findAll(any(Pageable.class))).thenReturn(coursePage);

        CourseResponse expectedResponse = new CourseResponse();
        when(modelMapper.map(testCourse, CourseResponse.class)).thenReturn(expectedResponse);

        Page<CourseResponse> result = courseService.getAllCourses(0, 10, "name");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getCoursesByDepartment() {
        when(departmentRepository.findByExternalId("DEPT1")).thenReturn(Optional.of(testDepartment));

        Page<Course> coursePage = new PageImpl<>(Collections.singletonList(testCourse));
        when(courseRepository.findByDepartmentExternalId(eq("DEPT1"), any(Pageable.class))).thenReturn(coursePage);

        CourseResponse expectedResponse = new CourseResponse();
        when(modelMapper.map(testCourse, CourseResponse.class)).thenReturn(expectedResponse);

        Page<CourseResponse> result = courseService.getCoursesByDepartment("DEPT1", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
