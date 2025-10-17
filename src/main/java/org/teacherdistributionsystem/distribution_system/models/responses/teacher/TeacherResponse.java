package org.teacherdistributionsystem.distribution_system.models.responses.teacher;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeacherResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private Integer codeSmartex;
    private String grade;
    private Boolean participeSurveillance;
    private Integer quota;
    private Integer credit;


    public TeacherResponse(Long id, String nom, String prenom, String email,
                           Integer codeSmartex, String gradeCode, Boolean participeSurveillance,
                           Integer quotaCredit) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.codeSmartex = codeSmartex;
        this.grade = gradeCode;
        this.participeSurveillance = participeSurveillance;
        this.quota = quotaCredit;
        this.credit = 0;
    }
    // Default constructor
    public TeacherResponse() {}

}