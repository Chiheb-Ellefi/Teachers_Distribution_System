package org.teacherdistributionsystem.distribution_system.mappers.assignment;

import org.teacherdistributionsystem.distribution_system.dtos.assignment.AssignmentDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.Assignment;
import org.teacherdistributionsystem.distribution_system.entities.assignment.Exam;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;

public class AssignmentMapper {
    public static Assignment toAssignment(AssignmentDto dto, Exam exam, ExamSession examSession, Teacher teacher) {
        return Assignment.builder()
                .id(dto.getId())
                .exam(exam)
                .examSession(examSession)
                .teacher(teacher)
                .examDate(dto.getExamDate())
                .jourNumero(dto.getJourNumero())
                .seance(dto.getSeance())
                .isSubjectExpert(dto.getIsSubjectExpert())
                .roomNumber(dto.getRoomNumber())
                .assignmentType(dto.getAssignmentType())
                .build();
    }
    public static AssignmentDto toAssignmentDto(Assignment assignment) {
       return  AssignmentDto.builder()
               .id(assignment.getId())
               .examId(assignment.getExam().getId())
               .examSessionId(assignment.getExamSession().getId())
               .teacherId(assignment.getTeacher().getId())
               .examDate(assignment.getExamDate())
               .jourNumero(assignment.getJourNumero())
               .seance(assignment.getSeance())
               .isSubjectExpert(assignment.getIsSubjectExpert())
               .roomNumber(assignment.getRoomNumber())
               .assignmentType(assignment.getAssignmentType())
               .build();
    }
}
