package org.teacherdistributionsystem.distribution_system.controllers.assignment;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherExamAssignmentDto;
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
    public ResponseEntity<List<TeacherExamAssignmentDto>> getAssignmentsForSession(@PathVariable Long sessionId) {
        if(sessionId == null) {
            throw new BadRequestException("Bad Request", "Session id is required");
        }
        List<TeacherExamAssignmentDto> result = assignmentPersistenceService.getAssignmentsForSession(sessionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/teacher/{sessionId}/{teacherId}")
    public ResponseEntity<List<TeacherExamAssignmentDto>> getTeacherAssignments(@PathVariable Long sessionId, @PathVariable Long teacherId){
        if(sessionId == null) {
            throw new BadRequestException("Bad Request", "Session id is required");
        }
        if(teacherId == null) {
            throw new BadRequestException("Bad Request", "Teacher id is required");
        }
        return ResponseEntity.ok().body(assignmentPersistenceService.getTeacherAssignments(teacherId, sessionId,false));
    }

    @PostMapping("/execute/{sessionId}")
    public DeferredResult<ResponseEntity<Object>> executeAssignment(@PathVariable Long sessionId) {

        DeferredResult<ResponseEntity<Object>> deferredResult = new DeferredResult<>(15000L);

        try {
            assignmentAlgorithmService.executeAssignment(sessionId)
                    .whenComplete((response, exception) -> {
                        try {
                            if (exception != null) {
                                handleError(deferredResult, exception);
                            } else {
                                handleSuccess(deferredResult, response);
                            }
                        } catch (Exception e) {
                            handleError(deferredResult, e);
                        } finally {
                            cleanUp();
                        }
                    });
        } catch (Exception e) {
            // Catch any exception that occurs when setting up the async call
            handleError(deferredResult, e);
            cleanUp();
        }

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

    private void handleSuccess(DeferredResult<ResponseEntity<Object>> deferredResult,
                               AssignmentResponseModel response) {
        try {
            HttpStatus httpStatus = switch (response.getStatus()) {
                case SUCCESS -> HttpStatus.OK;
                case INFEASIBLE -> HttpStatus.OK;
                case TIMEOUT -> HttpStatus.PARTIAL_CONTENT;
                case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            };

            if (response.getStatus() == AssignmentStatus.SUCCESS) {
                persistenceService.saveAssignmentResultsAsync(response);
                jsonFileWriter.writeDataToJsonFileAsync(response);

                deferredResult.setResult(ResponseEntity.status(httpStatus).body(AssignmentResponseModel.builder()
                        .diagnosis(response.getDiagnosis())
                                .status(response.getStatus())
                        .message(response.getMessage())
                                .metadata(response.getMetadata())
                                .generatedAt(response.getGeneratedAt())
                        .build()));
            }
        } catch (Exception e) {
            handleError(deferredResult, e);
        }
    }

    private void handleError(DeferredResult<ResponseEntity<Object>> deferredResult, Throwable exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Failed to execute assignment";

        Throwable actualException = exception;
        if (exception instanceof java.util.concurrent.CompletionException && exception.getCause() != null) {
            actualException = exception.getCause();
        }
        if (actualException instanceof EntityNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = actualException.getMessage();
        } else if (actualException instanceof BadRequestException) {
            status = HttpStatus.BAD_REQUEST;
            message = actualException.getMessage();
        } else if (actualException instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            message = actualException.getMessage();
        } else {
            message = "Failed to execute assignment: " + actualException.getMessage();
        }

        AssignmentResponseModel errorResponse = AssignmentResponseModel.builder()
                .status(AssignmentStatus.ERROR)
                .message(message)
                .build();

        deferredResult.setResult(
                ResponseEntity.status(status).body(errorResponse)
        );
    }


    private void cleanUp(){
        examService.clearAllExams();
        teacherQuotaService.clearAllQuotas();
        quotaPerGradeService.clearAllQuotasPerGrade();
        gradeService.clearAllGrades();
    }

    @PostMapping("/clean-up")
    public ResponseEntity<Void> cleanUpDB(){
        cleanUp();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Boolean> checkAssignmentStatus(@PathVariable Long sessionId) {
        boolean exists = persistenceService.hasAssignments(sessionId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/exam/{sessionId}/{examId}")
    public ResponseEntity<List<TeacherExamAssignmentDto>> getExamAssignments(@PathVariable Long sessionId, @PathVariable String examId){
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




}