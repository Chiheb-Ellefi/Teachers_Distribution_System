package org.teacherdistributionsystem.distribution_system.models.responses.assignment;

import lombok.Getter;
import lombok.Setter;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;


@Getter
@Setter
public  class SwapDetails {
    private AssignmentInfo assignment1;
    private AssignmentInfo assignment2;

    public SwapDetails(TeacherExamAssignment a1, TeacherExamAssignment a2) {
        this.assignment1 = new AssignmentInfo(a1);
        this.assignment2 = new AssignmentInfo(a2);
    }


}
