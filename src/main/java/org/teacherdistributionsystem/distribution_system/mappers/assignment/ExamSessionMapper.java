package org.teacherdistributionsystem.distribution_system.mappers.assignment;

import org.teacherdistributionsystem.distribution_system.dtos.assignment.ExamSessionDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;

public class ExamSessionMapper {

    public static ExamSession toExamSession(ExamSessionDto dto) {
        return ExamSession.builder()
                .id(dto.getId())
                .academicYear(dto.getAcademicYear())
                .semesterCode(dto.getSemesterCode())
                .semesterLibelle(dto.getSemesterLibelle())
                .sessionLibelle(dto.getSessionLibelle())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .numExamDays(dto.getNumExamDays())
                .seancesPerDay(dto.getSeancesPerDay())
                .build();
    }

    public static ExamSessionDto toExamSessionDto(ExamSession examSession) {
        return ExamSessionDto.builder()
                .id(examSession.getId())
                .academicYear(examSession.getAcademicYear())
                .semesterCode(examSession.getSemesterCode())
                .semesterLibelle(examSession.getSemesterLibelle())
                .sessionLibelle(examSession.getSessionLibelle())
                .startDate(examSession.getStartDate())
                .endDate(examSession.getEndDate())
                .numExamDays(examSession.getNumExamDays())
                .seancesPerDay(examSession.getSeancesPerDay())
                .build();
    }
}
