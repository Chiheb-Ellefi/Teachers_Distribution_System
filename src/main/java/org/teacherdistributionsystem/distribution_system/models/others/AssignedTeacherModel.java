package org.teacherdistributionsystem.distribution_system.models.others;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedTeacherModel {
    private Long teacherId;
    private String teacherName;
    private String teacherGrade;
}