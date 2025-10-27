package com.demo.universityManagementApp.repository;

import com.demo.universityManagementApp.repository.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByExternalId(final String externalId);
    Optional<Department> findByNameIgnoreCase(final String name);
    boolean existsByExternalId(final String externalId);
    void deleteByExternalId(final String externalId);
}
