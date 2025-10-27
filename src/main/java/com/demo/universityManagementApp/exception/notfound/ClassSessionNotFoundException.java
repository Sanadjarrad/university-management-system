package com.demo.universityManagementApp.exception.notfound;

import com.demo.universityManagementApp.exception.domain.classSession.ClassSessionException;

public class ClassSessionNotFoundException extends NotFoundException {
    public ClassSessionNotFoundException(String classSessionId) {
        super(String.format("Class session with ID (%s) not found.", classSessionId));
    }

    public ClassSessionNotFoundException(String classSessionId, Throwable cause) {
        super(String.format("Class session with ID (%s) not found.", classSessionId), cause);
    }
}
