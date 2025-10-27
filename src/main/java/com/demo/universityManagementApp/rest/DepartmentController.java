package com.demo.universityManagementApp.rest;

import com.demo.universityManagementApp.rest.model.request.DepartmentRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateDepartmentRequest;
import com.demo.universityManagementApp.rest.model.response.APIResponse;
import com.demo.universityManagementApp.rest.model.response.DepartmentResponse;
import com.demo.universityManagementApp.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("departments")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "Operations for managing departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(
            summary = "Creates a new department",
            description = "Adds a new department to the system. Requires ADMIN role",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Department created successfully"),
                    @ApiResponse(responseCode = "409", description = "Department already exists")
            }
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<DepartmentResponse>> createDepartment(@Valid @RequestBody final DepartmentRequest request) {

        DepartmentResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.success("Department created successfully", response));
    }

    @Operation(
            summary = "Retrieves all departments",
            description = "Retrieves all departments from the database (Paginated)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Departments retrieved successfully")
            }
    )
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<DepartmentResponse>>> getAllDepartments(@RequestParam(defaultValue = "0") final int page,
                                                                                   @RequestParam(defaultValue = "20") final int size,
                                                                                   @RequestParam(defaultValue = "name") final String sortBy) {

        Page<DepartmentResponse> departments = departmentService.getAllDepartments(page, size, sortBy);
        return ResponseEntity.ok(APIResponse.success(departments));
    }

    @Operation(
            summary = "Retrieves a department by ID",
            description = "Retrieves a department from the database by its external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Department retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Department not found or does not exist")
            }
    )
    @GetMapping("/{externalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<DepartmentResponse>> getDepartmentById(@PathVariable final String externalId) {

        DepartmentResponse department = departmentService.getDepartmentByExternalId(externalId);
        return ResponseEntity.ok(APIResponse.success(department));
    }

    @Operation(
            summary = "Retrieves a department by name",
            description = "Retrieves a department from the database by its name (ignoring case)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Department retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Department not found or does not exist")
            }
    )
    @GetMapping("/search/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<DepartmentResponse>> getDepartmentByName(@PathVariable final String name) {

        DepartmentResponse department = departmentService.getDepartmentByName(name);
        return ResponseEntity.ok(APIResponse.success(department));
    }

    @Operation(
            summary = "Updates a department",
            description = "Updates a department by giving a new name or department code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Department updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Department not found or does not exist")
            }
    )
    @PutMapping("/{externalId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<DepartmentResponse>> updateDepartment(@PathVariable final String externalId,
                                                                            @Valid @RequestBody final UpdateDepartmentRequest request) {

        DepartmentResponse updatedDepartment = departmentService.updateDepartment(externalId, request);
        return ResponseEntity.ok(APIResponse.success("Department updated successfully", updatedDepartment));
    }

    @Operation(
            summary = "Deletes a department",
            description = "Deletes a department from the database. Requires ADMIN role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Department deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Department not found or does not exist"),
                    @ApiResponse(responseCode = "409", description = "Department cannot be deleted due to relation constraints with Lecturers"),
                    @ApiResponse(responseCode = "409", description = "Department cannot be deleted due to relation constraints with Students"),
                    @ApiResponse(responseCode = "409", description = "Department cannot be deleted due to relation constraints with Courses")
            }
    )
    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Void>> deleteDepartment(@PathVariable final String externalId) {
        departmentService.deleteDepartment(externalId);
        return ResponseEntity.ok(APIResponse.success("Department deleted successfully", null));
    }
}
