package org.teacherdistributionsystem.distribution_system.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
}
