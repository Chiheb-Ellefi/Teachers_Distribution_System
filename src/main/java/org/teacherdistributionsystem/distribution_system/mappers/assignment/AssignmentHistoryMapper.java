package org.teacherdistributionsystem.distribution_system.mappers.assignment;

import org.teacherdistributionsystem.distribution_system.dtos.assignment.AssignmentHistoryDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.AssignmentHistory;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;

public class AssignmentHistoryMapper {

    public static AssignmentHistory toAssignmentHistory(AssignmentHistoryDto dto, Teacher teacher, ExamSession examSession) {
        return AssignmentHistory.builder()
                .id(dto.getId())
                .teacher(teacher)
                .examSession(examSession)
                .totalSeancesAssigned(dto.getTotalSeancesAssigned())
                .earlyMorningCount(dto.getEarlyMorningCount())
                .lateAfternoonCount(dto.getLateAfternoonCount())
                .consecutiveDaysCount(dto.getConsecutiveDaysCount())
                .weekendCount(dto.getWeekendCount())
                .credit(dto.getCredit())
                .build();
    }

    public static AssignmentHistoryDto toAssignmentHistoryDto(AssignmentHistory assignmentHistory) {
        return AssignmentHistoryDto.builder()
                .id(assignmentHistory.getId())
                .teacherId(assignmentHistory.getTeacher().getId())
                .examSessionId(assignmentHistory.getExamSession().getId())
                .totalSeancesAssigned(assignmentHistory.getTotalSeancesAssigned())
                .earlyMorningCount(assignmentHistory.getEarlyMorningCount())
                .lateAfternoonCount(assignmentHistory.getLateAfternoonCount())
                .consecutiveDaysCount(assignmentHistory.getConsecutiveDaysCount())
                .weekendCount(assignmentHistory.getWeekendCount())
                .credit(assignmentHistory.getCredit())
                .build();
    }
}
