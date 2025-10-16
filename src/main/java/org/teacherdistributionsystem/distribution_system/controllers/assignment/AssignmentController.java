package org.teacherdistributionsystem.distribution_system.controllers.assignment;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import org.teacherdistributionsystem.distribution_system.entities.assignment.AssignmentSession;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;
import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;
import org.teacherdistributionsystem.distribution_system.exceptions.BadRequestException;
import org.teacherdistributionsystem.distribution_system.models.responses.AssignmentResponseModel;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentAlgorithmService;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentPersistenceService;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamService;
import org.teacherdistributionsystem.distribution_system.services.teachers.GradeService;
import org.teacherdistributionsystem.distribution_system.services.teachers.QuotaPerGradeService;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherQuotaService;
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
    private final GradeService gradeService;
    private final QuotaPerGradeService quotaPerGradeService;
    private final TeacherQuotaService teacherQuotaService;
    private final ExamService examService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<List<TeacherExamAssignment>> getAssignmentsForSession(@PathVariable Long sessionId) {
        if(sessionId == null) {
            throw new BadRequestException("Bad Request", "Session id is required");
        }
        List<TeacherExamAssignment> result = assignmentPersistenceService.getAssignmentsForSession(sessionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/teacher/{sessionId}/{teacherId}")
    public ResponseEntity<List<TeacherExamAssignment>> getTeacherAssignments(@PathVariable Long sessionId, @PathVariable Long teacherId){
        if(sessionId == null) {
            throw new BadRequestException("Bad Request", "Session id is required");
        }
        if(teacherId == null) {
            throw new BadRequestException("Bad Request", "Teacher id is required");
        }
        return ResponseEntity.ok().body(assignmentPersistenceService.getTeacherAssignments(teacherId, sessionId));
    }

    @PostMapping("/execute/{sessionId}")
    public DeferredResult<ResponseEntity<Object>> executeAssignment(@PathVariable Long sessionId) {

        DeferredResult<ResponseEntity<Object>> deferredResult = new DeferredResult<>(15000L);


        assignmentAlgorithmService.executeAssignment(sessionId)
                .whenComplete((response, exception) -> {
                    try {
                        if (exception != null) {
                            handleError(deferredResult, exception);
                        } else {
                            handleSuccess(deferredResult, response);
                        }
                    } finally {

                        cleanUp();
                    }
                });

        // Handle timeout scenario
        deferredResult.onTimeout(() -> {
            AssignmentResponseModel timeoutResponse = AssignmentResponseModel.builder()
                    .status(AssignmentStatus.TIMEOUT)
                    .message("Request timed out after 15 seconds")
                    .build();
            deferredResult.setResult(
                    ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(timeoutResponse)
            );
            cleanUp();
        });

        return deferredResult;
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Boolean> checkAssignmentStatus(@PathVariable Long sessionId) {
        boolean exists = persistenceService.hasAssignments(sessionId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/exam/{sessionId}/{examId}")
    public ResponseEntity<List<TeacherExamAssignment>> getExamAssignments(@PathVariable Long sessionId, @PathVariable String examId){
        if(sessionId == null) {
            throw new BadRequestException("Bad Request", "Session id is required");
        }
        if(examId == null) {
            throw new BadRequestException("Bad Request", "Exam id is required");
        }
        return ResponseEntity.ok().body(assignmentPersistenceService.getExamAssignments(examId, sessionId));
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


    private void handleSuccess(DeferredResult<ResponseEntity<Object>> deferredResult,
                               AssignmentResponseModel response) {
        try {

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


            deferredResult.setResult(ResponseEntity.status(httpStatus).body(response));

        } catch (Exception e) {

            handleError(deferredResult, e);
        }
    }


    private void handleError(DeferredResult<ResponseEntity<Object>> deferredResult, Throwable exception) {
        AssignmentResponseModel errorResponse = AssignmentResponseModel.builder()
                .status(AssignmentStatus.ERROR)
                .message("Failed to execute assignment: " + exception.getMessage())
                .build();

        deferredResult.setErrorResult(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
        );
    }


    private void cleanUp(){
        examService.clearAllExams();
        teacherQuotaService.clearAllQuotas();
        quotaPerGradeService.clearAllQuotasPerGrade();
        gradeService.clearAllGrades();
    }
}