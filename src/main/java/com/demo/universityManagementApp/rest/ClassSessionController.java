package com.demo.universityManagementApp.rest;

import com.demo.universityManagementApp.rest.model.request.ClassSessionRequest;
import com.demo.universityManagementApp.rest.model.request.update.UpdateClassSessionRequest;
import com.demo.universityManagementApp.rest.model.response.APIResponse;
import com.demo.universityManagementApp.rest.model.response.ClassSessionResponse;
import com.demo.universityManagementApp.service.ClassSessionService;
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

import java.time.DayOfWeek;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("class-sessions")
@RequiredArgsConstructor
@Tag(name = "Class Session Management", description = "Operations for managing Class Sessions")
public class ClassSessionController {

    private final ClassSessionService classSessionService;

    @Operation(
            summary = "Creates a new class sessions",
            description = "Adds a new class sessions to the system and assigns it to a course an assigns it a lecturer. Requires ADMIN role",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Course created successfully"),
                    @ApiResponse(responseCode = "404", description = "Lecturer not found or does not exist"),
                    @ApiResponse(responseCode = "404", description = "Course not found or does not exist"),
                    @ApiResponse(responseCode = "409", description = "Lecturer is not assigned to the course the class session is in"),
                    @ApiResponse(responseCode = "409", description = "Lecturer has a schedule conflict (another class at the same time)")
            }
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<ClassSessionResponse>> createClassSession(@Valid @RequestBody final ClassSessionRequest request) {

        ClassSessionResponse response = classSessionService.createClassSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse.success("Class session created successfully", response));
    }

    @Operation(
            summary = "Retrieves all Class Sessions",
            description = "Retrieves all Class Sessions from the database (Paginated - sorted by start time, earliest class sessions)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Class Sessions retrieved successfully")
            }
    )
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<ClassSessionResponse>>> getAllClassSessions(@RequestParam(defaultValue = "0") final int page,
                                                                                       @RequestParam(defaultValue = "20") final int size,
                                                                                       @RequestParam(defaultValue = "timeSlot.startTime") final String sortBy) {


        Page<ClassSessionResponse> classSessions = classSessionService.getAllClassSessions(page, size, sortBy);
        return ResponseEntity.ok(APIResponse.success(classSessions));
    }

    @Operation(
            summary = "Retrieves a Class Session by ID",
            description = "Retrieves a Class Session from the database by its external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Class Session retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Class Session not found or does not exist")
            }
    )
    @GetMapping("/{externalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<ClassSessionResponse>> getClassSessionById(@PathVariable final String externalId) {
        ClassSessionResponse classSession = classSessionService.getClassSessionByExternalId(externalId);

        return ResponseEntity.ok(APIResponse.success(classSession));
    }

    @Operation(
            summary = "Retrieves Class Sessions in a Course",
            description = "Retrieves Class Sessions which belong to a course by the courses external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Class Session retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Course not found or does not exist")
            }
    )
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<ClassSessionResponse>>> getClassSessionsByCourse(@PathVariable final String courseId,
                                                                                            @RequestParam(defaultValue = "0") final int page,
                                                                                            @RequestParam(defaultValue = "20") final int size) {

        Page<ClassSessionResponse> classSessions = classSessionService.getClassSessionsByCourse(courseId, page, size);
        return ResponseEntity.ok(APIResponse.success(classSessions));
    }

    @Operation(
            summary = "Retrieves Class Sessions taught by a Lecturer",
            description = "Retrieves Class Sessions taught by a lecturer from the lecturers external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Class Sessions retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Lecturer not found or does not exist")
            }
    )
    @GetMapping("/lecturer/{lecturerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<ClassSessionResponse>>> getClassSessionsByLecturer(@PathVariable final String lecturerId,
                                                                                              @RequestParam(defaultValue = "0") final int page,
                                                                                              @RequestParam(defaultValue = "20") final int size) {

        Page<ClassSessionResponse> classSessions = classSessionService.getClassSessionsByLecturer(lecturerId, page, size);
        return ResponseEntity.ok(APIResponse.success(classSessions));
    }

    @Operation(
            summary = "Retrieves Class Sessions for a given Student",
            description = "Retrieves Class Sessions taken by a student from the students external ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Class Sessions retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Student not found or does not exist")
            }
    )
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<ClassSessionResponse>>> getClassSessionsByStudent(@PathVariable final String studentId,
                                                                                             @RequestParam(defaultValue = "0") final int page,
                                                                                             @RequestParam(defaultValue = "20") final int size) {

        Page<ClassSessionResponse> classSessions = classSessionService.getClassSessionsByStudent(studentId, page, size);
        return ResponseEntity.ok(APIResponse.success(classSessions));
    }

    @Operation(
            summary = "Retrieves Class Sessions for a given Day of the week",
            description = "Retrieves Class Sessions which are scheduled on a given day of the week",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Class Sessions retrieved successfully"),
            }
    )
    @GetMapping("/day/{day}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Page<ClassSessionResponse>>> getClassSessionsByDay(@PathVariable final DayOfWeek day,
                                                                                         @RequestParam(defaultValue = "0") final int page,
                                                                                         @RequestParam(defaultValue = "20") final int size) {

        Page<ClassSessionResponse> classSessions = classSessionService.getClassSessionsByDay(day, page, size);
        return ResponseEntity.ok(APIResponse.success(classSessions));
    }

    @Operation(
            summary = "Updates a Class Session",
            description = "Updates a Class Session by giving a new location or max capacity or time slot",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Class Session updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Class max capacity cannot be less than the enrolled amount"),
                    @ApiResponse(responseCode = "404", description = "Class Session not found or does not exist"),
                    @ApiResponse(responseCode = "409", description = "Time slot changes causes conflicts for enrolled students")
            }
    )
    @PutMapping("/{externalId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<APIResponse<ClassSessionResponse>> updateClassSession(@PathVariable final String externalId,
                                                                                @Valid @RequestBody final UpdateClassSessionRequest request) {


        ClassSessionResponse updatedClassSession = classSessionService.updateClassSession(externalId, request);
        return ResponseEntity.ok(APIResponse.success("Class session updated successfully", updatedClassSession));
    }

    @Operation(
            summary = "Deletes a Class Session",
            description = "Deletes a Class Session from the database. Requires ADMIN role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Class Session deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Class Session not found or does not exist"),
                    @ApiResponse(responseCode = "409", description = "Course cannot be deleted due to relation constraints with Students"),
            }
    )
    @DeleteMapping("/{externalId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Void>> deleteClassSession(@PathVariable final String externalId) {

        classSessionService.deleteClassSession(externalId);
        return ResponseEntity.ok(APIResponse.success("Class session deleted successfully", null));
    }

    @Operation(
            summary = "Retrieves available seats",
            description = "Retrieves available number of seats for a Class Session",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Available seats retrieved successfully"),
            }
    )
    @GetMapping("/{externalId}/available-seats")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Map<String, Object>>> getAvailableSeats(@PathVariable final String externalId) {

        int availableSeats = classSessionService.getAvailableSeats(externalId);
        boolean hasAvailableSeats = classSessionService.hasAvailableSeats(externalId);

        Map<String, Object> seatInfo = Map.of(
                "classSessionId", externalId,
                "availableSeats", availableSeats,
                "hasAvailableSeats", hasAvailableSeats
        );

        return ResponseEntity.ok(APIResponse.success(seatInfo));
    }

    @Operation(
            summary = "Retrieves amount of Class Sessions",
            description = "Retrieves the total amount of Class Sessions available in the database",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Class Session count retrieved successfully"),
            }
    )
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<APIResponse<Map<String, Long>>> getClassSessionCount() {

        long count = classSessionService.getClassSessionCount();
        return ResponseEntity.ok(APIResponse.success(Map.of("count", count)));
    }
}
