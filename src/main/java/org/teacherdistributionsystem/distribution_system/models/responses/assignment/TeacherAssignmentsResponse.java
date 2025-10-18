package org.teacherdistributionsystem.distribution_system.models.responses.assignment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherExamAssignmentDto;

import java.util.List;

@Builder
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherAssignmentsResponse {
    private Long teacherId;
    private String teacherName;
    private String email;
    private boolean supervisionStatus;
    private List<TeacherExamAssignmentDto> assignments;
}
