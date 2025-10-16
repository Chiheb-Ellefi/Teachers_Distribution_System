package org.teacherdistributionsystem.distribution_system.repositories.assignement;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.assignment.Exam;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;
import org.teacherdistributionsystem.distribution_system.models.projections.ExamForAssignmentProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.ExamProjection;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, String> {
    @Query("SELECT e.id AS id, e.seance AS seance, e.jourNumero AS jourNumero, e.numRooms AS numRooms, e.responsable.id AS responsableId, e.examDate AS examDate, e.requiredSupervisors AS requiredSupervisors FROM Exam e WHERE e.examSession.id = ?1 ")
    List<ExamForAssignmentProjection> getExamsBySessionIdForAssignment(Long sessionId);
    @Query("SELECT e.id AS id, e.seance AS seance, e.examDate AS examDate, e.numRooms AS numRooms, e.responsable.id AS responsableId, e.requiredSupervisors AS requiredSupervisors, e.responsable.nom AS nom, e.responsable.prenom AS prenom  FROM Exam e WHERE e.examSession.id = ?1 ")
    List<ExamProjection> getExamsBySessionId(Long sessionId);

    @Modifying
    @Transactional
    @Query("""
    UPDATE Exam e 
    SET e.requiredSupervisors = :requiredSupervisors 
    WHERE e.examSession.id = :sessionId 
    AND e.jourNumero = :jourNumero 
    AND e.seance = :seance 
    AND e.numRooms = :numRooms
    """)
    void updateRequiredSupervisorsForRelatedExams(
            Long sessionId,
            Integer jourNumero,
            SeanceType seance,
            String numRooms,
            Integer requiredSupervisors
    );
}
