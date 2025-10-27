package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.conflict.ClassEnrollmentConflictException;
import com.demo.universityManagementApp.exception.domain.classSession.ClassCapacityException;
import com.demo.universityManagementApp.exception.notfound.ClassSessionNotFoundException;
import com.demo.universityManagementApp.exception.notfound.DepartmentNotFoundException;
import com.demo.universityManagementApp.exception.notfound.StudentNotFoundException;
import com.demo.universityManagementApp.repository.ClassSessionRepository;
import com.demo.universityManagementApp.repository.DepartmentRepository;
import com.demo.universityManagementApp.repository.StudentRepository;
import com.demo.universityManagementApp.repository.entity.ClassSession;
import com.demo.universityManagementApp.repository.entity.Department;
import com.demo.universityManagementApp.repository.entity.Student;
import com.demo.universityManagementApp.rest.model.request.StudentRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateStudentRequest;
import com.demo.universityManagementApp.rest.model.response.EnrollmentResponse;
import com.demo.universityManagementApp.rest.model.response.StudentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

import static com.demo.universityManagementApp.util.Helper.Generate.generateStudentEmail;
import static com.demo.universityManagementApp.util.Helper.Validate.validatePagination;

/**
 * Comprehensive service for student lifecycle management including enrollment, class registration,
 * and academic record maintenance within the university system. Handles complete student operations
 * from admission to graduation with robust transaction management and business rule enforcement.
 * Key Features:
 * <ul>
 * <li> Student enrollment with departmental assignment and credential generation </li>
 * <li> Class registration with comprehensive conflict detection and capacity validation </li>
 * <li> Bulk operations with transactional safety and consistency guarantees </li>
 * </ul>
 * This service implements complex business rules for student enrollment including schedule conflicts,
 * capacity constraints, and academic prerequisite validation where applicable.
 *
 * @see org.springframework.stereotype.Service
 * @see org.springframework.transaction.annotation.Transactional
 * @see ClassSessionService
 * @see DepartmentService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final ModelMapper modelMapper;
    private final ClassSessionRepository classSessionRepository;

    /**
     * Creates a new student within the specified academic department with automatic credential generation.
     * Generates a unique external identifier and institutional email address based on student name and enrollment year.
     * The student becomes immediately available for class registration and academic activities.
     *
     * @param request {@link StudentRequest} containing student personal details, department assignment, and enrollment year
     * @return {@link StudentResponse} with persisted student details including department context and empty class registrations
     * @throws DepartmentNotFoundException when the assigned department does not exist in the system
     * @see #updateStudent(String, UpdateStudentRequest)
     * @see #enrollInClass(String, String)
     * @see DepartmentService#getDepartmentByExternalId(String)
     */
    @Transactional
    public StudentResponse createStudent(final StudentRequest request) {
        log.info("Creating student with name: {} under department with external ID: {}", request.getName(), request.getDepartmentId());
        Department department = departmentRepository.findByExternalId(request.getDepartmentId())
                .orElseThrow(() -> new DepartmentNotFoundException(request.getDepartmentId()));

        String email = generateStudentEmail(request.getName(), request.getEnrollmentYear());

        Student student = Student.builder()
                .externalId(generateStudentExternalId())
                .name(request.getName())
                .email(email)
                .phone(request.getPhone())
                .department(department)
                .enrollmentYear(request.getEnrollmentYear())
                .build();

        Student savedStudent = studentRepository.save(student);
        log.info("Student created successfully with external ID: {}", savedStudent.getExternalId());

        return mapToResponse(savedStudent);
    }

    /**
     * Retrieves all students from the system with comprehensive pagination and sorting capabilities.
     * Supports student directories, administrative dashboards, and reporting interfaces with flexible
     *
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of students per page
     * @param sortBy property name to sort students by, typically name or enrollment year
     * @return {@link Page} of {@link StudentResponse} objects representing all enrolled students
     * @throws IllegalArgumentException when pagination parameters are invalid
     * @see org.springframework.data.domain.Page
     * @see #getStudentsByDepartment(String, int, int)
     */
    public Page<StudentResponse> getAllStudents(final int pageNo, final int pageSize, final String sortBy) {
        log.info("Fetching all students with args: pageNo: {}, pageSize: {}, sortBy: {}", pageNo, pageSize, sortBy);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<Student> studentPage = studentRepository.findAll(pageable);

        return studentPage.map(this::mapToResponse);
    }

    /**
     * Retrieves students whose names contain a specified character sequence using case-insensitive partial matching.
     * Supports flexible student search operations for administrative interfaces and student lookup functionality
     * where complete name information may be incomplete.
     *
     * @param sequence substring to search for within student names, case-insensitive matching
     * @return {@link StudentResponse} representing the first matched student meeting the search criteria
     * @throws StudentNotFoundException when no students match the specified search sequence
     * @see #getStudentByName(String)
     * @see #getStudentByExternalId(String)
     */
    public StudentResponse getStudentByNameContaining(final String sequence) {
        log.info("Fetching student containing sequence: {}", sequence);
        Student student = studentRepository.findByNameContainingIgnoreCase(sequence)
                .orElseThrow(() -> new StudentNotFoundException(String.format("Student with name that contains sequence: %s not found", sequence)));

        return mapToResponse(student);
    }

    /**
     * Retrieves a student by their exact name using case-insensitive matching for precise student identification.
     *
     * @param name the complete name of the student to search for, case-insensitive matching
     * @return {@link StudentResponse} representing the matched student with all academic relationships
     * @throws StudentNotFoundException when no student exists with the specified name
     * @see #getStudentByNameContaining(String)
     * @see #getStudentByExternalId(String)
     */
    public StudentResponse getStudentByName(final String name) {
        log.info("Fetching student with name: {}", name);
        Student student = studentRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new StudentNotFoundException(String.format("Student with name: %s not found", name), null));

        return mapToResponse(student);
    }

    /**
     * Retrieves a specific student by their unique external identifier with complete academic context.
     * Provides full student details including department assignment, enrolled classes, and academic
     * information.
     *
     * @param externalId unique external identifier of the student to retrieve
     * @return {@link StudentResponse} representing the complete student with all academic relationships
     * @throws StudentNotFoundException when no student exists with the specified external identifier
     * @see #getStudentByName(String)
     * @see #getStudentsByDepartment(String, int, int)
     */
    public StudentResponse getStudentByExternalId(final String externalId) {
        log.info("Fetching student with external ID: {}", externalId);
        Student student = studentRepository.findByExternalId(externalId).orElseThrow(() -> new StudentNotFoundException(externalId));

        return mapToResponse(student);
    }

    /**
     * Retrieves all students associated with a specific department with pagination support.
     * Results are sorted alphabetically by student name.
     *
     * @param departmentId external ID of the department to filter students
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of student records per page
     * @return {@link Page} of {@link StudentResponse} objects associated with the specified department
     * @throws DepartmentNotFoundException when the specified department does not exist
     * @throws IllegalArgumentException when pagination parameters are invalid
     * @see DepartmentService#getDepartmentByExternalId(String)
     * @see #getAllStudents(int, int, String)
     */
    public Page<StudentResponse> getStudentsByDepartment(final String departmentId, final int pageNo, final int pageSize) {
        log.info("Fetching students for department with external ID: {}, and args => pageNo: {}, pageSize: {}", departmentId, pageNo, pageSize);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("name"));
        Department department = departmentRepository.findByExternalId(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException(departmentId));

        Page<Student> students = studentRepository.findByDepartmentExternalId(department.getExternalId(), pageable);
        return students.map(this::mapToResponse);
    }

    /**
     * Retrieves all students enrolled in a specific class session with pagination support.
     * Provides class rosters for instructional management, attendance tracking, and academic
     * administration. Results are sorted alphabetically by student name for consistent presentation.
     *
     * @param classSessionId external ID of the class session to filter enrolled students
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of student records per page
     * @return {@link Page} of {@link StudentResponse} objects enrolled in the specified class session
     * @throws ClassSessionNotFoundException when the specified class session does not exist
     * @throws IllegalArgumentException when pagination parameters are invalid
     * @see ClassSessionService#getClassSessionByExternalId(String)
     * @see #enrollInClass(String, String)
     */
    public Page<StudentResponse> getStudentsByClassSession(final String classSessionId, final int pageNo, final int pageSize) {
        log.info("Fetching students for Class Session with external ID: {}, and args => pageNo: {}, pageSize: {}", classSessionId, pageNo, pageSize);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("name"));
        ClassSession classSession = classSessionRepository.findByExternalId(classSessionId)
                .orElseThrow(() -> new ClassSessionNotFoundException(classSessionId));

        Page<Student> students = studentRepository.findByClassSessionExternalId(classSession.getExternalId(), pageable);
        return students.map(this::mapToResponse);
    }

    /**
     * Enrolls a student in a class session with comprehensive business rule validation and conflict detection.
     * Validates seat availability, prevents duplicate enrollments, and checks for scheduling conflicts with
     * existing class registrations to maintain academic schedule integrity.
     *
     * @param studentExternalId external ID of the student to enroll in the class session
     * @param classSessionExternalId external ID of the class session for enrollment
     * @return {@link EnrollmentResponse} containing enrollment status, student information, course details, and seat availability
     * @throws StudentNotFoundException when the specified student does not exist
     * @throws ClassSessionNotFoundException when the specified class session does not exist
     * @throws ClassCapacityException when the class session is at maximum capacity
     * @throws ClassEnrollmentConflictException for duplicate enrollments or schedule conflicts
     * @see #createStudent(StudentRequest)
     * @see ClassSessionService#hasAvailableSeats(String)
     */
    @Transactional
    public EnrollmentResponse enrollInClass(final String studentExternalId, final String classSessionExternalId) {
        log.info("Enrolling Student with external ID: {} in Class Session with external ID: {}", studentExternalId, classSessionExternalId);
        Student student = studentRepository.findByExternalId(studentExternalId)
                .orElseThrow(() -> new StudentNotFoundException(studentExternalId));

        ClassSession classSession = classSessionRepository.findByExternalId(classSessionExternalId)
                .orElseThrow(() -> new ClassSessionNotFoundException(classSessionExternalId));

        if (classSession.isFull()) throw new ClassCapacityException(classSessionExternalId, classSession.getMaxCapacity());

        if (student.getClassSessions().stream().anyMatch(cs -> cs.getExternalId().equals(classSessionExternalId)))
            throw new ClassEnrollmentConflictException(studentExternalId, classSessionExternalId);

        boolean hasConflict = student.getClassSessions().stream().anyMatch(existingClass -> existingClass.getTimeSlot().overlapsWith(classSession.getTimeSlot()));

        if (hasConflict) throw new ClassEnrollmentConflictException(studentExternalId, classSessionExternalId);

        classSession.addStudent(student);

        studentRepository.save(student);
        classSessionRepository.save(classSession);

        log.info("Student with external ID: {} enrolled successfully in Class Session with external ID: {}", studentExternalId, classSessionExternalId);

        return EnrollmentResponse.builder()
                .success(true)
                .message("Enrollment successful")
                .studentId(student.getExternalId())
                .classSessionId(classSession.getExternalId())
                .studentName(student.getName())
                .courseName(classSession.getCourse().getName())
                .availableSeats(classSession.getMaxCapacity() - classSession.getEnrolledCount())
                .build();
    }

    /**
     * Updates an existing student's personal details with partial update support for administrative changes.
     * Only non-null fields in the request are updated, allowing selective modification of student properties
     * without affecting unchanged personal or academic attributes.
     *
     * @param externalId external ID of the student to update
     * @param request {@link UpdateStudentRequest} containing optional new name and phone values
     * @return {@link StudentResponse} representing the updated student with refreshed academic relationships
     * @throws StudentNotFoundException when the specified student does not exist
     * @see #createStudent(StudentRequest)
     * @see #deleteStudent(String)
     */
    @Transactional
    public StudentResponse updateStudent(final String externalId, final UpdateStudentRequest request) {
        log.info("Updating student with external ID: {}", externalId);
        Student existingStudent = studentRepository.findByExternalId(externalId).orElseThrow(() -> new StudentNotFoundException(externalId));

        if (request.getName() != null) existingStudent.setName(request.getName());
        if (request.getPhone() != null) existingStudent.setPhone(request.getPhone());

        Student updatedStudent = studentRepository.save(existingStudent);
        log.info("Student with external ID: {} updated successfully", updatedStudent.getExternalId());

        return mapToResponse(updatedStudent);
    }

    /**
     * Deletes a student by their external ID with comprehensive academic record cleanup.
     * Removes the student from all enrolled class sessions before deletion to maintain academic
     * record consistency and prevent orphaned enrollment records in the system.
     *
     * @param externalId external ID of the student to delete
     * @throws StudentNotFoundException when the specified student does not exist
     * @see #createStudent(StudentRequest)
     * @see #updateStudent(String, UpdateStudentRequest)
     */
    @Transactional
    public void deleteStudent(final String externalId) {
        log.info("Deleting student with external ID: {}", externalId);
        Student student = studentRepository.findByExternalId(externalId).orElseThrow(() -> new StudentNotFoundException(externalId));
        student.getClassSessions().forEach(classSession -> classSession.removeStudent(student));

        studentRepository.delete(student);
        log.info("Student with external ID: {} deleted successfully", externalId);
    }

    /**
     * Generates a unique external identifier for a new student following institutional numbering conventions.
     * Uses sequential numbering starting from 15001.
     * Format: sequential integer where student numbers start from 15001 and increment.
     *
     * @return generated unique external ID string for the new student
     * @see #createStudent(StudentRequest)
     */
    private String generateStudentExternalId() {
        long count = studentRepository.count();
        return String.valueOf(15000 + count + 1);
    }

    public long getStudentCount() {
        return studentRepository.count();
    }

    private StudentResponse mapToResponse(final Student student) {
        StudentResponse response = modelMapper.map(student, StudentResponse.class);
        response.setDepartmentId(student.getDepartment().getExternalId());
        response.setDepartmentName(student.getDepartment().getName());
        response.setEnrolledClassCount(student.getEnrolledClassCount());
        response.setClassIds(student.getClassSessions().stream()
                .map(ClassSession::getExternalId)
                .collect(Collectors.toSet()));
        return response;
    }
}
