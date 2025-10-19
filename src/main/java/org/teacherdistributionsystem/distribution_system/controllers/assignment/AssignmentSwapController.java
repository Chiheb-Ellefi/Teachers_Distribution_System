package org.teacherdistributionsystem.distribution_system.controllers.assignment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.models.requests.SwapRequest;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.SwapResponse;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.SwapResult;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.ValidationResponse;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentSwapService;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "*")
public class AssignmentSwapController {

    private final AssignmentSwapService swapService;

    public AssignmentSwapController(AssignmentSwapService swapService) {
        this.swapService = swapService;
    }

    @PostMapping("/swap")
    public ResponseEntity<SwapResponse> swapAssignments(@RequestBody SwapRequest request) {
        try {

            if (request.getAssignmentId1() == null || request.getAssignmentId2() == null) {
                return ResponseEntity.badRequest()
                        .body(SwapResponse.error("Both assignment IDs are required"));
            }

            if (request.getAssignmentId1().equals(request.getAssignmentId2())) {
                return ResponseEntity.badRequest()
                        .body(SwapResponse.error("Cannot swap an assignment with itself"));
            }


            SwapResult result = swapService.swapAssignments(
                    request.getAssignmentId1(),
                    request.getAssignmentId2()
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(SwapResponse.success(result));
            } else {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(SwapResponse.failure(result));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SwapResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SwapResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }


    @GetMapping("/swap/validate")
    public ResponseEntity<ValidationResponse> validateSwap(
            @RequestParam Long assignmentId1,
            @RequestParam Long assignmentId2) {

        try {

            return ResponseEntity.ok(ValidationResponse.builder()
                    .message("Use POST /api/assignments/swap to attempt the swap")
                    .note("Validation is performed automatically during swap")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ValidationResponse.builder()
                            .message("Error: " + e.getMessage())
                            .build());
        }
    }




}