package org.teacherdistributionsystem.distribution_system.repositories.assignement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;
import org.teacherdistributionsystem.distribution_system.models.projections.AssignmentDetailsProjection;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.DaySeanceGroupAssignments;


import java.util.List;

@Repository
public interface TeacherExamAssignmentRepository extends JpaRepository<TeacherExamAssignment, Long> {

    List<TeacherExamAssignment> findBySessionIdAndIsActiveTrue(Long sessionId);

    List<TeacherExamAssignment> findByTeacherIdAndSessionIdAndIsActiveTrue(Long teacherId, Long sessionId);

    List<TeacherExamAssignment> findByExamIdAndSessionIdAndIsActiveTrue(String examId, Long sessionId);

    @Modifying
    @Query("UPDATE TeacherExamAssignment t SET t.isActive = false WHERE t.sessionId = :sessionId")
    void deactivateAllForSession(Long sessionId);

    boolean existsBySessionIdAndIsActiveTrue(Long sessionId);

    @Query(value = "SELECT a.exam_date AS examDate, a.start_time AS startTime, a.end_time AS endTime, " +
            "a.exam_day AS examDay, a.seance AS seance, a.teacher_id AS teacherId, " +
            "t.nom AS nom, t.prenom AS prenom, t.email AS email, t.code_smartex AS codeSmartex, " +
            "t.grade_code AS gradeCode, t.participe_surveillance AS participeSurveillance, " +
            "t.quota_credit AS quotaCredit " +
            "FROM teacher_exam_assignments a " +
            "INNER JOIN teachers t ON a.teacher_id = t.id " +
            "WHERE a.session_id = :sessionId AND a.is_active = true " +
            "AND a.seance = :seance AND a.exam_day = :examDay",
            nativeQuery = true)
    List<AssignmentDetailsProjection> getAllByDate(@Param("sessionId") Long sessionId,
                                                   @Param("examDay") Integer examDay,
                                                   @Param("seance") Integer seance);
}