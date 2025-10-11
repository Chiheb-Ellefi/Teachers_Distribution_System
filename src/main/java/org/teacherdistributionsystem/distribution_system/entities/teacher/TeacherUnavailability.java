package org.teacherdistributionsystem.distribution_system.entities.teacher;

import jakarta.persistence.*;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;

@Entity
@Table(name = "teacher_unavailability")
public class TeacherUnavailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "numero_jour", nullable = false)
    private Integer numeroJour;

    @Column(length = 10, nullable = false)
    private String seance;

    @Column(name = "is_absolute", columnDefinition = "BOOLEAN DEFAULT TRUE") // Hard constraint vs. soft preference
    private Boolean isAbsolute = true;

}
