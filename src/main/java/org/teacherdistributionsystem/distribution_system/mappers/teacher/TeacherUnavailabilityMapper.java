package org.teacherdistributionsystem.distribution_system.mappers.teacher;

import org.teacherdistributionsystem.distribution_system.dtos.teacher.TeacherUnavailabilityDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherUnavailability;

public class TeacherUnavailabilityMapper {

    public static TeacherUnavailability toTeacherUnavailability(TeacherUnavailabilityDto dto, ExamSession examSession, Teacher teacher) {
        return TeacherUnavailability.builder()
                .id(dto.getId())
                .examSession(examSession)
                .teacher(teacher)
                .numeroJour(dto.getNumeroJour())
                .seance(dto.getSeance())
                .build();
    }

    public static TeacherUnavailabilityDto toTeacherUnavailabilityDto(TeacherUnavailability teacherUnavailability) {
        return TeacherUnavailabilityDto.builder()
                .id(teacherUnavailability.getId())
                .examSessionId(teacherUnavailability.getExamSession().getId())
                .teacherId(teacherUnavailability.getTeacher().getId())
                .numeroJour(teacherUnavailability.getNumeroJour())
                .seance(teacherUnavailability.getSeance())
                .build();
    }
}
