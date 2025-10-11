package org.teacherdistributionsystem.distribution_system.entities.assignment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;

import java.sql.Time;
import java.time.LocalDate;
import java.util.Date;

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
    private String examDate;

    @Column(name = "jour_numero", nullable = false)
    private Integer jourNumero;

    @Column(length = 10, nullable = false)
    private String seance;
    @Column( nullable = false)
    private String startTime;

    @Column (nullable = false)
    private String endTime;


    @Column(name = "num_rooms", length = 10)
    private String numRooms;

    @Column(name = "required_supervisors", nullable = false, columnDefinition = "INTEGER DEFAULT 2")
    private Integer requiredSupervisors = 2;

    @Column(nullable = false,length = 20)
    private String examType;

}
