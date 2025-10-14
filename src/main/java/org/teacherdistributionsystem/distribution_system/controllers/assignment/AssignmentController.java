package org.teacherdistributionsystem.distribution_system.controllers.assignment;



import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;
import org.teacherdistributionsystem.distribution_system.models.responses.AssignmentResponseModel;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentAlgorithmService;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentPersistenceService;
import org.teacherdistributionsystem.distribution_system.utils.data.JsonFileWriter;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssignmentController {

    private final AssignmentAlgorithmService assignmentAlgorithmService;
    private final AssignmentPersistenceService persistenceService;
    private final JsonFileWriter jsonFileWriter;

    @GetMapping("/execute/{sessionId}")
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
            jsonFileWriter.writeDataToJsonFile(response);
            return ResponseEntity.status(httpStatus).body("Data successfully saved to file");

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
}