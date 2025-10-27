package com.demo.universityManagementApp.rest.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkReportResponse {
    private Integer totalRequests;
    private Integer successfulGenerations;
    private Integer failedGenerations;
    private OffsetDateTime generatedAt;
    private List<String> fileNames;
}
