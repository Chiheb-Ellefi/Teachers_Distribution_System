package org.teacherdistributionsystem.distribution_system.models.requests;

import lombok.Getter;

@Getter
public class MainRequestBody {
    private String examDataFilePath;
    private String teachersUnavailabilityFilePath;
    private String teachersListFilePath;
}
