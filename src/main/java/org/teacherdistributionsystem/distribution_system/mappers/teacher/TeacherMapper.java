package org.teacherdistributionsystem.distribution_system.mappers.teacher;

import org.teacherdistributionsystem.distribution_system.dtos.teacher.TeacherDto;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;

public class TeacherMapper {

    public static Teacher toTeacher(TeacherDto dto) {
        return Teacher.builder()
                .id(dto.getId())
                .prenom(dto.getPrenom())
                .nom(dto.getNom())
                .email(dto.getEmail())
                .codeSmartex(dto.getCodeSmartex())
                .gradeCode(dto.getGradeCode())
                .participeSurveillance(dto.getParticipeSurveillance())
                .quotaCredit(dto.getQuotaCredit())
                .build();
    }

    public static TeacherDto toTeacherDto(Teacher teacher) {
        return TeacherDto.builder()
                .id(teacher.getId())
                .prenom(teacher.getPrenom())
                .nom(teacher.getNom())
                .email(teacher.getEmail())
                .codeSmartex(teacher.getCodeSmartex())
                .gradeCode(teacher.getGradeCode())
                .participeSurveillance(teacher.getParticipeSurveillance())
                .quotaCredit(teacher.getQuotaCredit())
                .build();
    }
}
