package com.demo.universityManagementApp.exception.Io;

public class FileWriteException extends FileException{
    public FileWriteException(String path, Throwable cause) {
        super(String.format("Failed to write to file at path: (%s)", path), cause);
    }

    public FileWriteException(String path, String reason) {
        super(String.format("Failed to write to file at path: (%s). Reason: %s", path, reason));
    }
}
