package org.teacherdistributionsystem.distribution_system.entities.assignment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "exam_sessions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "seances_per_day", columnDefinition = "INTEGER DEFAULT 4") //S1, S2, S3 , S4
    private Integer seancesPerDay = 4;

    @Column(name = "status", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'DRAFT'") // DRAFT, OPTIMIZING, COMPLETED, ARCHIVED
    private String status = "DRAFT";

}
