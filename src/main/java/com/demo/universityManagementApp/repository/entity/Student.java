package com.demo.universityManagementApp.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "students")
public class Student extends BaseEntity {

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "enrollment_year", nullable = false)
    private Integer enrollmentYear;

    @ManyToMany
    @JoinTable(
            name = "student_class_sessions",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "class_session_id")
    )
    @Builder.Default
    private Set<ClassSession> classSessions = new HashSet<>();

    public boolean addClassSession(ClassSession classSession) {
        return this.classSessions.add(classSession);
    }

    public boolean removeClassSession(ClassSession classSession) {
        return this.classSessions.remove(classSession);
    }

    public boolean isEnrolledInClass(ClassSession classSession) {
        return this.classSessions.contains(classSession);
    }

    public int getEnrolledClassCount() {
        return this.classSessions.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        Student student = (Student) o;
        return externalId != null && externalId.equals(student.externalId);
    }

    @PrePersist
    @PreUpdate
    private void validateStudent() {
        if (enrollmentYear < 2000 || enrollmentYear > java.time.Year.now().getValue())
            throw new IllegalStateException(String.format("Invalid enrollment year: %d", enrollmentYear));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
