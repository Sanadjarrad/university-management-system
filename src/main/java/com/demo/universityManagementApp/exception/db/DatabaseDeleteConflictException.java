package com.demo.universityManagementApp.exception.db;

import lombok.Getter;

@Getter
public class DatabaseDeleteConflictException extends DatabaseException{
    private final String entityName;
    private final String entityId;
    private final String conflictCause;

    public DatabaseDeleteConflictException(String entityName, String entityId, String conflictCause) {
        super(String.format("Failed to delete %s (ID: %s): %s", entityName, entityId, conflictCause));
        this.entityName = entityName;
        this.entityId = entityId;
        this.conflictCause = conflictCause;
    }

    public DatabaseDeleteConflictException(String entityName, String entityId, String conflictCause, Throwable cause) {
        super(String.format("Failed to delete %s (ID: %s): %s", entityName, entityId, conflictCause), cause);
        this.entityName = entityName;
        this.entityId = entityId;
        this.conflictCause = conflictCause;
    }

}
