package com.demo.universityManagementApp.rest.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private String content;
    private String format;
    private String entityType;
    private String entityId;
    private String entityName;
    private String fileName;
    private long fileSize;
}
