package com.demo.universityManagementApp.exception.domain.validation;

public class InvalidEmailException extends InvalidArgsException{
    public InvalidEmailException(String email) {
        super(String.format("Invalid email address: '%s'. Expected format: firstInitial + middleInitial + lastName + lastTwoDigitsOfYear + @DigitinaryUniversity.com for students, or firstName.lastName@DigitinaryUniversity.com for lecturers.", email));
    }

    public InvalidEmailException(String email, Throwable cause) {
        super(String.format("Invalid email address: '%s'. Expected format: firstInitial + middleInitial + lastName + lastTwoDigitsOfYear + @DigitinaryUniversity.com for students, or firstName.lastName@DigitinaryUniversity.com for lecturers.", email), cause);
    }
}
