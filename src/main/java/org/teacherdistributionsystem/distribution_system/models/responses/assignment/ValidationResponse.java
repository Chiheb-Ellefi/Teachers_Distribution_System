package org.teacherdistributionsystem.distribution_system.models.responses.assignment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public  class ValidationResponse {
    private String message;
    private String note;


}
