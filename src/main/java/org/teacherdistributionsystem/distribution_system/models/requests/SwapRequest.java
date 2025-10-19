package org.teacherdistributionsystem.distribution_system.models.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public  class SwapRequest {
    private Long assignmentId1;
    private Long assignmentId2;


}