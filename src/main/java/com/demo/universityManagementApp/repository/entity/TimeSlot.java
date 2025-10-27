package com.demo.universityManagementApp.repository.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class TimeSlot {

    private LocalTime startTime;

    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private DayOfWeek day;

    public TimeSlot(LocalTime startTime, LocalTime endTime, DayOfWeek day) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.day = day;

        if (endTime != null && startTime != null && !endTime.isAfter(startTime)) throw new IllegalArgumentException("End time must be after start time");
    }

    public boolean overlapsWith(TimeSlot other) {
        return this.day == other.day && this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return Objects.equals(startTime, timeSlot.startTime) && Objects.equals(endTime, timeSlot.endTime) && day == timeSlot.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, day);
    }
}
