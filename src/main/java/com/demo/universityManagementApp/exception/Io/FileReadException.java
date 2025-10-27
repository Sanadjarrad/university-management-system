package com.demo.universityManagementApp.exception.Io;

public class FileReadException extends FileException{
    public FileReadException(String path, Throwable cause) {
        super(String.format("Failed to read file at path: (%s)", path), cause);
    }

    public FileReadException(String path, String reason) {
        super(String.format("Failed to read file at path: (%s). Reason: %s", path, reason));
    }
}
