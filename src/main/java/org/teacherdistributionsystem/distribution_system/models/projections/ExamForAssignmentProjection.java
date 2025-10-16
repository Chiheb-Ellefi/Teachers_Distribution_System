package org.teacherdistributionsystem.distribution_system.models.projections;

import org.teacherdistributionsystem.distribution_system.enums.SeanceType;

import java.time.LocalDate;
import java.time.LocalTime;

public interface ExamForAssignmentProjection {
    String getId();
    SeanceType getSeance();
    Integer getJourNumero();
    String getNumRooms();
    Long getResponsableId();
    Integer getRequiredSupervisors();
    LocalDate getExamDate();
    LocalTime getStartTime();
    LocalTime getEndTime();
}