package org.teacherdistributionsystem.distribution_system.controllers.assignment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.teacherdistributionsystem.distribution_system.config.AssignmentConstraintConfig;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.AssignmentResponseModel;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentAlgorithmService;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/assignment")
public class AssignmentConfigController {

    private final AssignmentAlgorithmService assignmentService;

    public AssignmentConfigController(AssignmentAlgorithmService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /**
     * Execute assignment with default configuration
     */
    @PostMapping("/execute/{sessionId}")
    public CompletableFuture<ResponseEntity<AssignmentResponseModel>> executeAssignment(
            @PathVariable Long sessionId) {

        return assignmentService.executeAssignment(sessionId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.badRequest().build());
    }

    /**
     * Execute assignment with custom configuration
     */
    @PostMapping("/execute/{sessionId}/custom")
    public CompletableFuture<ResponseEntity<AssignmentResponseModel>> executeAssignmentWithConfig(
            @PathVariable Long sessionId,
            @RequestBody AssignmentConstraintConfig config) {

        return assignmentService.executeAssignmentWithConfig(sessionId, config)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.badRequest().build());
    }

    /**
     * Get default configuration
     */
    @GetMapping("/config/default")
    public ResponseEntity<AssignmentConstraintConfig> getDefaultConfig() {
        return ResponseEntity.ok(AssignmentConstraintConfig.defaultConfig());
    }

    /**
     * Get strict configuration
     */
    @GetMapping("/config/strict")
    public ResponseEntity<AssignmentConstraintConfig> getStrictConfig() {
        return ResponseEntity.ok(AssignmentConstraintConfig.strictConfig());
    }

    /**
     * Get relaxed configuration
     */
    @GetMapping("/config/relaxed")
    public ResponseEntity<AssignmentConstraintConfig> getRelaxedConfig() {
        return ResponseEntity.ok(AssignmentConstraintConfig.relaxedConfig());
    }

    /**
     * Get fairness-optimized configuration
     */
    @GetMapping("/config/fairness")
    public ResponseEntity<AssignmentConstraintConfig> getFairnessConfig() {
        return ResponseEntity.ok(AssignmentConstraintConfig.fairnessOptimizedConfig());
    }
}
