package org.teacherdistributionsystem.distribution_system.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}