package com.demo.universityManagementApp.service;

import com.demo.universityManagementApp.exception.conflict.AssignmentConflictException;
import com.demo.universityManagementApp.exception.conflict.ScheduleConflictException;
import com.demo.universityManagementApp.exception.db.DatabaseDeleteConflictException;
import com.demo.universityManagementApp.exception.domain.classSession.ClassCapacityException;
import com.demo.universityManagementApp.exception.notfound.ClassSessionNotFoundException;
import com.demo.universityManagementApp.exception.notfound.CourseNotFoundException;
import com.demo.universityManagementApp.exception.notfound.LecturerNotFoundException;
import com.demo.universityManagementApp.exception.notfound.StudentNotFoundException;
import com.demo.universityManagementApp.repository.ClassSessionRepository;
import com.demo.universityManagementApp.repository.CourseRepository;
import com.demo.universityManagementApp.repository.LecturerRepository;
import com.demo.universityManagementApp.repository.StudentRepository;
import com.demo.universityManagementApp.repository.entity.*;
import com.demo.universityManagementApp.rest.model.request.ClassSessionRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateClassSessionRequest;
import com.demo.universityManagementApp.rest.model.response.ClassSessionResponse;
import jakarta.persistence.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.demo.universityManagementApp.util.Helper.Validate.validatePagination;

