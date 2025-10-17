package org.teacherdistributionsystem.distribution_system.models.responses.teacher;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradeCount {
    private String grade;
    private Long nbr;
    private Long percentage;

    public GradeCount(String grade, Long nbr) {
        this.grade = grade;
        this.nbr = nbr;
        this.percentage = 0L;
    }
}
