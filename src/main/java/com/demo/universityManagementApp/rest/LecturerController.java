package com.demo.universityManagementApp.rest;

import com.demo.universityManagementApp.rest.model.request.LecturerRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateLecturerRequest;
import com.demo.universityManagementApp.rest.model.response.APIResponse;
import com.demo.universityManagementApp.rest.model.response.LecturerResponse;
import com.demo.universityManagementApp.service.LecturerService;
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
@RequestMapping("lecturers")
@RequiredArgsConstructor
@Tag(name = "Lecturer Management", description = "Operations for managing lecturers")
public class LecturerController {

    private final LecturerService lecturerService;

    @Operation(
            summary = "Creates a new lecturer",
            description = "Adds a new lecturer to the system. Requires ADMIN role",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Lecturer created successfully"),
                    @ApiResponse(responseCode = "409", description = "Lecturer already exists")
            }
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<LecturerResponse>> createLecturer(@Valid @RequestBody final LecturerRequest request) {

        LecturerResponse response = lecturerService.createLecturer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.success("Lecturer created successfully", response));
    }

    @Operation(
            summary = "Retrieves all Lecturers",
            description = "Retrieves all lecturers from the database (Paginated)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lecturers retrieved successfully")
            }
    )
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<LecturerResponse>>> getAllLecturers(@RequestParam(defaultValue = "0") final int page,
                                                                               @RequestParam(defaultValue = "20") final int size,
                                                                               @RequestParam(defaultValue = "name") final String sortBy) {

        Page<LecturerResponse> lecturers = lecturerService.getAllLecturers(page, size, sortBy);
        return ResponseEntity.ok(APIResponse.success(lecturers));
    }

    @Operation(
            summary = "Retrieves a lecturer by ID",
            description = "Retrieves a lecturer from the database by his external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "lecturer retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "lecturer not found or does not exist")
            }
    )
    @GetMapping("/{externalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<LecturerResponse>> getLecturerById(@PathVariable(name = "externalId") final String externalId) {

        LecturerResponse lecturer = lecturerService.getLecturerByExternalId(externalId);
        return ResponseEntity.ok(APIResponse.success(lecturer));
    }

    @Operation(
            summary = "Retrieves a lecturer by name",
            description = "Retrieves a lecturer from the database by his name (ignoring case)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lecturer retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Lecturer not found or does not exist")
            }
    )
    @GetMapping("/search/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<LecturerResponse>> getLecturerByName(@PathVariable(name = "name") final String name) {

        LecturerResponse lecturer = lecturerService.getLecturerByName(name);
        return ResponseEntity.ok(APIResponse.success(lecturer));
    }

    @Operation(
            summary = "Retrieves lecturers by department",
            description = "Retrieves all lecturers in a given department by department external ID (Paginated)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lecturers retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Department not found or does not exist")
            }
    )
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<LecturerResponse>>> getLecturersByDepartment(@PathVariable final String departmentId,
                                                                                        @RequestParam(defaultValue = "0") final int page,
                                                                                        @RequestParam(defaultValue = "20") final int size) {

        Page<LecturerResponse> lecturers = lecturerService.getLecturersByDepartment(departmentId, page, size);
        return ResponseEntity.ok(APIResponse.success(lecturers));
    }

    @Operation(
            summary = "Updates a lecturer",
            description = "Updates a lecturer by giving a new name or phone number or assigning them to a new department",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lecturer updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Lecturer not found or does not exist")
            }
    )
    @PutMapping("/{externalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<LecturerResponse>> updateLecturer(@PathVariable final String externalId,
                                                                        @Valid @RequestBody final UpdateLecturerRequest request) {

        LecturerResponse updatedLecturer = lecturerService.updateLecturer(externalId, request);
        return ResponseEntity.ok(APIResponse.success("Lecturer updated successfully", updatedLecturer));
    }

    @Operation(
            summary = "Deletes a lecturer",
            description = "Deletes a lecturer from the database. Requires ADMIN role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lecturer deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Lecturer not found or does not exist"),
                    @ApiResponse(responseCode = "409", description = "Lecturer cannot be deleted due to relation constraints with Class Sessions")
            }
    )
    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Void>> deleteLecturer(@PathVariable final String externalId) {

        lecturerService.deleteLecturer(externalId);
        return ResponseEntity.ok(APIResponse.success("Lecturer deleted successfully", null));
    }
}
