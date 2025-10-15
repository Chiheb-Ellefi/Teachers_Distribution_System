package org.teacherdistributionsystem.distribution_system.entities.assignment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Entity
@Table(name = "exam_sessions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class ExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year", length = 20, nullable = false) // 2025/2026
    private String academicYear;

    @Column(name = "semester_code", length = 10, nullable = false) // S1 , S2
    private String semesterCode;

    @Column(name = "semester_libelle", length = 50) // Semestre 1
    private String semesterLibelle;

    @Column(name = "session_libelle", length = 50, nullable = false) // Principale, Rattrapage
    private String sessionLibelle;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "num_exam_days", nullable = false) // 10
    private Integer numExamDays;

    @Column(name = "seances_per_day")
    @ColumnDefault("4")
    private Integer seancesPerDay;

    @Column(name = "teachers_per_exam")
    private Integer teachersPerExam ;



}
