package org.teacherdistributionsystem.distribution_system.entities.assignment;

import jakarta.persistence.*;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;

@Entity
@Table(name = "assignment_history")
public class AssignmentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession examSession;

    @Column(name = "total_seances_assigned", columnDefinition = "INTEGER DEFAULT 0")
    private Integer totalSeancesAssigned = 0;

    @Column(name = "early_morning_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer earlyMorningCount = 0;

    @Column(name = "late_afternoon_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer lateAfternoonCount = 0;

    @Column(name = "consecutive_days_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer consecutiveDaysCount = 0;

    @Column(name = "weekend_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer weekendCount = 0;
    @Column(name = "credit", columnDefinition = "INTEGER DEFAULT 0")
    private Integer credit = 0;
}
