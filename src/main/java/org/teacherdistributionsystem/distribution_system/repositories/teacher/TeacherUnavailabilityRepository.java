package org.teacherdistributionsystem.distribution_system.repositories.teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherUnavailability;
@Repository
public interface TeacherUnavailabilityRepository extends JpaRepository<TeacherUnavailability, Long> {
}
