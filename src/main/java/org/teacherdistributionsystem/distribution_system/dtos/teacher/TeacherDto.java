package org.teacherdistributionsystem.distribution_system.dtos.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDto {
    private Long id;
    private String prenom;
    private String nom;
    private String email;
    private Integer codeSmartex;
    private String gradeCode;
    private Boolean participeSurveillance;
}