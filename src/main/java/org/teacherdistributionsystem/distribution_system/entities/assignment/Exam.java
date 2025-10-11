package org.teacherdistributionsystem.distribution_system.entities.assignment;

import jakarta.persistence.*;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Subject;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "exams")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "jour_numero", nullable = false)
    private Integer jourNumero;

    @Column(length = 10, nullable = false)
    private String seance;

    @Column(name = "num_students", nullable = false) ///--------------------- Optional--------------------
    private Integer numStudents;

    @Column(name = "num_rooms", length = 10)///--------------------- Optional--------------------
    private String numRooms;

    @Column(name = "required_supervisors", nullable = false, columnDefinition = "INTEGER DEFAULT 2")
    private Integer requiredSupervisors = 2;

    @Column(name = "subject_experts_required", columnDefinition = "INTEGER DEFAULT 1")
    private Integer subjectExpertsRequired = 1;

    @Column(name = "exam_duration_hours", precision = 3, scale = 1, columnDefinition = "DECIMAL(3,1) DEFAULT 1.5")
    private BigDecimal examDurationHours = BigDecimal.valueOf(1.5);

    @Column(name = "difficulty_level", columnDefinition = "INTEGER DEFAULT 1")
    private Integer difficultyLevel = 1;//difficulty level to see if it needs more supervision than other exams when there is some sup aside 1=easy, 2=medium, 3=hard

}
