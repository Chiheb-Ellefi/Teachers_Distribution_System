package org.teacherdistributionsystem.distribution_system.repositories.assignement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.assignment.Exam;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
}
