package org.teacherdistributionsystem.distribution_system.controllers.teachers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.exceptions.BadRequestException;

import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilitiesProjection;
import org.teacherdistributionsystem.distribution_system.models.responses.PageResponse;

import org.teacherdistributionsystem.distribution_system.models.responses.TeacherResponse;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherService;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherUnavailabilityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TeacherController {

    private final TeacherUnavailabilityService teacherUnavailabilityService;
    private final TeacherService teacherService;

    @GetMapping
    public ResponseEntity<PageResponse<TeacherResponse>> getAllTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<TeacherResponse> response = teacherService.getAllTeachers(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<List<TeacherUnavailabilitiesProjection>> getTeachersUnavailabilityList(@PathVariable Long sessionId) {
        if (sessionId == null) {
            throw new BadRequestException("Bad Request", "sessionId cannot be null");
        }
        return ResponseEntity.ok().body( teacherUnavailabilityService.getTeacherUnavailabilityListBySessionId(sessionId));
    }

}
