package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.db.DatabaseDeleteConflictException;
import com.demo.universityManagementApp.exception.notfound.CourseNotFoundException;
import com.demo.universityManagementApp.exception.notfound.DepartmentNotFoundException;
import com.demo.universityManagementApp.repository.CourseRepository;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.entity.ClassSession;
import com.demo.universityManagementApp.repository.entity.Course;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.rest.model.request.CourseRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateCourseRequest;
import com.demo.universityManagementApp.rest.model.response.CourseResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

import static com.demo.universityManagementApp.util.Helper.Validate.validatePagination;

/**
 * Core service for managing academic course catalog and course-department relationships.
 * Provides comprehensive CRUD operations for courses with referential integrity validation
 * and proper cleanup of associated entities.
 * Key Features:
 * <ul>
 * <li> Course lifecycle management within department context </li>
 * <li> Validation of department associations </li>
 * <li> Prevention of orphaned class sessions and enrollments </li>
 * <li> Generation of unique course identifiers </li>
 * </ul>
 *
 * All operations maintain data consistency and prevent invalid state transitions.
 * Supports paginated queries for course catalog browsing.
 *
 * @see org.springframework.stereotype.Service
 * @see org.springframework.transaction.annotation.Transactional
 * @see StudentService
 * @see LecturerService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final ModelMapper modelMapper;

    /**
     * Creates a new course entity within the specified academic department with comprehensive validation.
     * Validates department existence and generates a unique external identifier following institutional
     * naming conventions. The course becomes immediately available for lecturer assignments and class scheduling.
     *
     * @param request {@link CourseRequest} containing course name, code, and department external ID
     * @return {@link CourseResponse} with persisted course details including department context and association counts
     * @throws DepartmentNotFoundException when the specified department does not exist in the system
     * @see #updateCourse(String, UpdateCourseRequest)
     * @see #getCoursesByDepartment(String, int, int)
     * @see DepartmentService#getDepartmentByExternalId(String)
     */
    @Transactional
    public CourseResponse createCourse(final CourseRequest request) {
        log.info("Creating course with name: {} and code: {} under department with external ID: {}", request.getName(), request.getCode(), request.getDepartmentId());
        Department department = departmentRepository.findByExternalId(request.getDepartmentId())
                .orElseThrow(() -> new DepartmentNotFoundException(request.getDepartmentId()));

        Course course = new Course(request.getName(), request.getCode(), department);
        course.setExternalId(generateCourseExternalId());

        Course savedCourse = courseRepository.save(course);
        log.info("Course created successfully with external ID: {}", savedCourse.getExternalId());

        return mapToResponse(savedCourse);
    }

    /**
     * Retrieves all courses from the system with comprehensive pagination and sorting capabilities.
     * Supports large course catalogs by returning paginated results with configurable page size
     * and multiple sorting criteria.
     *
     * @param pageNo zero-based page index for pagination, must be non-negative
     * @param pageSize number of courses per page, must be positive integer
     * @param sortBy property name to sort courses by, typically field names from Course entity
     * @return {@link Page} of {@link CourseResponse} objects representing all courses in the system
     * @throws IllegalArgumentException when pagination parameters are invalid or out of acceptable bounds
     * @see org.springframework.data.domain.Page
     * @see #getCoursesByDepartment(String, int, int)
     */
    public Page<CourseResponse> getAllCourses(final int pageNo, final int pageSize, final String sortBy) {
        log.info("Fetching all courses with args: pageNo: {}, pageSize: {}, sortBy: {}", pageNo, pageSize, sortBy);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<Course> coursePage = courseRepository.findAll(pageable);

        return coursePage.map(this::mapToResponse);
    }

    /**
     * Retrieves a specific course by its unique external identifier with complete entity relationships.
     * Provides full course details including department information, lecturer assignments, and class sessions
     * for course management and reporting.
     *
     * @param externalId unique external identifier of the course to retrieve
     * @return {@link CourseResponse} representing the complete course with all associated entities
     * @throws CourseNotFoundException when no course exists with the specified external identifier
     * @see #getCourseByName(String)
     * @see #getCoursesByDepartment(String, int, int)
     */
    public CourseResponse getCourseByExternalId(final String externalId) {
        log.info("Fetching course with external ID: {}", externalId);
        Course course = courseRepository.findByExternalId(externalId).orElseThrow(() -> new CourseNotFoundException(externalId));

        return mapToResponse(course);
    }

    /**
     * Retrieves a course by its exact name using case-insensitive matching for flexible searching.
     *
     * @param name the complete name of the course to search for, case-insensitive matching
     * @return {@link CourseResponse} representing the matched course with all relationships
     * @throws CourseNotFoundException when no course exists with the specified name
     * @see #getCourseByExternalId(String)
     * @see #getAllCourses(int, int, String)
     */
    public CourseResponse getCourseByName(final String name) {
        log.info("Fetching course with name: {}", name);
        Course course = courseRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new CourseNotFoundException(String.format("Course with name: %s not found", name), null));

        return mapToResponse(course);
    }

    /**
     * Retrieves all courses associated with a specific department with pagination support.
     * Results are sorted alphabetically by course name for consistent presentation.
     *
     * @param departmentId external ID of the department to filter courses
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of course records per page
     * @return {@link Page} of {@link CourseResponse} objects associated with the specified department
     * @throws DepartmentNotFoundException when the specified department does not exist
     * @throws IllegalArgumentException when pagination parameters are invalid
     * @see DepartmentService#getDepartmentByExternalId(String)
     * @see #getAllCourses(int, int, String)
     */
    public Page<CourseResponse> getCoursesByDepartment(final String departmentId, final int pageNo, final int pageSize) {
        log.info("Fetching courses for department with external ID: {}, and args => pageNo: {}, pageSize: {}", departmentId, pageNo, pageSize);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("name"));
        Department department = departmentRepository.findByExternalId(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException(departmentId));

        Page<Course> coursePage = courseRepository.findByDepartmentExternalId(department.getExternalId(), pageable);
        return coursePage.map(this::mapToResponse);
    }

    /**
     * Updates an existing course's metadata with partial update support for flexible modifications.
     * Only non-null fields in the request are updated, allowing selective modification of course properties
     * without affecting unchanged attributes.
     *
     * @param externalId external ID of the course to update
     * @param request {@link UpdateCourseRequest} containing optional new name and code values
     * @return {@link CourseResponse} representing the updated course with refreshed entity relationships
     * @throws CourseNotFoundException when the specified course does not exist
     * @see #createCourse(CourseRequest)
     * @see #deleteCourse(String)
     */
    @Transactional
    public CourseResponse updateCourse(final String externalId, final UpdateCourseRequest request) {
        log.info("Updating course with external ID: {}", externalId);
        Course existingCourse = courseRepository.findByExternalId(externalId).orElseThrow(() -> new CourseNotFoundException(externalId));

        if (request.getName() != null) existingCourse.setName(request.getName());
        if (request.getCode() != null) existingCourse.setCode(request.getCode());

        Course updatedCourse = courseRepository.save(existingCourse);
        log.info("Course with ID: {} updated successfully", updatedCourse.getExternalId());

        return mapToResponse(updatedCourse);
    }

    /**
     * Deletes a course by its external ID with comprehensive referential integrity validation.
     * Ensures the course has no associated class sessions with enrolled students before allowing deletion
     * to maintain data consistency and prevent academic record corruption.
     *
     * @param externalId external ID of the course to delete
     * @throws CourseNotFoundException when the specified course does not exist
     * @throws DatabaseDeleteConflictException when the course has active class sessions with student enrollments
     * @see #createCourse(CourseRequest)
     * @see #updateCourse(String, UpdateCourseRequest)
     */
    @Transactional
    public void deleteCourse(final String externalId) {
        log.info("Deleting course with external ID: {}", externalId);
        Course course = courseRepository.findByExternalId(externalId).orElseThrow(() -> new CourseNotFoundException(externalId));

        if (!course.getClassSessions().isEmpty())
            throw new DatabaseDeleteConflictException("Course", externalId, String.format(
                    "Cannot delete course %s because class sessions are assigned to it and students are enrolled in them", course.getName()));

        courseRepository.delete(course);
        log.info("Course with external ID: {} deleted successfully", externalId);
    }

    /**
     * Generates a unique external identifier for a new course following institutional naming conventions.
     * Uses sequential numbering prefixed with 'CRS' for clear course identification and system integration.
     * Format: CRS<sequence_number> where sequence_number starts from 1 and increments.
     *
     * @return generated unique external ID string for the new course
     * @see #createCourse(CourseRequest)
     */
    private String generateCourseExternalId() {
        long count = courseRepository.count();
        return String.format("CRS%d", count + 1);
    }

    private CourseResponse mapToResponse(final Course course) {
        CourseResponse response = modelMapper.map(course, CourseResponse.class);
        response.setDepartmentId(course.getDepartment().getExternalId());
        response.setDepartmentName(course.getDepartment().getName());
        response.setLecturerCount(course.getLecturerCount());
        response.setClassCount(course.getClassCount());
        response.setLecturerIds(course.getLecturers().stream()
                .map(lecturer -> lecturer.getExternalId())
                .collect(Collectors.toSet()));
        response.setClassIds(course.getClassSessions().stream()
                .map(classSession -> classSession.getExternalId())
                .collect(Collectors.toSet()));
        return response;
    }
}
