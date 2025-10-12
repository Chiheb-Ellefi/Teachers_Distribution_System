package org.teacherdistributionsystem.distribution_system.repositories.teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherUnavailability;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilityProjection;

import java.util.List;

@Repository
public interface TeacherUnavailabilityRepository extends JpaRepository<TeacherUnavailability, Long> {
    @Query("SELECT t.id AS id, t.numeroJour AS numeroJour, t.seance AS seance " +
            "FROM TeacherUnavailability t WHERE t.teacher.id = ?1")
    List<TeacherUnavailabilityProjection> getTeacherUnavailableByTeacherId(Long teacherId);

}
