package org.teacherdistributionsystem.distribution_system.repositories.assignement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;


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

    @Query("")
    List<TeacherExamAssignment> getAllByDate(Long sessionId, Integer day, SeanceType value);
}