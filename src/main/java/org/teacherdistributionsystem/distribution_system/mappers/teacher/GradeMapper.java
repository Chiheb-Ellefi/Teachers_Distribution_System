package org.teacherdistributionsystem.distribution_system.mappers.teacher;

import org.teacherdistributionsystem.distribution_system.dtos.teacher.GradeDto;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Grade;

public class GradeMapper {

    public static Grade toGrade(GradeDto dto) {
        return Grade.builder()
                .gradeCode(dto.getGradeCode())
                .gradeLibelle(dto.getGradeLibelle())
                .defaultQuotaPerSession(dto.getDefaultQuotaPerSession())
                .priorityLevel(dto.getPriorityLevel())
                .build();
    }

    public static GradeDto toGradeDto(Grade grade) {
        return GradeDto.builder()
                .gradeCode(grade.getGradeCode())
                .gradeLibelle(grade.getGradeLibelle())
                .defaultQuotaPerSession(grade.getDefaultQuotaPerSession())
                .priorityLevel(grade.getPriorityLevel())
                .build();
    }
}
