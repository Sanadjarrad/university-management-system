package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.db.DatabaseDeleteConflictException;
import com.demo.universityManagementApp.exception.notfound.DepartmentNotFoundException;
import com.demo.universityManagementApp.exception.notfound.LecturerNotFoundException;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.LecturerRepository;
import com.demo.universityManagementApp.repository.entity.ClassSession;
import com.demo.universityManagementApp.repository.entity.Course;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.repository.entity.Lecturer;
import com.demo.universityManagementApp.rest.model.request.LecturerRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateLecturerRequest;
import com.demo.universityManagementApp.rest.model.response.LecturerResponse;
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

import java.util.stream.Collectors;

import static com.demo.universityManagementApp.util.Helper.Generate.generateLecturerEmail;
import static com.demo.universityManagementApp.util.Helper.Validate.validatePagination;

/**
 * Service for managing academic staff (lecturers) and their departmental assignments within the institution.
 * Provides comprehensive CRUD functionality including creation, retrieval, updating, and deletion of lecturers
 * with strict referential integrity validation and teaching assignment management.
 * Key Features:
 * <ul>
 * <li> Automatic email generation based on institutional naming conventions </li>
 * <li> Department assignment validation and management </li>
 * <li> Teaching load tracking through course associations </li>
 * <li> Conflict prevention for lecturer deletion with active teaching assignments </li>
 * </ul>
 * Maintains consistency between lecturer records and their academic responsibilities across the institution.
 *
 * @see org.springframework.stereotype.Service
 * @see org.springframework.transaction.annotation.Transactional
 * @see ClassSessionService
 * @see CourseService
 * @see DepartmentService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LecturerService {

    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final ModelMapper modelMapper;

    /**
     * Creates a new lecturer record within the specified academic department with automatic credential generation.
     * Generates a unique external identifier and institutional email address based on naming conventions.
     * The lecturer becomes immediately available for course assignments and class scheduling.
     *
     * @param request {@link LecturerRequest} containing lecturer personal details and department assignment
     * @return {@link LecturerResponse} with persisted lecturer details including department context and teaching associations
     * @throws DepartmentNotFoundException when the assigned department does not exist in the system
     * @see #updateLecturer(String, UpdateLecturerRequest)
     * @see #getLecturersByDepartment(String, int, int)
     */
    @Transactional
    public LecturerResponse createLecturer(final LecturerRequest request) {
        log.info("Creating lecturer with name: {} under department with external ID: {}", request.getName(), request.getDepartmentId());
        Department department = departmentRepository.findByExternalId(request.getDepartmentId())
                .orElseThrow(() -> new DepartmentNotFoundException(request.getDepartmentId()));

        String email = generateLecturerEmail(request.getName());

        Lecturer lecturer = Lecturer.builder()
                .externalId(generateLecturerExternalId())
                .name(request.getName())
                .email(email)
                .phone(request.getPhone())
                .department(department)
                .build();

        Lecturer savedLecturer = lecturerRepository.save(lecturer);
        log.info("Lecturer created successfully with external ID: {}", savedLecturer.getExternalId());

        return mapToResponse(savedLecturer);
    }

    /**
     * Retrieves all lecturers from the system with comprehensive pagination and sorting capabilities.
     *
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of lecturers per page
     * @param sortBy property name to sort lecturers by, typically name or email
     * @return {@link Page} of {@link LecturerResponse} objects representing all teaching faculty
     * @throws IllegalArgumentException when pagination parameters are invalid
     * @see org.springframework.data.domain.Page
     * @see #getLecturersByDepartment(String, int, int)
     */
    public Page<LecturerResponse> getAllLecturers(final int pageNo, final int pageSize, final String sortBy) {
        log.info("Fetching all lecturers with args: pageNo: {}, pageSize: {}, sortBy: {}", pageNo, pageSize, sortBy);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<Lecturer> lecturerPage = lecturerRepository.findAll(pageable);

        return lecturerPage.map(this::mapToResponse);
    }

    /**
     * Retrieves a specific lecturer by their unique external identifier with complete academic context.
     * Provides full lecturer details including department assignment, course responsibilities, and
     *
     * @param externalId unique external identifier of the lecturer to retrieve
     * @return {@link LecturerResponse} representing the complete lecturer with all teaching associations
     * @throws LecturerNotFoundException when no lecturer exists with the specified external identifier
     * @see #getLecturerByName(String)
     * @see #getLecturersByDepartment(String, int, int)
     */
    public LecturerResponse getLecturerByExternalId(final String externalId) {
        log.info("Fetching lecturer with external ID: {}", externalId);
        Lecturer lecturer = lecturerRepository.findByExternalId(externalId).orElseThrow(() -> new LecturerNotFoundException(externalId));

        return mapToResponse(lecturer);
    }

    /**
     * Retrieves a lecturer by their exact name using case-insensitive matching for faculty lookup.
     * Useful for administrative interfaces and teaching assignment operations where external IDs are unknown.
     *
     * @param name the complete name of the lecturer to search for, case-insensitive matching
     * @return {@link LecturerResponse} representing the matched lecturer with all academic relationships
     * @throws LecturerNotFoundException when no lecturer exists with the specified name
     * @see #getLecturerByExternalId(String)
     * @see #getAllLecturers(int, int, String)
     */
    public LecturerResponse getLecturerByName(final String name) {
        log.info("Fetching lecturer with name: {}", name);
        Lecturer lecturer = lecturerRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new LecturerNotFoundException(String.format("Lecturer with name: %s not found", name), null));

        return mapToResponse(lecturer);
    }

    /**
     * Retrieves all lecturers associated with a specific department with pagination support.
     * Provides departmental faculty directories for academic planning and department management interfaces.
     * Results are sorted alphabetically by lecturer name for consistent presentation.
     *
     * @param departmentId external ID of the department to filter lecturers
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of lecturer records per page
     * @return {@link Page} of {@link LecturerResponse} objects associated with the specified department
     * @throws DepartmentNotFoundException when the specified department does not exist
     * @throws IllegalArgumentException when pagination parameters are invalid
     * @see DepartmentService#getDepartmentByExternalId(String)
     * @see #getAllLecturers(int, int, String)
     */
    public Page<LecturerResponse> getLecturersByDepartment(final String departmentId, final int pageNo, final int pageSize) {
        log.info("Fetching lecturers for department with external ID: {}, and args => pageNo: {}, pageSize: {}", departmentId, pageNo, pageSize);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("name"));
        Department department = departmentRepository.findByExternalId(departmentId).orElseThrow(() -> new DepartmentNotFoundException(departmentId));

        Page<Lecturer> lecturerPage = lecturerRepository.findByDepartmentExternalId(department.getExternalId(), pageable);
        return lecturerPage.map(this::mapToResponse);
    }

    /**
     * Updates an existing lecturer's personal details with partial update support for administrative changes.
     * Only non-null fields in the request are updated, allowing selective modification of lecturer properties
     * without affecting unchanged personal or professional attributes.
     *
     * @param externalId external ID of the lecturer to update
     * @param request {@link UpdateLecturerRequest} containing optional new name and phone values
     * @return {@link LecturerResponse} representing the updated lecturer with refreshed academic relationships
     * @throws LecturerNotFoundException when the specified lecturer does not exist
     * @see #createLecturer(LecturerRequest)
     * @see #deleteLecturer(String)
     */
    @Transactional
    public LecturerResponse updateLecturer(final String externalId, final UpdateLecturerRequest request) {
        log.info("Updating lecturer with external ID: {}", externalId);
        Lecturer existingLecturer = lecturerRepository.findByExternalId(externalId).orElseThrow(() -> new LecturerNotFoundException(externalId));

        if (request.getName() != null) existingLecturer.setName(request.getName());
        if (request.getPhone() != null) existingLecturer.setPhone(request.getPhone());

        Lecturer updatedLecturer = lecturerRepository.save(existingLecturer);
        log.info("Lecturer with external ID: {} updated successfully", updatedLecturer.getExternalId());

        return mapToResponse(updatedLecturer);
    }

    /**
     * Deletes a lecturer by their external ID with comprehensive teaching assignment validation.
     * Ensures the lecturer has no active class session assignments before allowing deletion to maintain
     * academic schedule integrity and prevent teaching assignment disruptions.
     *
     * @param externalId external ID of the lecturer to delete
     * @throws LecturerNotFoundException when the specified lecturer does not exist
     * @throws DatabaseDeleteConflictException when the lecturer has active class session assignments
     * @see #createLecturer(LecturerRequest)
     * @see #updateLecturer(String, UpdateLecturerRequest)
     */
    @Transactional
    public void deleteLecturer(final String externalId) {
        log.info("Deleting lecturer with external ID: {}", externalId);
        Lecturer lecturer = lecturerRepository.findByExternalId(externalId).orElseThrow(() -> new LecturerNotFoundException(externalId));

        if (!lecturer.getClassSessions().isEmpty())
            throw new DatabaseDeleteConflictException("Lecturer", externalId, String.format("Cannot delete lecturer %s because they are assigned to class sessions", lecturer.getName()));

        lecturerRepository.delete(lecturer);
        log.info("Lecturer with external ID: {} deleted successfully", externalId);
    }

    /**
     * Generates a unique external identifier for a new lecturer following institutional naming conventions.
     * Uses sequential numbering prefixed with 'LECT' starting from 5001 for clear faculty identification.
     * Format: LECT<sequence_number> where sequence_number starts from 5001 and increments.
     *
     * @return generated unique external ID string for the new lecturer
     * @see #createLecturer(LecturerRequest)
     */
    private String generateLecturerExternalId() {
        long count = lecturerRepository.count();
        return String.format("LECT%d", 5000 + count + 1);
    }

    private LecturerResponse mapToResponse(final Lecturer lecturer) {
        LecturerResponse response = modelMapper.map(lecturer, LecturerResponse.class);
        response.setDepartmentId(lecturer.getDepartment().getExternalId());
        response.setDepartmentName(lecturer.getDepartment().getName());
        response.setCourseCount(lecturer.getCourseCount());
        response.setClassCount(lecturer.getClassCount());
        response.setCourseIds(lecturer.getCourses().stream()
                .map(Course::getExternalId)
                .collect(Collectors.toSet()));
        response.setClassIds(lecturer.getClassSessions().stream()
                .map(ClassSession::getExternalId)
                .collect(Collectors.toSet()));
        return response;
    }
}