/**
 * Service layer implementation for managing academic class sessions within the university system.
 * Handles business logic for class session lifecycle including creation, scheduling conflicts validation,
 * capacity management, and student enrollment tracking.
 * Critical logical rules:
 * <ul>
 *  <li> Lecturer assignment validation to courses </li>
 *  <li> Schedule conflict detection for lecturers and students </li>
 *  <li> Class capacity constraints </li>
 *  <li> Transactional integrity for enrollment operation </li>
 * </ul>
 * All write operations are transactional with proper rollback on business rule violations.
 * Read operations support pagination and sorting for large datasets.
 *
 *  @see org.springframework.stereotype.Service
 *  @see org.springframework.transaction.annotation.Transactional
 *  @see lombok.extern.slf4j.Slf4j
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final CourseRepository courseRepository;
    private final LecturerRepository lecturerRepository;
    private final ModelMapper modelMapper;
    private final StudentRepository studentRepository;

    /**
     * Creates a new class session with comprehensive validation of business constraints.
     * Validates that the lecturer is assigned to the course and checks for scheduling conflicts
     * before persisting the class session. Generates a unique external identifier for reference.
     *
     * @param request {@link ClassSessionRequest} containing courseId, lecturerId, timeSlot, location, and capacity
     * @return {@link ClassSessionResponse} with populated entity relationships and calculated fields
     * @throws CourseNotFoundException when referenced course does not exist
     * @throws LecturerNotFoundException when referenced lecturer does not exist
     * @throws AssignmentConflictException when lecturer is not assigned to course
     * @throws ScheduleConflictException when lecturer has overlapping class sessions
     * @see #updateClassSession(String, UpdateClassSessionRequest)
     * @see #getClassSessionByExternalId(String)
     */
    @Transactional
    public ClassSessionResponse createClassSession(final ClassSessionRequest request) {
        log.info("Creating class session for course with external ID: {} and lecturer with external ID: {}", request.getCourseId(), request.getLecturerId());

        Course course = courseRepository.findByExternalId(request.getCourseId()).orElseThrow(() -> new CourseNotFoundException(request.getCourseId()));
        Lecturer lecturer = lecturerRepository.findByExternalId(request.getLecturerId()).orElseThrow(() -> new LecturerNotFoundException(request.getLecturerId()));

        if (!lecturer.getCourses().contains(course))
            throw new AssignmentConflictException("Lecturer", lecturer.getExternalId(), "Lecturer is not assigned to this course");

        TimeSlot timeSlot = new TimeSlot(request.getStartTime(), request.getEndTime(), request.getDay());

        boolean hasConflict = classSessionRepository.findByLecturerExternalId(lecturer.getExternalId()).stream()
                .anyMatch(existingClass -> existingClass.getTimeSlot().overlapsWith(timeSlot));

        if (hasConflict) throw new ScheduleConflictException("Lecturer", lecturer.getExternalId(), "Lecturer has scheduling conflict");

        ClassSession classSession = new ClassSession(course, lecturer, timeSlot, request.getLocation(), request.getMaxCapacity());
        classSession.setExternalId(generateClassSessionExternalId());

        ClassSession savedClassSession = classSessionRepository.save(classSession);
        log.info("Class session created successfully with external ID: {}", savedClassSession.getExternalId());

        return mapToResponse(savedClassSession);
    }

    /**
     * Retrieves all class sessions from the system with pagination and sorting capabilities.
     * Supports large datasets by returning paginated results with configurable page size and sorting criteria.
     * Validates pagination parameters to ensure they fall within acceptable ranges.
     *
     * @param pageNo zero-based page index for pagination, must be non-negative
     * @param pageSize number of records per page, must be positive
     * @param sortBy property name to sort the results by, typically field names from ClassSession entity
     * @return {@link Page} of {@link ClassSessionResponse} objects mapped from ClassSession entities
     * @throws IllegalArgumentException if pagination parameters are invalid or out of bounds
     * @see org.springframework.data.domain.Page
     * @see org.springframework.data.domain.Pageable
     */
    public Page<ClassSessionResponse> getAllClassSessions(final int pageNo, final int pageSize, final String sortBy) {
        log.info("Fetching all class sessions with args: pageNo: {}, pageSize: {}, sortBy: {}", pageNo, pageSize, sortBy);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<ClassSession> classSessionPage = classSessionRepository.findAll(pageable);

        return classSessionPage.map(this::mapToResponse);
    }

    /**
     * Fetches a specific class session by its unique external identifier.
     * Provides complete class session details including course information, lecturer assignment,
     * time slot details, and current enrollment statistics.
     *
     * @param externalId unique external identifier of the class session to retrieve
     * @return {@link ClassSessionResponse} mapped from the corresponding ClassSession entity with all relationships
     * @throws ClassSessionNotFoundException if no class session exists with the specified externalId
     * @see #getClassSessionsByCourse(String, int, int)
     * @see #getClassSessionsByLecturer(String, int, int)
     */
    public ClassSessionResponse getClassSessionByExternalId(final String externalId) {
        log.info("Fetching class session with external ID: {}", externalId);
        ClassSession classSession = classSessionRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ClassSessionNotFoundException(externalId));
        return mapToResponse(classSession);
    }

    /**
     * Retrieves all class sessions associated with a specific course with pagination support.
     * Useful for displaying all scheduled sessions for a particular course across different time slots
     * and lecturers. Results are sorted by start time for chronological presentation.
     *
     * @param courseId external ID of the course to filter class sessions
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of class session records per page
     * @return {@link Page} of {@link ClassSessionResponse} objects for the specified course
     * @throws CourseNotFoundException if no course exists with the given external ID
     * @throws IllegalArgumentException if pagination parameters are invalid
     * @see CourseService#getCourseByExternalId(String)
     * @see #getClassSessionsByLecturer(String, int, int)
     */
    public Page<ClassSessionResponse> getClassSessionsByCourse(final String courseId, final int pageNo, final int pageSize) {
        log.info("Fetching class sessions for Course with external ID: {}, and args => pageNo: {}, pageSize: {}", courseId, pageNo, pageSize);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("timeSlot.startTime"));
        Course course = courseRepository.findByExternalId(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));

        Page<ClassSession> classSessionPage = classSessionRepository.findByCourseExternalId(course.getExternalId(), pageable);
        return classSessionPage.map(this::mapToResponse);
    }

    /**
     * Retrieves all class sessions taught by a specific lecturer with pagination support.
     * Provides a complete teaching schedule for a lecturer, sorted chronologically by start time.
     * Essential for lecturer workload management and schedule coordination.
     *
     * @param lecturerId external ID of the lecturer to filter class sessions
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of class session records per page
     * @return {@link Page} of {@link ClassSessionResponse} objects taught by the specified lecturer
     * @throws LecturerNotFoundException if no lecturer exists with the given external ID
     * @throws IllegalArgumentException if pagination parameters are invalid
     * @see LecturerService#getLecturerByExternalId(String)
     * @see #getClassSessionsByCourse(String, int, int)
     */
    public Page<ClassSessionResponse> getClassSessionsByLecturer(final String lecturerId, final int pageNo, final int pageSize) {
        log.info("Fetching class sessions for Lecturer with external ID: {}, and args => pageNo: {}, pageSize: {}", lecturerId, pageNo, pageSize);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("timeSlot.startTime"));
        Lecturer lecturer = lecturerRepository.findByExternalId(lecturerId).orElseThrow(() -> new LecturerNotFoundException(lecturerId));

        Page<ClassSession> classSessionPage = classSessionRepository.findByLecturerExternalId(lecturer.getExternalId(), pageable);
        return classSessionPage.map(this::mapToResponse);
    }

    /**
     * Retrieves all class sessions in which a specific student is currently enrolled.
     * Provides the student's complete class schedule with pagination for large enrollment lists.
     * Results are sorted by start time to display the student's weekly schedule chronologically.
     *
     * @param studentId external ID of the student to filter enrolled class sessions
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of class session records per page
     * @return {@link Page} of {@link ClassSessionResponse} objects the student is enrolled in
     * @throws StudentNotFoundException if no student exists with the given external ID
     * @throws IllegalArgumentException if pagination parameters are invalid
     * @see StudentService#getStudentByExternalId(String)
     * @see StudentService#enrollInClass(String, String)
     */
    public Page<ClassSessionResponse> getClassSessionsByStudent(final String studentId, final int pageNo, final int pageSize) {
        log.info("Fetching class sessions for Student with external ID: {}, and args => pageNo: {}, pageSize: {}", studentId, pageNo, pageSize);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("timeSlot.startTime"));
        Student student = studentRepository.findByExternalId(studentId).orElseThrow(() -> new StudentNotFoundException(studentId));

        Page<ClassSession> classSessionPage = classSessionRepository.findByStudentExternalId(student.getExternalId(), pageable);
        return classSessionPage.map(this::mapToResponse);
    }

    /**
     * Retrieves all class sessions scheduled on a specific day of the week with pagination.
     * Results are sorted by start time to provide a chronological view of the day's classes.
     *
     * @param day {@link DayOfWeek} to filter class sessions, case-insensitive enumeration
     * @param pageNo zero-based page index for pagination
     * @param pageSize number of class session records per page
     * @return {@link Page} of {@link ClassSessionResponse} objects for the specified day
     * @throws IllegalArgumentException if pagination parameters are invalid
     * @see java.time.DayOfWeek
     * @see #getClassSessionsByCourse(String, int, int)
     */
    public Page<ClassSessionResponse> getClassSessionsByDay(final DayOfWeek day, final int pageNo, final int pageSize) {
        log.info("Fetching class sessions for day: {}, and args => pageNo: {}, pageSize: {}", day, pageNo, pageSize);
        validatePagination(pageNo, pageSize);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("timeSlot.startTime"));
        Page<ClassSession> classSessionPage = classSessionRepository.findByDay(day, pageable);
        return classSessionPage.map(this::mapToResponse);
    }

    /**
     * Updates an existing class session's details with comprehensive validation of business constraints.
     * Supports partial updates where only specified fields are modified. Validates scheduling conflicts
     * for enrolled students when time slot changes and ensures capacity constraints are maintained.
     *
     * @param externalId external ID of the class session to update
     * @param request {@link UpdateClassSessionRequest} containing optional fields to update
     * @return {@link ClassSessionResponse} mapped from the updated ClassSession entity
     * @throws ClassSessionNotFoundException if the class session does not exist
     * @throws ScheduleConflictException if updated time slot conflicts with enrolled students' existing schedules
     * @throws ClassCapacityException if the new maxCapacity is less than current enrolled student count
     * @see #createClassSession(ClassSessionRequest)
     * @see #deleteClassSession(String)
     */
    @Transactional
    public ClassSessionResponse updateClassSession(final String externalId, final UpdateClassSessionRequest request) {
        log.info("Updating class session with external ID: {}", externalId);
        ClassSession existingClass = classSessionRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ClassSessionNotFoundException(externalId));

        TimeSlot newTimeSlot;
        if (request.getStartTime() != null || request.getEndTime() != null || request.getDay() != null) {
            LocalTime startTime = request.getStartTime() != null ? request.getStartTime() : existingClass.getTimeSlot().getStartTime();
            LocalTime endTime = request.getEndTime() != null ? request.getEndTime() : existingClass.getTimeSlot().getEndTime();
            DayOfWeek day = request.getDay() != null ? request.getDay() : existingClass.getTimeSlot().getDay();

            newTimeSlot = new TimeSlot(startTime, endTime, day);

            if (!newTimeSlot.equals(existingClass.getTimeSlot())) {
                boolean hasStudentConflict = existingClass.getStudents().stream()
                        .flatMap(student -> student.getClassSessions().stream())
                        .filter(classSession -> !classSession.equals(existingClass))
                        .anyMatch(otherClass -> otherClass.getTimeSlot().overlapsWith(newTimeSlot));

                if (hasStudentConflict) throw new ScheduleConflictException("Time slot change creates conflicts for enrolled students");
            }
        } else {
            newTimeSlot = null;
        }

        if (newTimeSlot != null) existingClass.setTimeSlot(newTimeSlot);
        if (request.getLocation() != null) existingClass.setLocation(request.getLocation());
        if (request.getMaxCapacity() != null) {
            if (request.getMaxCapacity() < existingClass.getEnrolledCount()) {
                throw new ClassCapacityException(existingClass.getExternalId(), existingClass.getMaxCapacity());
            }
            existingClass.setMaxCapacity(request.getMaxCapacity());
        }

        ClassSession updatedClass = classSessionRepository.save(existingClass);
        log.info("Class session with external ID: {} updated successfully", updatedClass.getExternalId());

        return mapToResponse(updatedClass);
    }

    /**
     * Deletes a class session by its external ID with comprehensive referential integrity checks.
     * Ensures that no students are currently enrolled in the class session before allowing deletion
     * to maintain data consistency and prevent orphaned enrollment records.
     *
     * @param externalId external ID of the class session to delete
     * @throws ClassSessionNotFoundException if the class session does not exist
     * @throws DatabaseDeleteConflictException if students are currently enrolled in the class session
     * @see #createClassSession(ClassSessionRequest)
     * @see #updateClassSession(String, UpdateClassSessionRequest)
     */
    @Transactional
    public void deleteClassSession(final String externalId) {
        log.info("Deleting class session with external ID: {}", externalId);

        ClassSession classSession = classSessionRepository.findByExternalId(externalId).orElseThrow(() -> new ClassSessionNotFoundException(externalId));
        if (!classSession.getStudents().isEmpty())
            throw new DatabaseDeleteConflictException("ClassSession", externalId, String.format(
                    "Cannot delete class with ID: %s ,because students are enrolled in the class", classSession.getExternalId()));

        classSessionRepository.delete(classSession);
        log.info("Class session with external ID: {} deleted successfully", externalId);
    }

    /**
     * Checks if a class session has available seats for new student enrollments.
     * Provides a quick availability check without exposing detailed capacity information.
     *
     * @param classSessionId external ID of the class session to check
     * @return true if available seats exist and enrollment is possible, false otherwise
     * @throws ClassSessionNotFoundException if the class session does not exist
     * @see #getAvailableSeats(String)
     * @see StudentService#enrollInClass(String, String)
     */
    public boolean hasAvailableSeats(final String classSessionId) {
        log.info("Checking available seats for Class Session with external ID: {}", classSessionId);

        ClassSession classSession = classSessionRepository.findByExternalId(classSessionId)
                .orElseThrow(() -> new ClassSessionNotFoundException(classSessionId));
        return !classSession.isFull();
    }

    public long getClassSessionCount() {
        return classSessionRepository.count();
    }

    /**
     * Returns the exact number of available seats for a specific class session.
     * Provides detailed capacity information for enrollment management and waitlist processing.
     * Calculated as the difference between maximum capacity and current enrollment count.
     *
     * @param classSessionId external ID of the class session to check
     * @return number of available seats, zero indicates class is at capacity
     * @throws ClassSessionNotFoundException if the class session does not exist
     */
    public int getAvailableSeats(final String classSessionId) {
        log.info("Getting available seats for Class Session with external ID: {}", classSessionId);

        ClassSession classSession = classSessionRepository.findByExternalId(classSessionId).orElseThrow(() -> new ClassSessionNotFoundException(classSessionId));
        return classSession.getAvailableSeats();
    }

    /**
     * Generates a unique external identifier for a new class session following institutional format.
     * Uses a sequential numbering system prefixed with 'CL'.
     * Format: CL<sequence_number> where sequence_number starts from 101 and increments.
     *
     * @return generated unique external ID string for the new class session
     * @see #createClassSession(ClassSessionRequest)
     */
    private String generateClassSessionExternalId() {
        long count = classSessionRepository.count();
        return String.format("CL%d", 100 + count + 1);
    }

    private ClassSessionResponse mapToResponse(final ClassSession classSession) {
        ClassSessionResponse response = modelMapper.map(classSession, ClassSessionResponse.class);
        response.setCourseId(classSession.getCourse().getExternalId());
        response.setCourseName(classSession.getCourse().getName());
        response.setLecturerId(classSession.getLecturer().getExternalId());
        response.setLecturerName(classSession.getLecturer().getName());
        response.setStartTime(classSession.getTimeSlot().getStartTime());
        response.setEndTime(classSession.getTimeSlot().getEndTime());
        response.setDay(classSession.getTimeSlot().getDay());
        response.setEnrolledCount(classSession.getEnrolledCount());
        response.setAvailableSeats(classSession.getAvailableSeats());
        response.setIsFull(classSession.isFull());
        response.setStudentIds(classSession.getStudents().stream()
                .map(student -> student.getExternalId())
                .collect(Collectors.toSet()));
        return response;
    }
}
