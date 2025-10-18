package org.teacherdistributionsystem.distribution_system.controllers.assignment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.teacherdistributionsystem.distribution_system.config.AssignmentConstraintConfig;


@RestController
@RequestMapping("/api/assignment")
public class AssignmentConfigController {



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
