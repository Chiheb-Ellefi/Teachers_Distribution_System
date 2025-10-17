package org.teacherdistributionsystem.distribution_system.mappers.assignment;

import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherExamAssignmentDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;

public class TeacherExamAssignmentMapper {
    public static TeacherExamAssignment toEntity(TeacherExamAssignmentDto dto) {
        return TeacherExamAssignment.builder()
                .id(dto.getId())
                .sessionId(dto.getSessionId())
                .teacherId(dto.getTeacherId())
                .examId(dto.getExamId())
                .examDate(dto.getExamDate())
                .examDay(dto.getExamDay())
                .seance(dto.getSeance())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .isActive(dto.getIsActive())
                .assignedAt(dto.getAssignedAt())
                .build();

    }
    public static TeacherExamAssignmentDto toDto(TeacherExamAssignment entity) {
        return TeacherExamAssignmentDto.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .teacherId(entity.getTeacherId())
                .examId(entity.getExamId())
                .examDate(entity.getExamDate())
                .examDay(entity.getExamDay())
                .seance(entity.getSeance())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .isActive(entity.getIsActive())
                .assignedAt(entity.getAssignedAt())
                .build();
    }
    public static TeacherExamAssignmentDto toLightDto(TeacherExamAssignment entity) {
        return TeacherExamAssignmentDto.builder()
                .id(entity.getId())
                .examDate(entity.getExamDate())
                .examDay(entity.getExamDay())
                .seance(entity.getSeance())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .assignedAt(entity.getAssignedAt())
                .build();
    }
}
