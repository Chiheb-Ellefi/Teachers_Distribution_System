package org.teacherdistributionsystem.distribution_system.models.responses;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Main response DTO for assignment algorithm results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentResponseModel {
    private AssignmentStatus status;
    private String message;
    private AssignmentMetadata metadata;
    private List<ExamAssignmentModel> examAssignments;
    private List<TeacherWorkloadModel> teacherWorkloads;
    private InfeasibilityDiagnosisModel diagnosis; // Only present if infeasible
    private LocalDateTime generatedAt;
}




