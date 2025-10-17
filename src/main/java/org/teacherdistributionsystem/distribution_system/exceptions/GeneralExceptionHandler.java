package org.teacherdistributionsystem.distribution_system.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.teacherdistributionsystem.distribution_system.exceptions.custom.BadRequestException;
import org.teacherdistributionsystem.distribution_system.exceptions.custom.InvalidSearchParameterException;
import org.teacherdistributionsystem.distribution_system.models.responses.ErrorDetails;

import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
public class GeneralExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDetails> handleBadRequestException(BadRequestException e) {
        String message = e.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
       ErrorDetails error= ErrorDetails.builder().message(message).details(message).build();
        return ResponseEntity.status(status).body(error);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgumentException(IllegalArgumentException e) {
        String message = e.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorDetails error= ErrorDetails.builder().message(message).details(message).build();
        return ResponseEntity.status(status).body(error);
    }
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleEntityNotFoundException(EntityNotFoundException e) {
        String message = e.getMessage();
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorDetails error = ErrorDetails.builder()
                .message(message)
                .details(message)
                .build();
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(InvalidSearchParameterException.class)
    public ResponseEntity<ErrorDetails> handleInvalidSearchParameter(
            InvalidSearchParameterException ex) {
        ErrorDetails error =ErrorDetails.builder().details(ex.getDetails()).message(ex.getMessage()).build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorDetails> runtimeExceptionHandler(RuntimeException ex) {

        String message = "An unexpected error occurred";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        Throwable cause = ex.getCause();
        if(cause ==null) {
            cause = ex;
        }
        if (ex instanceof DataIntegrityViolationException) {
            message = "Database integrity violation - likely due to a constraint failure.";
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof PropertyValueException) {
            message = "A required field is missing or null.";
            status = HttpStatus.BAD_REQUEST;
        } else if (cause instanceof ConstraintViolationException) {
            message = "A database constraint was violated. Check unique or foreign key constraints.";
            status = HttpStatus.BAD_REQUEST;
        } else if (cause instanceof SQLIntegrityConstraintViolationException) {
            message = "SQL integrity constraint violated - likely a duplicate or foreign key issue.";
            status = HttpStatus.BAD_REQUEST;
        } else if (cause instanceof InvalidFormatException) {
            message = "An invalid format occurred";
            status = HttpStatus.BAD_REQUEST;

        }
        else if (cause instanceof NullPointerException) {
            message = "A null value been provided";
            status = HttpStatus.BAD_REQUEST;

        }
        ErrorDetails errorDetails = ErrorDetails.builder()
                .message(message)
                .details(ex.getMessage())
                .build();

        return new ResponseEntity<>(errorDetails, status);
    }
}
