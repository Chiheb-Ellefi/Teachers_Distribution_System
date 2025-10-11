package org.teacherdistributionsystem.distribution_system.dtos.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDto {
    private Long id;

    private Long examSessionId;
    private String examId;
    private Long teacherId;

    private LocalDate examDate;
    private Integer jourNumero;
    private SeanceType seance;
    private Boolean isSubjectExpert;
    private String roomNumber;
    private String assignmentType;
}