package org.teacherdistributionsystem.distribution_system.exceptions;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    private String details;
    public BadRequestException(String message, String details) {
        super(message);
    }
}
