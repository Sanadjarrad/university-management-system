package com.demo.universityManagementApp.rest;

import com.demo.universityManagementApp.rest.model.request.EnrollmentRequest;
import com.demo.universityManagementApp.rest.model.request.StudentRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateStudentRequest;
import com.demo.universityManagementApp.rest.model.response.APIResponse;
import com.demo.universityManagementApp.rest.model.response.EnrollmentResponse;
import com.demo.universityManagementApp.rest.model.response.StudentResponse;
import com.demo.universityManagementApp.service.StudentService;
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

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("students")
@RequiredArgsConstructor
@Tag(name = "Student Management", description = "Operations for managing students")
public class StudentController {

    private final StudentService studentService;

    @Operation(
            summary = "Create a new Student",
            description = "Adds a new student to the system. Requires ADMIN role",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Student created successfully"),
                    @ApiResponse(responseCode = "409", description = "Student already exists")
            }
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<StudentResponse>> createStudent(@Valid @RequestBody final StudentRequest request) {

        StudentResponse response = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.success("Student created successfully", response));
    }

    @Operation(
            summary = "Retrieves all Students",
            description = "Retrieves all students from the database (Paginated)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Students retrieved successfully")
            }
    )
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<StudentResponse>>> getAllStudents(@RequestParam(defaultValue = "0") final int page,
                                                                             @RequestParam(defaultValue = "20") final int size,
                                                                             @RequestParam(defaultValue = "name") final String sortBy) {

        Page<StudentResponse> students = studentService.getAllStudents(page, size, sortBy);
        return ResponseEntity.ok(APIResponse.success(students));
    }

    @Operation(
            summary = "Retrieves a student by ID",
            description = "Retrieves a student from the database by his external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Student retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Student not found or does not exist")
            }
    )
    @GetMapping("/{externalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<StudentResponse>> getStudentById(@PathVariable final String externalId) {

        StudentResponse student = studentService.getStudentByExternalId(externalId);
        return ResponseEntity.ok(APIResponse.success(student));
    }

    @Operation(
            summary = "Retrieves a student by name",
            description = "Retrieves a student from the database by his name (ignoring case)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Student retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Student not found or does not exist")
            }
    )
    @GetMapping("/search/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<StudentResponse>> getStudentByName(@PathVariable final String name) {

        StudentResponse student = studentService.getStudentByName(name);
        return ResponseEntity.ok(APIResponse.success(student));
    }

    @Operation(
            summary = "Retrieves a student by name containing",
            description = "Retrieves a student from the database by matching a char sequence to his name (ignoring case)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Student retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Student not found or does not exist"),
                    @ApiResponse(responseCode = "500", description = "Multiple Students found with matching char sequence")
            }
    )
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<StudentResponse>> getStudentByNameContaining(@RequestParam final String name) {

        StudentResponse student = studentService.getStudentByNameContaining(name);
        return ResponseEntity.ok(APIResponse.success(student));
    }


    @Operation(
            summary = "Retrieves students by department",
            description = "Retrieves all students in a given department by department external ID (Paginated)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Department not found or does not exist")
            }
    )
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<StudentResponse>>> getStudentsByDepartment(@PathVariable final String departmentId,
                                                                                      @RequestParam(defaultValue = "0") final int page,
                                                                                      @RequestParam(defaultValue = "20") final int size) {

        Page<StudentResponse> students = studentService.getStudentsByDepartment(departmentId, page, size);
        return ResponseEntity.ok(APIResponse.success(students));
    }

    @Operation(
            summary = "Retrieves students by Class Session",
            description = "Retrieves all students in a given Class Session by Class Session external ID (Paginated)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Class Session not found or does not exist")
            }
    )
    @GetMapping("/classSession/{classSessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<StudentResponse>>> getStudentsByClassSession(@PathVariable final String classSessionId,
                                                                                        @RequestParam(defaultValue = "0") final int page,
                                                                                        @RequestParam(defaultValue = "20") final int size) {

        Page<StudentResponse> students = studentService.getStudentsByClassSession(classSessionId, page, size);
        return ResponseEntity.ok(APIResponse.success(students));
    }

    @Operation(
            summary = "Updates a student",
            description = "Updates a student by giving a new name or phone number or changing the enrollment year or assigning to a new department",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Student updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Student not found or does not exist")
            }
    )
    @PutMapping("/{externalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<StudentResponse>> updateStudent(@PathVariable final String externalId,
                                                                      @Valid @RequestBody final UpdateStudentRequest request) {

        StudentResponse updatedStudent = studentService.updateStudent(externalId, request);
        return ResponseEntity.ok(APIResponse.success("Student updated successfully", updatedStudent));
    }

    @Operation(
            summary = "Deletes a student",
            description = "Deletes a student from the database. Requires ADMIN role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Student deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Student not found or does not exist")
            }
    )
    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Void>> deleteStudent(@PathVariable final String externalId) {

        studentService.deleteStudent(externalId);
        return ResponseEntity.ok(APIResponse.success("Student deleted successfully", null));
    }

    @Operation(
            summary = "Enrolls a student in a class",
            description = "Enrolls a student in a given class, and checks if any scheduling conflicts occur",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Student Enrolled successfully"),
                    @ApiResponse(responseCode = "400", description = "Class does not have available seats"),
                    @ApiResponse(responseCode = "404", description = "Student not found or does not exist"),
                    @ApiResponse(responseCode = "404", description = "Class Session not found or does not exist"),
                    @ApiResponse(responseCode = "409", description = "Student already has a different class in the same time period")
            }
    )
    @PostMapping("/enroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<EnrollmentResponse>> enrollInClass(@Valid @RequestBody final EnrollmentRequest request) {

        EnrollmentResponse response = studentService.enrollInClass(request.getStudentId(), request.getClassSessionId());
        return ResponseEntity.ok(APIResponse.success(response.getMessage(), response));
    }

    @Operation(
            summary = "Retrieves students count",
            description = "Retrieves the count of all students from the database",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Students count retrieved successfully")
            }
    )
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Map<String, Long>>> getStudentCount() {

        long count = studentService.getStudentCount();
        return ResponseEntity.ok(APIResponse.success(Map.of("count", count)));
    }
}
