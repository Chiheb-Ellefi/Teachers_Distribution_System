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
    private Integer codeSmartex;  // Changed to Integer
    private String grade;
    private Boolean participeSurveillance;
    private Integer quota;
    private Integer credit;



    public TeacherResponse(Long id, String nom, String prenom, String email,
                           Integer codeSmartex, String grade, Boolean participeSurveillance,Integer credit) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.codeSmartex = codeSmartex;
        this.grade = grade;
        this.participeSurveillance = participeSurveillance;
        this.credit = credit;
    }

    // Default constructor
    public TeacherResponse() {}

}