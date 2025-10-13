package org.teacherdistributionsystem.distribution_system.mappers.teacher;

import org.teacherdistributionsystem.distribution_system.dtos.teacher.TeacherPreferenceDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherPreference;

public class TeacherPreferenceMapper {

    public static TeacherPreference toTeacherPreference(TeacherPreferenceDto dto, Teacher teacher, ExamSession examSession) {
        return TeacherPreference.builder()
                .id(dto.getId())
                .teacher(teacher)
                .examSession(examSession)
                .preferenceTypes(dto.getPreferenceTypes())
                .priorityWeight(dto.getPriorityWeight())
                .build();
    }

    public static TeacherPreferenceDto toTeacherPreferenceDto(TeacherPreference teacherPreference) {
        return TeacherPreferenceDto.builder()
                .id(teacherPreference.getId())
                .teacherId(teacherPreference.getTeacher().getId())
                .examSessionId(teacherPreference.getExamSession().getId())
                .preferenceTypes(teacherPreference.getPreferenceTypes())
                .priorityWeight(teacherPreference.getPriorityWeight())
                .build();
    }
}
