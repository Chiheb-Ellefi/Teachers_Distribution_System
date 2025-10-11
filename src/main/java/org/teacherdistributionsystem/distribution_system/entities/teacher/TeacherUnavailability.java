package org.teacherdistributionsystem.distribution_system.entities.teacher;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;

@Entity
@Table(name = "teacher_unavailability")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
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


}
