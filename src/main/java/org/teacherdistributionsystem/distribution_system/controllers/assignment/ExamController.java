package org.teacherdistributionsystem.distribution_system.controllers.assignment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.exceptions.BadRequestException;
import org.teacherdistributionsystem.distribution_system.models.projections.ExamForAssignmentProjection;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExamController {
    private final ExamService examService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<List<ExamForAssignmentProjection>> getExamsBySessionId(@PathVariable Long sessionId) {
        if (sessionId == null) {
            throw new BadRequestException("Bad Request","SessionId cannot be null");
        }
        return ResponseEntity.ok().body(examService.getExamsForAssignment(sessionId));
    }
}
