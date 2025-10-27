package com.demo.universityManagementApp.exception.domain.classSession;

public class ClassCapacityException extends ClassSessionException{
    public ClassCapacityException(String classSessionId, int maxCapacity) {
        super(String.format("Cannot enroll: class session '%s' has reached its maximum capacity of %d students.", classSessionId, maxCapacity));
    }

    public ClassCapacityException(String classSessionId, int maxCapacity, Throwable cause) {
        super(String.format("Cannot enroll: class session '%s' has reached its maximum capacity of %d students.", classSessionId, maxCapacity), cause);
    }
}
