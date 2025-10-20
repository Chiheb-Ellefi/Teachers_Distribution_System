package org.teacherdistributionsystem.distribution_system.models.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public  class SwapRequest {
    private TeacherSwapRequest teacher1;
    private TeacherSwapRequest teacher2;


}