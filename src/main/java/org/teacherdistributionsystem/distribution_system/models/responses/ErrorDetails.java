package org.teacherdistributionsystem.distribution_system.models.responses;



import lombok.*;

@Getter
@Builder
@Setter
public class ErrorDetails {
    private  String message;
    private  String details;
}