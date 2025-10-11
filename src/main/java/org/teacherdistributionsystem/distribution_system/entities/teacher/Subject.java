package org.teacherdistributionsystem.distribution_system.entities.teacher;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subjects")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, unique = true, nullable = false)
    private String code;

    @Column(length = 200, nullable = false)
    private String libelle;

    @Column(length = 100)
    private String department;

    @Column(length = 50) //'L1', 'L2', 'L3', 'M1', 'M2', ‘ING1’
    private String level;
}
