package org.teacherdistributionsystem.distribution_system.controllers.assignment;



import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.teacherdistributionsystem.distribution_system.entities.assignment.AssignmentSession;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;
import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;
import org.teacherdistributionsystem.distribution_system.exceptions.BadRequestException;
import org.teacherdistributionsystem.distribution_system.models.responses.AssignmentResponseModel;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentAlgorithmService;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentPersistenceService;
import org.teacherdistributionsystem.distribution_system.utils.JsonFileWriter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssignmentController {

    private final AssignmentAlgorithmService assignmentAlgorithmService;
    private final AssignmentPersistenceService persistenceService;
    private final JsonFileWriter jsonFileWriter;
    private final AssignmentPersistenceService assignmentPersistenceService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<List<TeacherExamAssignment>> getAssignmentsForSession(@PathVariable Long sessionId) {
        if(sessionId == null) {
            throw new BadRequestException("Bad Request", "Session id is required");
        }
        List<TeacherExamAssignment> result=   assignmentPersistenceService.getAssignmentsForSession(sessionId);
        return ResponseEntity.ok(result);
    }
    @GetMapping("/teacher/{sessionId}/{teacherId}")
    public ResponseEntity<List<TeacherExamAssignment>> getTeacherAssignments(@PathVariable Long sessionId, @PathVariable Long teacherId){
        if(sessionId == null ) {
            throw new BadRequestException("Bad Request", "Session id is required");
        }
        if(teacherId == null) {
            throw new BadRequestException("Bad Request", "Teacher id is required");
        }
        return ResponseEntity.ok().body(assignmentPersistenceService.getTeacherAssignments(teacherId,sessionId));
    }

    @PostMapping("/execute/{sessionId}")
    public ResponseEntity<Object> executeAssignment(@PathVariable Long sessionId) {

        try {
            AssignmentResponseModel response = assignmentAlgorithmService.executeAssignment(sessionId);
            HttpStatus httpStatus = switch (response.getStatus()) {
                case SUCCESS -> HttpStatus.OK;
                case INFEASIBLE -> HttpStatus.OK; // still ok but there is no solution
                case TIMEOUT -> HttpStatus.PARTIAL_CONTENT;
                case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            persistenceService.saveAssignmentResults(response);
            jsonFileWriter.writeDataToJsonFile(response.getTeacherWorkloads());
            response.setExamAssignments(null);
            response.setTeacherWorkloads(null);
            return ResponseEntity.status(httpStatus).body(response);

        } catch (Exception e) {
            AssignmentResponseModel errorResponse = AssignmentResponseModel.builder()
                    .status(AssignmentStatus.ERROR)
                    .message("Failed to execute assignment: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Boolean> checkAssignmentStatus(@PathVariable Long sessionId) {

        boolean exists = persistenceService.hasAssignments(sessionId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/exam/{sessionId}/{examId}")
    public ResponseEntity<List<TeacherExamAssignment>> getExamAssignments(@PathVariable Long sessionId, @PathVariable String examId){
        if(sessionId == null ) {
            throw new BadRequestException("Bad Request", "Session id is required");
        }
        if(examId == null) {
            throw new BadRequestException("Bad Request", "Exam id is required");
        }
        return ResponseEntity.ok().body(assignmentPersistenceService.getExamAssignments(examId,sessionId));
    }
    @GetMapping("/metadata/{sessionId}")
    public ResponseEntity<AssignmentSession> getSessionMetadata(@PathVariable Long sessionId){
        if(sessionId == null) {
            throw new BadRequestException("Bad Request", "Session id is required");
        }
        return ResponseEntity.ok().body(assignmentPersistenceService.getSessionMetadata(sessionId));
    }
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<String> deleteAssignment(@PathVariable Long sessionId){
        if(sessionId == null) {
            throw new BadRequestException("Bad Request", "Session id is required");
        }
        assignmentPersistenceService.deleteAssignments(sessionId);
        return ResponseEntity.accepted().body("Assignments deleted for session: " + sessionId);
    }

}