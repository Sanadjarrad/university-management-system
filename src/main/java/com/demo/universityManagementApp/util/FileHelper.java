package com.demo.universityManagementApp.util;

import com.demo.universityManagementApp.exception.Io.FileReadException;
import com.demo.universityManagementApp.exception.Io.FileWriteException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static com.demo.universityManagementApp.util.Helper.Validate.validateNotNull;

public final class FileHelper {

    private FileHelper() {}

    public static void writeFile(final Path path, final String content) {
        validateNotNull(path, "filePath");
        validateNotNull(content, "fileContent");

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            throw new FileWriteException(path.toString(), e.getCause());
        }
    }
}
