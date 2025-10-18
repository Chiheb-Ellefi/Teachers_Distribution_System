package org.teacherdistributionsystem.distribution_system.entities.teacher;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "teachers")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String prenom;

    @Column(length = 100, nullable = false)
    private String nom;

    @Column(length = 150, unique = true)
    private String email;

    @Column(name = "code_smartex", unique = true)
    private Integer codeSmartex;


    @JoinColumn(name = "grade_code", nullable = false)
    private String gradeCode;
    @Column(name = "participe_surveillance")
    private Boolean participeSurveillance ;

    @Column(name = "quota_credit")
    private Integer quotaCredit;




}
