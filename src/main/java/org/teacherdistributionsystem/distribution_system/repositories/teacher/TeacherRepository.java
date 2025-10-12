package org.teacherdistributionsystem.distribution_system.repositories.teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherNameProjection;

import java.util.List;
import java.util.Map;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Teacher findByNomAndPrenom(String nom, String prenom);

    Teacher findByCodeSmartex(Integer codeSmartex);
    @Query("SELECT t.id FROM Teacher t")
    List<Long> getAllIds();
    @Query("SELECT t.id, t.participeSurveillance FROM Teacher t")
    List<Object[]> getAllParticipants();
    @Query("SELECT t.id, t.gradeCode FROM Teacher t")
    List<Object[]> getAllGrades();

    @Query("SELECT t.id AS id, t.nom AS nom, t.prenom AS prenom FROM Teacher t")
    List<TeacherNameProjection> getAllNames();
}
