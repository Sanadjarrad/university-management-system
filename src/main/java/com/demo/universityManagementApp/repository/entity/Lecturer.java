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
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lecturers")
public class Lecturer extends BaseEntity {

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

    @ManyToMany
    @JoinTable(
            name = "lecturer_courses",
            joinColumns = @JoinColumn(name = "lecturer_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @Builder.Default
    private Set<Course> courses = new HashSet<>();

    @OneToMany(mappedBy = "lecturer")
    @Builder.Default
    private Set<ClassSession> classSessions = new HashSet<>();

    public boolean addCourse(Course course) {
        return this.courses.add(course);
    }

    public boolean addClassSession(ClassSession classSession) {
        return this.classSessions.add(classSession);
    }

    public boolean removeCourse(Course course) {
        return this.courses.remove(course);
    }

    public boolean removeClassSession(ClassSession classSession) {
        return this.classSessions.remove(classSession);
    }

    public boolean teachesCourse(Course course) {
        return this.courses.contains(course);
    }

    public int getCourseCount() {
        return this.courses.size();
    }

    public int getClassCount() {
        return this.classSessions.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lecturer)) return false;
        Lecturer lecturer = (Lecturer) o;
        return externalId != null && externalId.equals(lecturer.externalId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
