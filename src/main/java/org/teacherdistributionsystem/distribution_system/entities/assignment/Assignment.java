package org.teacherdistributionsystem.distribution_system.entities.assignment;

import jakarta.persistence.*;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;

import java.time.LocalDate;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "jour_numero", nullable = false)
    private Integer jourNumero;

    @Enumerated(EnumType.STRING)
    @Column( nullable = false)
    private SeanceType seance;

    @Column(name = "is_subject_expert", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isSubjectExpert = false;

    @Column(name = "room_number", length = 20)
    private String roomNumber;

    @Column(name = "assignment_type", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'REGULAR'")
    private String assignmentType = "REGULAR";
}
