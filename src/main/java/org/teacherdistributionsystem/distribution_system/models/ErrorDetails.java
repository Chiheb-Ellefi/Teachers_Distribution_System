package org.teacherdistributionsystem.distribution_system.models;



import lombok.*;

@Getter
@Builder
@Setter
public class ErrorDetails {
    private  String message;
    private  String details;
}