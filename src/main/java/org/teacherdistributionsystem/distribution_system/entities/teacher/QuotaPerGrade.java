package org.teacherdistributionsystem.distribution_system.entities.teacher;

import jakarta.persistence.*;
import lombok.*;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;

@Entity
@Table(name = "quota_per_grade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaPerGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, unique = true)
    private GradeType grade;

    @Column(name = "default_quota", nullable = false)
    private Integer defaultQuota;

    @Column(name = "priority", nullable = false)
    private Integer priority;
}