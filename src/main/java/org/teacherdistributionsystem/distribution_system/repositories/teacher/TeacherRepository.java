package org.teacherdistributionsystem.distribution_system.repositories.teacher;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.models.projections.GradeCountProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherNameProjection;
import org.teacherdistributionsystem.distribution_system.models.responses.teacher.GradeCount;
import org.teacherdistributionsystem.distribution_system.models.responses.teacher.TeacherResponse;


import java.util.List;
import java.util.Map;


@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    @Query("SELECT t.id, t.participeSurveillance FROM Teacher t")
    List<Object[]> getAllParticipants();
    @Query("SELECT t.id, t.gradeCode FROM Teacher t")
    List<Object[]> getAllGrades();
    @Query("SELECT t.id, t.email FROM Teacher t")
    List<Object[]> getAllEmails();

    @Query("SELECT t.id AS id, t.nom AS nom, t.prenom AS prenom FROM Teacher t")
    List<TeacherNameProjection> getAllNames();

    @Modifying
    @Transactional
    @Query("UPDATE Teacher t SET t.quotaCredit = :credit WHERE t.id = :teacherId")
    void updateQuotaCreditById(@Param("teacherId") Long teacherId, @Param("credit") Integer credit);

    @Query("SELECT t.codeSmartex FROM Teacher t ")
    List<Integer> findAllCodesSmartex();

    @Query("SELECT new org.teacherdistributionsystem.distribution_system.models.responses.teacher.TeacherResponse(" +
            "t.id, t.nom, t.prenom, t.email, t.codeSmartex, t.gradeCode, " +
            "t.participeSurveillance, t.quotaCredit) " +
            "FROM Teacher t")
    Page<TeacherResponse> getAllTeachers(Pageable pageable);


    @Query("SELECT t FROM Teacher t WHERE LOWER(t.prenom) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(t.nom) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Teacher> getAllContainsName(@Param("name") String name);

    @Query("SELECT NEW org.teacherdistributionsystem.distribution_system.models.responses.teacher.GradeCount(t.gradeCode, COUNT(t)) " +
            "FROM Teacher t GROUP BY t.gradeCode")
    List<GradeCount> countTeachersByGradeCode();
}
