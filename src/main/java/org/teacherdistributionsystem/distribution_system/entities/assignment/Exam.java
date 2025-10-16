package org.teacherdistributionsystem.distribution_system.entities.assignment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "exams")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher responsable;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "jour_numero", nullable = false)
    private Integer jourNumero;

    @Enumerated(EnumType.STRING)
    @Column( nullable = false)
    private SeanceType seance;

    @Column( nullable = false)
    private LocalTime startTime;

    @Column (nullable = false)
    private LocalTime endTime;


    @Column(name = "num_rooms", length = 10)
    private String numRooms;

    @Column(name = "required_supervisors", nullable = false)
    private Integer requiredSupervisors ;

    @Column(nullable = false,length = 20)
    private String examType;

}
