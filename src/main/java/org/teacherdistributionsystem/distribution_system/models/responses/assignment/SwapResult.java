package org.teacherdistributionsystem.distribution_system.models.responses.assignment;

import lombok.Getter;
import lombok.Setter;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentSwapService;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public  class SwapResult {
    private boolean success;
    private String message;
    private List<String> violations;
    private SwapDetails details;

    public static SwapResult success(TeacherExamAssignment assignment1,
                                     TeacherExamAssignment assignment2) {
        SwapResult result = new SwapResult();
        result.success = true;
        result.message = "Assignments swapped successfully";
        result.violations = new ArrayList<>();
        result.details = new SwapDetails(assignment1, assignment2);
        return result;
    }

    public static SwapResult failure(String message) {
        SwapResult result = new SwapResult();
        result.success = false;
        result.message = message;
        result.violations = new ArrayList<>();
        return result;
    }

    public static SwapResult failure(String message, List<String> violations) {
        SwapResult result = new SwapResult();
        result.success = false;
        result.message = message;
        result.violations = violations;
        return result;
    }


}