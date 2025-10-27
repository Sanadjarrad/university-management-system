package com.demo.universityManagementApp.repository.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "class_sessions")
public class ClassSession extends BaseEntity {

    @Column(name = "external_id", unique = true)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startTime", column = @Column(name = "start_time")),
            @AttributeOverride(name = "endTime", column = @Column(name = "end_time")),
            @AttributeOverride(name = "day", column = @Column(name = "day_of_week"))
    })
    private TimeSlot timeSlot;

    @Column(nullable = false)
    private String location;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @ManyToMany(mappedBy = "classSessions")
    private Set<Student> students = new HashSet<>();

    public ClassSession(Course course, Lecturer lecturer, TimeSlot timeSlot, String location, int maxCapacity) {
        this.course = course;
        this.lecturer = lecturer;
        this.timeSlot = timeSlot;
        this.location = location;
        this.maxCapacity = maxCapacity;
    }

    public ClassSession(String externalId, Course course, Lecturer lecturer, TimeSlot timeSlot, String location, int maxCapacity) {
        this.externalId = externalId;
        this.course = course;
        this.lecturer = lecturer;
        this.timeSlot = timeSlot;
        this.location = location;
        this.maxCapacity = maxCapacity;
    }

    public int getEnrolledCount() {
        return this.students.size();
    }

    public boolean isFull() {
        return this.students.size() >= this.maxCapacity;
    }

    public int getAvailableSeats() {
        return this.maxCapacity - this.students.size();
    }

    public boolean addStudent(Student student) {
        if (isFull() || this.students.contains(student)) return false;
        return this.students.add(student);
    }

    public boolean removeStudent(Student student) {
        return this.students.remove(student);
    }

    public boolean hasStudent(Student student) {
        return this.students.contains(student);
    }

    public boolean hasTimeConflictWith(ClassSession other) {
        return this.timeSlot.overlapsWith(other.getTimeSlot());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassSession)) return false;
        ClassSession that = (ClassSession) o;
        return externalId != null && externalId.equals(that.externalId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
