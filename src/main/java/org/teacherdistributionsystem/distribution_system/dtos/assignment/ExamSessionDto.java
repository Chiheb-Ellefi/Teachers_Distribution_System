package org.teacherdistributionsystem.distribution_system.dtos.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionDto {
    private Long id;

    private String academicYear;
    private String semesterCode;
    private String semesterLibelle;
    private String sessionLibelle;

    private LocalDate startDate;
    private LocalDate endDate;

    private Integer numExamDays;
    private Integer seancesPerDay;
    private Integer teachersPerExam;
}