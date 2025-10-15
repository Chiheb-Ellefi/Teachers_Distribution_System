package org.teacherdistributionsystem.distribution_system.repositories.teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherUnavailability;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilitiesProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilityProjection;

import java.util.List;

@Repository
public interface TeacherUnavailabilityRepository extends JpaRepository<TeacherUnavailability, Long> {
    @Query("SELECT t.id AS id, t.numeroJour AS numeroJour, t.seance AS seance " +
            "FROM TeacherUnavailability t WHERE t.examSession.id = ?1 ")
    List<TeacherUnavailabilityProjection> getTeacherUnavailableBySessionId(Long sessionId);

    @Query("SELECT tu.teacher.prenom as prenom, " +
            "tu.teacher.nom as nom, " +
            "tu.teacher.codeSmartex as codeSmartex, " +
            "tu.teacher.email as email, " +
            "tu.seance as seance, " +
            "tu.numeroJour as numeroJour, " +
            "tu.examSession.academicYear as academicYear, " +
            "tu.examSession.semesterLibelle as semesterLibelle, " +
            "tu.examSession.sessionLibelle as sessionLibelle " +
            "FROM TeacherUnavailability tu " +
            "WHERE tu.examSession.id = :examSessionId")
    List<TeacherUnavailabilitiesProjection> findAllByExamSessionId(@Param("examSessionId") Long examSessionId);
}
