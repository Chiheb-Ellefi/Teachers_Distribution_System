package org.teacherdistributionsystem.distribution_system.models.projections;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AssignmentDetailsProjection {
    Integer getExamDay();
    Integer getSeance();
    LocalDate getExamDate();
    LocalTime getStartTime();
    LocalTime getEndTime();
    Long getTeacherId();


    String getNom();
    String getPrenom();
    String getEmail();
    Integer getCodeSmartex();
    String getGradeCode();
    Boolean getParticipeSurveillance();
    Integer getQuotaCredit();
}