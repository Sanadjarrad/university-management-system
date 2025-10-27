package com.demo.universityManagementApp.exception.notfound;

public class CourseNotFoundException extends NotFoundException {
    public CourseNotFoundException(String courseId) {
        super(String.format("Course with ID: (%s) not found.", courseId));
    }

    public CourseNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
