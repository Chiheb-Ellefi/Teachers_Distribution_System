package org.teacherdistributionsystem.distribution_system.models.responses;

import lombok.Builder;
import lombok.Setter;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherExamAssignmentDto;

import java.util.List;

@Builder
@Setter
public class TeacherAssignmentsResponse {
    private Long teacherId;
    private String teacherName;
    private String email;
    private boolean supervisionStatus;
    private List<TeacherExamAssignmentDto> assignments;
}
