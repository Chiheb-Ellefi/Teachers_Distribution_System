package org.teacherdistributionsystem.distribution_system.controllers.assignment;



import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;
import org.teacherdistributionsystem.distribution_system.models.responses.AssignmentResponseModel;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentAlgorithmService;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssignmentController {

    private final AssignmentAlgorithmService assignmentAlgorithmService;


    @GetMapping("/execute/{sessionId}")
   
    public ResponseEntity<AssignmentResponseModel> executeAssignment(@PathVariable Long sessionId) {

        try {
            AssignmentResponseModel response = assignmentAlgorithmService.executeAssignment(sessionId);

            HttpStatus httpStatus = switch (response.getStatus()) {
                case SUCCESS -> HttpStatus.OK;
                case INFEASIBLE -> HttpStatus.OK;
                case TIMEOUT -> HttpStatus.PARTIAL_CONTENT;
                case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            };

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

}