package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.db.DatabaseDeleteConflictException;
import com.demo.universityManagementApp.exception.notfound.DepartmentNotFoundException;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.rest.model.request.DepartmentRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateDepartmentRequest;
import com.demo.universityManagementApp.rest.model.response.DepartmentResponse;
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

import static com.demo.universityManagementApp.util.Helper.Validate.validatePagination;

/**
 * Service layer for managing academic department hierarchy and organizational structure.
 * Handles department lifecycle including creation, modification, and deletion with comprehensive
 * referential integrity checks to maintain data consistency across the academic system.
 * Critical logical rules:
 * <ul>
 * <li> Departments cannot be deleted while containing students, lecturers, or courses </li>
 * <li> Department codes must be unique across the institution </li>
 * <li> All associated entities must be properly handled during department operations </li>
 * This service ensures organizational data remains consistent and prevents orphaned academic records.
 * </ul>
 * @see org.springframework.stereotype.Service
 * @see org.springframework.transaction.annotation.Transactional
 * @see StudentService
 * @see LecturerService
 * @see CourseService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final ModelMapper modelMapper;

    /**
     * Creates a new academic department with the specified name and institutional code.
     * Generates a unique external identifier following departmental naming conventions and
     * establishes the department as an active organizational unit within the institution.
     *
     * @param request {@link DepartmentRequest} containing department name and institutional code
     * @return {@link DepartmentResponse} with persisted department details and empty association collections
     * @see #updateDepartment(String, UpdateDepartmentRequest)
     * @see #deleteDepartment(String)
     */
    @Transactional
    public DepartmentResponse createDepartment(final DepartmentRequest request) {
        log.info("Creating department with name: {} and code: {}", request.getName(), request.getCode());
        Department department = new Department(request.getName(), request.getCode());
        department.setExternalId(generateDepartmentExternalId());

        Department savedDepartment = departmentRepository.save(department);
        log.info("Department created successfully with external ID: {}", savedDepartment.getExternalId());

        return mapToResponse(savedDepartment);
    }

    /**
     * Retrieves all academic departments from the system with comprehensive pagination and sorting.
     *
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of departments per page
     * @param sortBy property name to sort departments by, typically name or code
     * @return {@link Page} of {@link DepartmentResponse} objects representing all departments
     * @throws IllegalArgumentException when pagination parameters are invalid
     * @see org.springframework.data.domain.Page
     * @see #getDepartmentByExternalId(String)
     */
    public Page<DepartmentResponse> getAllDepartments(final int pageNo, final int pageSize, final String sortBy) {
        log.info("Fetching all departments with args: pageNo: {}, pageSize: {}, sortBy: {}", pageNo, pageSize, sortBy);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<Department> departmentPage = departmentRepository.findAll(pageable);

        return departmentPage.map(this::mapToResponse);
    }

    /**
     * Retrieves a specific department by its unique external identifier with complete organizational context.
     * Provides full department details including student, lecturer, and course associations.
     *
     * @param externalId unique external identifier of the department to retrieve
     * @return {@link DepartmentResponse} representing the complete department with all entity associations
     * @throws DepartmentNotFoundException when no department exists with the specified external identifier
     * @see #getDepartmentByName(String)
     * @see #getAllDepartments(int, int, String)
     */
    public DepartmentResponse getDepartmentByExternalId(final String externalId) {
        log.info("Fetching department with external ID: {}", externalId);
        Department department = departmentRepository.findByExternalId(externalId).orElseThrow(() -> new DepartmentNotFoundException(externalId));

        return mapToResponse(department);
    }

    /**
     * Retrieves a department by its exact name using case-insensitive matching for administrative lookup.
     *
     * @param name the complete name of the department to search for, case-insensitive matching
     * @return {@link DepartmentResponse} representing the matched department with all relationships
     * @throws DepartmentNotFoundException when no department exists with the specified name
     * @see #getDepartmentByExternalId(String)
     * @see #getAllDepartments(int, int, String)
     */
    public DepartmentResponse getDepartmentByName(final String name) {
        log.info("Fetching department with name: {}", name);
        Department department = departmentRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new DepartmentNotFoundException(String.format("Department with name: %s not found", name), null));

        return mapToResponse(department);
    }

    /**
     * Updates an existing department's metadata with partial update support for organizational changes.
     * Only non-null fields in the request are updated, allowing selective modification of department
     * properties without affecting unchanged organizational attributes.
     *
     * @param externalId external ID of the department to update
     * @param request {@link UpdateDepartmentRequest} containing optional new name and code values
     * @return {@link DepartmentResponse} representing the updated department with refreshed associations
     * @throws DepartmentNotFoundException when the specified department does not exist
     * @see #createDepartment(DepartmentRequest)
     * @see #deleteDepartment(String)
     */
    @Transactional
    public DepartmentResponse updateDepartment(final String externalId, final UpdateDepartmentRequest request) {
        log.info("Updating department with external ID: {}", externalId);
        Department existingDepartment = departmentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new DepartmentNotFoundException(externalId));

        if (request.getName() != null) existingDepartment.setName(request.getName());
        if (request.getCode() != null) existingDepartment.setCode(request.getCode());

        Department updatedDepartment = departmentRepository.save(existingDepartment);
        log.info("Department with external ID: {} updated successfully", updatedDepartment.getExternalId());

        return mapToResponse(updatedDepartment);
    }

    /**
     * Deletes a department by its external ID with comprehensive referential integrity validation.
     * Ensures no students, lecturers, or courses are associated with the department before allowing deletion
     * to maintain academic data consistency and prevent organizational record corruption.
     *
     * @param externalId external ID of the department to delete
     * @throws DepartmentNotFoundException when the specified department does not exist
     * @throws DatabaseDeleteConflictException when the department has active student, lecturer, or course associations
     * @see #createDepartment(DepartmentRequest)
     * @see #updateDepartment(String, UpdateDepartmentRequest)
     */
    @Transactional
    public void deleteDepartment(final String externalId) {
        log.info("Deleting department with external ID: {}", externalId);
        Department department = departmentRepository.findByExternalId(externalId).orElseThrow(() -> new DepartmentNotFoundException(externalId));

        if (!department.getStudents().isEmpty()) throw new DatabaseDeleteConflictException("department", externalId, "Cannot delete department with students");
        if (!department.getLecturers().isEmpty()) throw new DatabaseDeleteConflictException("department", externalId, "Cannot delete department with lecturers");
        if (!department.getCourses().isEmpty()) throw new DatabaseDeleteConflictException("department", externalId, "Cannot delete department with courses");

        departmentRepository.delete(department);
        log.info("Department with external ID: {} deleted successfully", externalId);
    }

    /**
     * Generates a unique external identifier for a new department following institutional naming conventions.
     * Uses sequential numbering prefixed with 'DEP' for clear departmental identification and system integration.
     * Format: DEP<sequence_number> where sequence_number starts from 1 and increments.
     *
     * @return generated unique external ID string for the new department
     * @see #createDepartment(DepartmentRequest)
     */
    private String generateDepartmentExternalId() {
        long count = departmentRepository.count();
        return String.format("DEP%d", count + 1);
    }

    private DepartmentResponse mapToResponse(final Department department) {
        DepartmentResponse response = modelMapper.map(department, DepartmentResponse.class);
        response.setStudentCount(department.getStudentCount());
        response.setLecturerCount(department.getLecturerCount());
        response.setCourseCount(department.getCourseCount());
        response.setStudentIds(department.getStudents().stream()
                .map(student -> student.getExternalId())
                .collect(Collectors.toSet()));
        response.setLecturerIds(department.getLecturers().stream()
                .map(lecturer -> lecturer.getExternalId())
                .collect(Collectors.toSet()));
        response.setCourseIds(department.getCourses().stream()
                .map(course -> course.getExternalId())
                .collect(Collectors.toSet()));
        return response;
    }

}
