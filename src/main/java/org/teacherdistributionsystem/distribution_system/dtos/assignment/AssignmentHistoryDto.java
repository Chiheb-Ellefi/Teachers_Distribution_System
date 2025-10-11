package org.teacherdistributionsystem.distribution_system.dtos.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentHistoryDto {
    private Long id;

    private Long teacherId;
    private Long examSessionId;

    private Integer totalSeancesAssigned;
    private Integer earlyMorningCount;
    private Integer lateAfternoonCount;
    private Integer consecutiveDaysCount;
    private Integer weekendCount;
    private Integer credit;
}