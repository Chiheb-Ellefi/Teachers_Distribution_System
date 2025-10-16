package org.teacherdistributionsystem.distribution_system.controllers.teachers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.dtos.teacher.TeacherUnavailabilityDto;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherUnavailability;
import org.teacherdistributionsystem.distribution_system.exceptions.BadRequestException;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilitiesProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilityProjection;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherUnavailabilityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TeacherController {

    private final TeacherUnavailabilityService teacherUnavailabilityService;
    @GetMapping("/{sessionId}")
    public ResponseEntity<List<TeacherUnavailabilitiesProjection>> getTeachersUnavailabilityList(@PathVariable Long sessionId) {
        if (sessionId == null) {
            throw new BadRequestException("Bad Request", "sessionId cannot be null");
        }
        return ResponseEntity.ok().body( teacherUnavailabilityService.getTeacherUnavailabilityListBySessionId(sessionId));
    }
}
