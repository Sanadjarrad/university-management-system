package com.demo.universityManagementApp.repository.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "departments")
public class Department extends BaseEntity {

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private Set<Student> students = new HashSet<>();

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private Set<Lecturer> lecturers = new HashSet<>();

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private Set<Course> courses = new HashSet<>();

    public Department(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public Department(String externalId, String name, String code) {
        this.externalId = externalId;
        this.name = name;
        this.code = code;
    }

    public boolean addStudent(Student student) {
        return this.students.add(student);
    }

    public boolean addLecturer(Lecturer lecturer) {
        return this.lecturers.add(lecturer);
    }

    public boolean addCourse(Course course) {
        return this.courses.add(course);
    }

    public boolean removeStudent(Student student) {
        return this.students.remove(student);
    }

    public boolean removeLecturer(Lecturer lecturer) {
        return this.lecturers.remove(lecturer);
    }

    public boolean removeCourse(Course course) {
        return this.courses.remove(course);
    }

    public int getStudentCount() {
        return this.students.size();
    }

    public int getLecturerCount() {
        return this.lecturers.size();
    }

    public int getCourseCount() {
        return this.courses.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department)) return false;
        Department that = (Department) o;
        return externalId != null && externalId.equals(that.externalId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
