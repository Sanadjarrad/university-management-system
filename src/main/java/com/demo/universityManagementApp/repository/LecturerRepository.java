package com.demo.universityManagementApp.repository;

import com.demo.universityManagementApp.repository.entity.Lecturer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    Optional<Lecturer> findByExternalId(final String externalId);
    Optional<Lecturer> findByNameIgnoreCase(final String name);
    Page<Lecturer> findByDepartmentExternalId(final String departmentId, final Pageable pageable);
    boolean existsByExternalId(final String externalId);
    void deleteByExternalId(final String externalId);
}
