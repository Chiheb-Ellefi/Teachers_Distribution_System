package org.teacherdistributionsystem.distribution_system.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentMetadata {
    private Long sessionId;
    private String sessionName;
    private Integer totalExams;
    private Integer totalTeachers;
    private Integer participatingTeachers;
    private Double solutionTimeSeconds;
    private Boolean isOptimal;
    private Integer totalAssignmentsMade;
    private Integer relaxedTeachersCount;
    private Integer totalConstraints;
    private Integer relaxationAttempts;
}