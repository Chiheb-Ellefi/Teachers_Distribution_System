package org.teacherdistributionsystem.distribution_system.mappers.assignment;

import org.teacherdistributionsystem.distribution_system.dtos.assignment.ExamDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.Exam;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;

public class ExamMapper {

    public static Exam toExam(ExamDto dto, ExamSession examSession, Teacher responsable) {
        return Exam.builder()
                .id(dto.getId())
                .examSession(examSession)
                .responsable(responsable)
                .examDate(dto.getExamDate())
                .jourNumero(dto.getJourNumero())
                .seance(dto.getSeance())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .numRooms(dto.getNumRooms())
                .requiredSupervisors(dto.getRequiredSupervisors())
                .examType(dto.getExamType())
                .build();
    }

    public static ExamDto toExamDto(Exam exam) {
        return ExamDto.builder()
                .id(exam.getId())
                .examSessionId(exam.getExamSession().getId())
                .responsableId(exam.getResponsable().getId())
                .examDate(exam.getExamDate())
                .jourNumero(exam.getJourNumero())
                .seance(exam.getSeance())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .numRooms(exam.getNumRooms())
                .requiredSupervisors(exam.getRequiredSupervisors())
                .examType(exam.getExamType())
                .build();
    }
}
