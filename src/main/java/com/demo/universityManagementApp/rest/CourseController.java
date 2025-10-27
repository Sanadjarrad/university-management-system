package com.demo.universityManagementApp.rest;

import com.demo.universityManagementApp.rest.model.request.CourseRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateCourseRequest;
import com.demo.universityManagementApp.rest.model.response.APIResponse;
import com.demo.universityManagementApp.rest.model.response.CourseResponse;
import com.demo.universityManagementApp.service.CourseService;
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
@RequestMapping("courses")
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "Operations for managing courses")
public class CourseController {

    private final CourseService courseService;

    @Operation(
            summary = "Creates a new course",
            description = "Adds a new course to the system and assigns it to a department. Requires ADMIN role",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Course created successfully"),
                    @ApiResponse(responseCode = "404", description = "Department not found or does not exist"),
                    @ApiResponse(responseCode = "409", description = "Course already exists")
            }
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<CourseResponse>> createCourse(@Valid @RequestBody final CourseRequest request) {

        CourseResponse response = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.success("Course created successfully", response));
    }

    @Operation(
            summary = "Retrieves all courses",
            description = "Retrieves all courses from the database (Paginated)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Courses retrieved successfully")
            }
    )
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<CourseResponse>>> getAllCourses(@RequestParam(defaultValue = "0") final int page,
                                                                           @RequestParam(defaultValue = "20") final int size,
                                                                           @RequestParam(defaultValue = "name") final String sortBy) {

        Page<CourseResponse> courses = courseService.getAllCourses(page, size, sortBy);
        return ResponseEntity.ok(APIResponse.success(courses));
    }

    @Operation(
            summary = "Retrieves a course by ID",
            description = "Retrieves a course from the database by its external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Course not found or does not exist")
            }
    )
    @GetMapping("/{externalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<CourseResponse>> getCourseById(@PathVariable final String externalId) {

        CourseResponse course = courseService.getCourseByExternalId(externalId);
        return ResponseEntity.ok(APIResponse.success(course));
    }

    @Operation(
            summary = "Retrieves a course by name",
            description = "Retrieves a course from the database by its name (ignoring case)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Course not found or does not exist")
            }
    )
    @GetMapping("/search/{name}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<CourseResponse>> getCourseByName(@PathVariable final String name) {

        CourseResponse course = courseService.getCourseByName(name);
        return ResponseEntity.ok(APIResponse.success(course));
    }

    @Operation(
            summary = "Retrieves courses by department",
            description = "Retrieves all courses in a given department by department external ID (Paginated)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Courses retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Department not found or does not exist")
            }
    )
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<CourseResponse>>> getCoursesByDepartment(@PathVariable final String departmentId,
                                                                                    @RequestParam(defaultValue = "0") final int page,
                                                                                    @RequestParam(defaultValue = "20") final int size) {

        Page<CourseResponse> courses = courseService.getCoursesByDepartment(departmentId, page, size);
        return ResponseEntity.ok(APIResponse.success(courses));
    }

    @Operation(
            summary = "Updates a course",
            description = "Updates a course by giving a new name or course code or assigning to a new department",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Course not found or does not exist")
            }
    )
    @PutMapping("/{externalId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<CourseResponse>> updateCourse(@PathVariable final String externalId,
                                                                    @Valid @RequestBody final UpdateCourseRequest request) {

        CourseResponse updatedCourse = courseService.updateCourse(externalId, request);
        return ResponseEntity.ok(APIResponse.success("Course updated successfully", updatedCourse));
    }

    @Operation(
            summary = "Deletes a course",
            description = "Deletes a course from the database. Requires ADMIN role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Course not found or does not exist"),
                    @ApiResponse(responseCode = "409", description = "Course cannot be deleted due to relation constraints with Class Sessions"),
            }
    )
    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Void>> deleteCourse(@PathVariable final String externalId) {

        courseService.deleteCourse(externalId);
        return ResponseEntity.ok(APIResponse.success("Course deleted successfully", null));
    }
}
