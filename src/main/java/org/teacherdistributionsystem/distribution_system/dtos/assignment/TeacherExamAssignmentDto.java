package org.teacherdistributionsystem.distribution_system.dtos.assignment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherExamAssignmentDto {

    private Long id;
    private Long sessionId;
    private String examId;
    private Long teacherId;
    private Integer examDay;
    private Integer seance;
    private LocalDateTime assignedAt;
    private Boolean isActive;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String roomNum;
}
