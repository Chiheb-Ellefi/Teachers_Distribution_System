package org.teacherdistributionsystem.distribution_system.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadDetailModel{
    private String examId;
    private Integer day;
    private String dayLabel;
    private Integer seance;
    private String seanceLabel;
    private String room;

    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
}