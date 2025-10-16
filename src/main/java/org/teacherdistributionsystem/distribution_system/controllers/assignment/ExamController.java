package org.teacherdistributionsystem.distribution_system.controllers.assignment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.exceptions.BadRequestException;
import org.teacherdistributionsystem.distribution_system.models.projections.ExamProjection;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExamController {
    private final ExamService examService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<List<ExamProjection>> getExamsBySessionId(@PathVariable Long sessionId) {
        if (sessionId == null) {
            throw new BadRequestException("Bad Request","SessionId cannot be null");
        }
        return ResponseEntity.ok().body(examService.getExam(sessionId));
    }
    @PatchMapping("/{examId}")
    public ResponseEntity<String> getExamsBySessionId(@PathVariable String examId, @RequestBody Integer requiredSupervisors) {
        if (examId == null) {
            throw new BadRequestException("Bad Request","ExamId cannot be null");
        }
        if (requiredSupervisors == null) {
            throw new BadRequestException("Bad Request","RequiredSupervisors cannot be null");
        }

        examService.updateRequiredSupervisors(examId,requiredSupervisors);
        return ResponseEntity.ok().body("Exam with id  " + examId + " has been updated");
    }
}
