package org.teacherdistributionsystem.distribution_system.repositories.teacher;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherQuota;

import java.util.List;
import java.util.Map;

@Repository
public interface TeacherQuotaRepository extends JpaRepository<TeacherQuota, Long> {
    @Query(value = "SELECT q.id, q.assignedQuota FROM TeacherQuota q")
    List<Object[]> getTeacherQuotaAndId();

    @Modifying
    @Transactional
    @Query("UPDATE TeacherQuota t SET t.assignedQuota = :quota WHERE t.id = :teacherId")
    void updateTeacherQuotaById(@Param("teacherId") Long teacherId,@Param("quota") Integer quota);
}
