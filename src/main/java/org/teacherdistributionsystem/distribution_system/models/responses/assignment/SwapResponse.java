package org.teacherdistributionsystem.distribution_system.models.responses.assignment;

import lombok.Getter;
import lombok.Setter;


import java.util.List;

@Getter
@Setter
public  class SwapResponse {
    private boolean success;
    private String message;
    private List<String> violations;
    private SwapDetails data;

    public static SwapResponse success(SwapResult result) {
        SwapResponse response = new SwapResponse();
        response.success = true;
        response.message = result.getMessage();
        response.violations = new java.util.ArrayList<>();
        response.data = result.getDetails();
        return response;
    }

    public static SwapResponse failure(SwapResult result) {
        SwapResponse response = new SwapResponse();
        response.success = false;
        response.message = result.getMessage();
        response.violations = result.getViolations();
        response.data = null;
        return response;
    }

    public static SwapResponse error(String message) {
        SwapResponse response = new SwapResponse();
        response.success = false;
        response.message = message;
        response.violations = new java.util.ArrayList<>();
        response.data = null;
        return response;
    }

}
