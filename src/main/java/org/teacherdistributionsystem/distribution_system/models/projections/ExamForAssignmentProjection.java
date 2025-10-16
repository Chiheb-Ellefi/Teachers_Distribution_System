package org.teacherdistributionsystem.distribution_system.models.projections;

import org.teacherdistributionsystem.distribution_system.enums.SeanceType;

public interface ExamForAssignmentProjection {
    String getId();
    SeanceType getSeance();
    Integer getJourNumero();
    String getNumRooms();
    Long getResponsableId();
    Integer getRequiredSupervisors();
}