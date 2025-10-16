package org.teacherdistributionsystem.distribution_system.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherWorkloadModel {
    private Long teacherId;
    private String teacherName;
    private String email;
    private String grade;
    private Integer assignedSupervisions;
    private Integer quotaSupervisions;
    private Integer unavailabilityCredit;
    private Double utilizationPercentage;
    private List<WorkloadDetailModel> assignments;
}