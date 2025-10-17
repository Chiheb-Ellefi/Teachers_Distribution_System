package org.teacherdistributionsystem.distribution_system.models.others;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsModel {
    private Integer totalTeachers;
    private Integer participatingTeachers;
    private Integer totalExams;
    private Integer totalSlotsUsed;
    private Double averageTeachersPerSlot;
    private Integer maxTeachersInOneSlot;
}