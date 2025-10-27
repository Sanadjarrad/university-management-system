package com.demo.universityManagementApp.repository;

import com.demo.universityManagementApp.repository.entity.ClassSession;
import com.demo.universityManagementApp.repository.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    Optional<ClassSession> findByExternalId(final String externalId);
    List<ClassSession> findByCourseExternalId(final String courseId);
    List<ClassSession> findByLecturerExternalId(final String lecturerId);
    Page<ClassSession> findByCourseExternalId(final String courseId, final Pageable pageable);
    Page<ClassSession> findByLecturerExternalId(final String lecturerId, final Pageable pageable);

    @Query("SELECT cs FROM ClassSession cs JOIN cs.students s WHERE s.externalId = :studentId")
    Page<ClassSession> findByStudentExternalId(@Param("studentId") final String studentId, final Pageable pageable);

    @Query("SELECT cs FROM ClassSession cs WHERE cs.timeSlot.day = :day")
    Page<ClassSession> findByDay(@Param("day") final DayOfWeek day, final Pageable pageable);
}
