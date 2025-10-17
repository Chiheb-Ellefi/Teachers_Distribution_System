package org.teacherdistributionsystem.distribution_system.controllers.teachers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherExamAssignmentDto;
import org.teacherdistributionsystem.distribution_system.dtos.teacher.TeacherDto;
import org.teacherdistributionsystem.distribution_system.exceptions.custom.BadRequestException;
import org.teacherdistributionsystem.distribution_system.exceptions.custom.InvalidSearchParameterException;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilitiesProjection;
import org.teacherdistributionsystem.distribution_system.models.responses.PageResponse;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.TeacherAssignmentsResponse;
import org.teacherdistributionsystem.distribution_system.models.responses.teacher.TeacherResponse;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentPersistenceService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherQuotaService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherUnavailabilityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TeacherController {

    private final TeacherUnavailabilityService teacherUnavailabilityService;
    private final TeacherService teacherService;
    private final TeacherQuotaService teacherQuotaService;
    private final AssignmentPersistenceService assignmentPersistenceService;

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

    @PatchMapping("/{teacherId}/quota")
    public ResponseEntity<String> updateTeacherQuotaById(@PathVariable Long teacherId, @RequestBody Integer quota) {
        if (teacherId == null) {
            throw new BadRequestException("Bad Request", "teacherId cannot be null");
        }
        if (quota == null) {
            throw new BadRequestException("Bad Request", "quota cannot be null");
        }
        teacherQuotaService.updateTeacherQuota(teacherId,quota);
        return  ResponseEntity.ok().body("Quota updated successfully for teacher with id " + teacherId);
    }



    @GetMapping("/{teacherId}/workload/{sessionId}")
    public ResponseEntity<TeacherAssignmentsResponse> getTeacherWorkloadById(@PathVariable Long teacherId,@PathVariable Long sessionId) {
        if (teacherId == null) {
            throw new BadRequestException("Bad Request", "teacherId cannot be null");
        }
       TeacherDto teacher= teacherService.getTeacherDetails(teacherId);
       List<TeacherExamAssignmentDto> assignments= assignmentPersistenceService.getTeacherAssignments(teacherId,sessionId,true);
        TeacherAssignmentsResponse response=TeacherAssignmentsResponse.builder()
                .teacherName(teacher.getNom()+ " "+teacher.getPrenom())
                .email(teacher.getEmail())
                .supervisionStatus(teacher.getParticipeSurveillance())
                .assignments(assignments)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<TeacherDto>> getTeacherByName(
            @RequestParam(required = false) String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new InvalidSearchParameterException("Request parameter invalid","Search name cannot be null or empty");
        }
        List<TeacherDto> teachers = teacherService.containsName(name.trim());
        return ResponseEntity.ok(teachers);
    }

}
