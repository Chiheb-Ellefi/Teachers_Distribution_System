package org.teacherdistributionsystem.distribution_system.models.projections;


import org.teacherdistributionsystem.distribution_system.enums.SeanceType;

import java.time.LocalDate;

public interface ExamProjection {
    String getId();
    SeanceType getSeance();
    LocalDate getExamDate();
    String getNumRooms();
    Long getResponsableId();
    Integer getRequiredSupervisors();
    String getNom();
    String getPrenom();

}