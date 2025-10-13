package org.teacherdistributionsystem.distribution_system.entities.assignment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_exam_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherExamAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "exam_id", nullable = false)
    private String examId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "exam_day")
    private Integer examDay;

    @Column(name = "seance")
    private Integer seance;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "is_active")
    private Boolean isActive;
}