package com.demo.universityManagementApp.exception.notfound;

import com.demo.universityManagementApp.exception.Io.FileException;

public class FileNotFoundException extends NotFoundException {

    public FileNotFoundException(String path) {
        super(String.format("File at path: (%s) not found", path));
    }

    public FileNotFoundException(String path, String reason) {
        super(String.format("File at path: (%s) not found, Reason: %s", path, reason));
    }
}
