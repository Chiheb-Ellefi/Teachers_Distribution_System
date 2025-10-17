package org.teacherdistributionsystem.distribution_system.models.others;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAssignmentModel {
    private String examId;
    private Integer day;
    private String dayLabel;
    private Integer seance;
    private String seanceLabel;
    private String room;
    private int requiredSupervisors;
    private Long ownerTeacherId;
    private String ownerTeacherName;
    private List<AssignedTeacherModel> assignedTeachers;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
