package com.demo.universityManagementApp.repository;

import com.demo.universityManagementApp.repository.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByExternalId(final String externalId);
    Optional<Student> findByNameContainingIgnoreCase(final String name);
    Optional<Student> findByNameIgnoreCase(final String name);
    Page<Student> findByDepartmentExternalId(final String departmentId, final Pageable pageable);

    // To avoid Lazy fetching
    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.department WHERE s.externalId = :externalId")
    Optional<Student> findByExternalIdWithDepartment(@Param("externalId") String externalId);

    @Query("SELECT s FROM Student s JOIN s.classSessions cs WHERE cs.externalId = :classSessionId")
    List<Student> findByClassSessionExternalId(@Param("classSessionId") final String classSessionId);

    @Query("SELECT s FROM Student s JOIN s.classSessions cs WHERE cs.externalId = :classSessionId")
    Page<Student> findByClassSessionExternalId(@Param("classSessionId") final String classSessionId, final Pageable pageable);
}
