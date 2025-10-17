package org.teacherdistributionsystem.distribution_system.models.others;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotIssueModel {
    private Integer day;
    private String dayLabel;
    private Integer seance;
    private String seanceLabel;
    private Integer numberOfExams;
    private Integer teachersNeeded;
    private Integer teachersAvailable;
    private Integer deficit; // teachersNeeded - teachersAvailable
    private Boolean isProblem;
}