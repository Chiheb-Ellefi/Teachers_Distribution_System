package org.teacherdistributionsystem.distribution_system.dtos.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDto {
    private String id;
    private Long examSessionId;
    private Long responsableId;
    private LocalDate examDate;
    private Integer jourNumero;
    private SeanceType seance;
    private LocalTime startTime;
    private LocalTime endTime;
    private String numRooms;
    private Integer requiredSupervisors;
    private String examType;
}