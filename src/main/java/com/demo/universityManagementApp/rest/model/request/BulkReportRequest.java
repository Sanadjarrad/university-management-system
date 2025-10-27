package com.demo.universityManagementApp.rest.model.request;

import com.demo.universityManagementApp.rest.model.ReportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkReportRequest {
    private List<String> studentIds;
    private ReportFormat format;
}