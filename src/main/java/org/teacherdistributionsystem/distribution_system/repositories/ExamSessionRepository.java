package org.teacherdistributionsystem.distribution_system.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

}
