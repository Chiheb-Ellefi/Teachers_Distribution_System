package org.teacherdistributionsystem.distribution_system.models.projections;

import java.time.LocalDate;
import java.time.LocalTime;




public interface AssignmentDetailsProjection {
    LocalDate getExamDate();
    LocalTime getStartTime();
    LocalTime getEndTime();
    Integer getExamDay();
    Integer getSeance();
    Long getTeacherId();
    String getNom();
    String getPrenom();
    String getEmail();
    String getCodeSmartex();
    String getGradeCode();
    Boolean getParticipeSurveillance();
    Integer getQuotaCredit();
    String getNumRooms(); // Add this
}