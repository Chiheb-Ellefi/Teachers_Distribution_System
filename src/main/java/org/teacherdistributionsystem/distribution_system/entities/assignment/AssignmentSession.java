package org.teacherdistributionsystem.distribution_system.entities.assignment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "assignment_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_session_id", nullable = false, unique = true)
    private Long examSessionId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    @Column(name = "is_optimal")
    private Boolean isOptimal;

    @Column(name = "total_assignments")
    private Integer totalAssignments;

    @Column(name = "solution_time_seconds")
    private Double solutionTimeSeconds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}