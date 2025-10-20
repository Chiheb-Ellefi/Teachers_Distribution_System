package org.teacherdistributionsystem.distribution_system.repositories.assignement;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.assignment.AssignmentSession;

@Repository
public interface AssignmentSessionRepository extends JpaRepository<AssignmentSession, Long> {

    AssignmentSession findByExamSessionId(Long examSessionId);

    @Transactional
    @Modifying
    @Query(value = """

            TRUNCATE TABLE quota_per_grade;
    TRUNCATE TABLE grade_types;
    TRUNCATE TABLE teacher_unavailability;
    TRUNCATE TABLE teacher_quotas;
    TRUNCATE TABLE teacher_exam_assignments;
    TRUNCATE TABLE exams;
    TRUNCATE TABLE teachers;
    TRUNCATE TABLE exam_sessions RESTART IDENTITY CASCADE;
    """, nativeQuery = true)
    void truncateAllTables();
    }