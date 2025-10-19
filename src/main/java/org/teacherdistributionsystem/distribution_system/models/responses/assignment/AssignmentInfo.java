package org.teacherdistributionsystem.distribution_system.models.responses.assignment;

import lombok.Getter;
import lombok.Setter;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;

@Getter
@Setter
public  class AssignmentInfo {
    private Long assignmentId;
    private Long teacherId;
    private String examId;
    private Integer examDay;
    private Integer seance;
    private String examDate;
    private String startTime;
    private String endTime;

    public AssignmentInfo(TeacherExamAssignment assignment) {
        this.assignmentId = assignment.getId();
        this.teacherId = assignment.getTeacherId();
        this.examId = assignment.getExamId();
        this.examDay = assignment.getExamDay();
        this.seance = assignment.getSeance();
        this.examDate = assignment.getExamDate() != null ?
                assignment.getExamDate().toString() : null;
        this.startTime = assignment.getStartTime() != null ?
                assignment.getStartTime().toString() : null;
        this.endTime = assignment.getEndTime() != null ?
                assignment.getEndTime().toString() : null;
    }


}