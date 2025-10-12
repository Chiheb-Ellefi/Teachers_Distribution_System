package org.teacherdistributionsystem.distribution_system.mappers.teacher;

import org.teacherdistributionsystem.distribution_system.dtos.teacher.TeacherQuotaDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherQuota;

public class TeacherQuotaMapper {

    public static TeacherQuota toTeacherQuota(TeacherQuotaDto dto, ExamSession examSession, Teacher teacher) {
        return TeacherQuota.builder()
                .id(dto.getId())
                .examSession(examSession)
                .teacher(teacher)
                .assignedQuota(dto.getAssignedQuota())
                .quotaType(dto.getQuotaType())
                .reason(dto.getReason())
                .build();
    }

    public static TeacherQuotaDto toTeacherQuotaDto(TeacherQuota teacherQuota) {
        return TeacherQuotaDto.builder()
                .id(teacherQuota.getId())
                .examSessionId(teacherQuota.getExamSession().getId())
                .teacherId(teacherQuota.getTeacher().getId())
                .assignedQuota(teacherQuota.getAssignedQuota())
                .quotaType(teacherQuota.getQuotaType())
                .reason(teacherQuota.getReason())
                .build();
    }
}
