package org.teacherdistributionsystem.distribution_system.entities.teacher;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grade_types")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Grade {
    @Id
    @Column(name = "grade_code", length = 10)
    private String gradeCode;

    @Column(name = "grade_libelle", length = 50, nullable = false)
    private String gradeLibelle;

    @Column(name = "default_quota_per_session", nullable = false)
    private Integer defaultQuotaPerSession;

    @Column(name = "priority_level", columnDefinition = "INTEGER DEFAULT 0")
    private Integer priorityLevel = 0;

}
