package com.demo.universityManagementApp.repository.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "courses")
public class Course extends BaseEntity {

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToMany(mappedBy = "courses")
    private Set<Lecturer> lecturers = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private Set<ClassSession> classSessions = new HashSet<>();

    public Course(String name, String code, Department department) {
        this.name = name;
        this.code = code;
        this.department = department;
    }

    public Course(String externalId, String name, String code, Department department) {
        this.externalId = externalId;
        this.name = name;
        this.code = code;
        this.department = department;
    }

    public boolean addLecturer(Lecturer lecturer) {
        return this.lecturers.add(lecturer);
    }

    public boolean addClassSession(ClassSession classSession) {
        return this.classSessions.add(classSession);
    }

    public boolean removeLecturer(Lecturer lecturer) {
        return this.lecturers.remove(lecturer);
    }

    public boolean removeClassSession(ClassSession classSession) {
        return this.classSessions.remove(classSession);
    }

    public boolean hasLecturer(Lecturer lecturer) {
        return this.lecturers.contains(lecturer);
    }

    public int getLecturerCount() {
        return this.lecturers.size();
    }

    public int getClassCount() {
        return this.classSessions.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course course = (Course) o;
        return externalId != null && externalId.equals(course.externalId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
