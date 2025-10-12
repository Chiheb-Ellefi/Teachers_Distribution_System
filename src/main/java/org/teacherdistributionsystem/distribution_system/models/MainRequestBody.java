package org.teacherdistributionsystem.distribution_system.models;

import lombok.Getter;

@Getter
public class MainRequestBody {
    private String examDataFilePath;
    private String teachersUnavailabilityFilePath;
    private String teachersListFilePath;
}
