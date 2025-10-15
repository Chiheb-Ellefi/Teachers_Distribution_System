package org.teacherdistributionsystem.distribution_system.models.projections;

public interface TeacherUnavailabilitiesProjection {

    String getPrenom();
    String getNom();
    Integer getCodeSmartex();
    String getEmail();

    String getSeance();
    Integer getNumeroJour();

    String getAcademicYear();
    String getSemesterLibelle();
    String getSessionLibelle();
}
