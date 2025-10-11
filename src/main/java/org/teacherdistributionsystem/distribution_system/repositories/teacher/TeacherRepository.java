package org.teacherdistributionsystem.distribution_system.repositories.teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Teacher findByNomAndPrenom(String nom, String prenom);

    Teacher findByCodeSmartex(Integer codeSmartex);
}
