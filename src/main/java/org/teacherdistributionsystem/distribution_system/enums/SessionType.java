package org.teacherdistributionsystem.distribution_system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SessionType {
    P("PRINCIPALE"),
    R("RATTRAPAGE");
    private final String label;
}
