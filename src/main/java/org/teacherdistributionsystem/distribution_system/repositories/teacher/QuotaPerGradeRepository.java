package org.teacherdistributionsystem.distribution_system.repositories.teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.teacher.QuotaPerGrade;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotaPerGradeRepository extends JpaRepository<QuotaPerGrade, String> {
    Optional<QuotaPerGrade> findByGrade(GradeType grade);
    @Query(value = "SELECT qg.grade, qg.priority FROM QuotaPerGrade qg")
    List<Object[]> getPrioritiesByGrade();
}
