package org.teacherdistributionsystem.distribution_system.models.requests;

import lombok.Getter;

@Getter
public class TeacherSwapRequest {
    private Long teacherId;
    private Integer seance;
    private Integer day;
}
