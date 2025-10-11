package org.teacherdistributionsystem.distribution_system.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherQuota;

@Repository
public interface TeacherQuotaRepository extends JpaRepository<TeacherQuota, Long> {
}
