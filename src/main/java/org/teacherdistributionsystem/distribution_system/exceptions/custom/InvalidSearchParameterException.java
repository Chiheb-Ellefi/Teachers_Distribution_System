package org.teacherdistributionsystem.distribution_system.exceptions.custom;

import lombok.Getter;

@Getter
public class InvalidSearchParameterException extends RuntimeException {
    private String Details;
    public InvalidSearchParameterException(String message, String details) {
        super(message);
    }
}