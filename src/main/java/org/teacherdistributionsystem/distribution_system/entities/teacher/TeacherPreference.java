package org.teacherdistributionsystem.distribution_system.entities.teacher;

import jakarta.persistence.*;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;

import java.math.BigDecimal;


@Entity
@Table(name = "teacher_preferences")
public class TeacherPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession examSession;

    @Column(name = "preference_type", length = 50, nullable = false,columnDefinition = "VARCHAR(20) DEFAULT 'NOTHING'")
    private String preferenceType="NOTHING"; //'MORNING_PREFERRED', 'AVOID_CONSECUTIVE' ‘PREFER_WEEKEND_OFF’

    @Column(name = "priority_weight", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 1.0")
    private BigDecimal priorityWeight = BigDecimal.valueOf(1.0);
}
