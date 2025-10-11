package org.teacherdistributionsystem.distribution_system.entities.teacher;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;

@Entity
@Table(name = "teacher_quotas")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherQuota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "assigned_quota", nullable = false)
    private Integer assignedQuota;

    @Column(name = "quota_type", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'STANDARD'") // STANDARD, REDUCED, INCREASED
    private String quotaType = "STANDARD";

    @Column(length = 255)
    private String reason; //if its reduced or increased why
}
