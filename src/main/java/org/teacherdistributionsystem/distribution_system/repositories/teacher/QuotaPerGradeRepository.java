package org.teacherdistributionsystem.distribution_system.repositories.teacher;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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


    @Modifying
    @Transactional
    @Query("""
        UPDATE QuotaPerGrade qg
        SET qg.defaultQuota = :quota
        WHERE qg.grade = :grade
    """)
    void updateQuotaPerGrade(@Param("grade") String grade, @Param("quota") Integer quota);

}
