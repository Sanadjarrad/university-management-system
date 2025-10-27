package com.demo.universityManagementApp.exception.domain.validation;

public class InvalidNameException extends InvalidArgsException {

    public InvalidNameException(String name) {
        super(String.format("Invalid name: '%s'. Name must contain only letters and spaces, with a length between 2 and 50 characters.", name));
    }

    public InvalidNameException(String name, Throwable cause) {
        super(String.format("Invalid name: '%s'. Name must contain only letters and spaces, with a length between 2 and 50 characters.", name), cause);
    }
}
