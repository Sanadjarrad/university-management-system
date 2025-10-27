package com.demo.universityManagementApp.exception.domain.validation;

public class InvalidPhoneNumberException extends InvalidArgsException {

    public InvalidPhoneNumberException(String phone) {
        super(String.format("Invalid phone number: '%s'. Phone number must be 10 digits and start with 077, 078, or 079.", phone));
    }

    public InvalidPhoneNumberException(String phone, Throwable cause) {
        super(String.format("Invalid phone number: '%s'. Phone number must be 10 digits and start with 077, 078, or 079.", phone), cause);
    }
}
