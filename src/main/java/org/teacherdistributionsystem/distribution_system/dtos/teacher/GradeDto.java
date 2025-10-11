package org.teacherdistributionsystem.distribution_system.dtos.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeDto {
    private String gradeCode;
    private String gradeLibelle;
    private Integer defaultQuotaPerSession;
    private Integer priorityLevel;
}