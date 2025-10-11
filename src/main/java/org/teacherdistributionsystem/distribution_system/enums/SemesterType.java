package org.teacherdistributionsystem.distribution_system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SemesterType {
    S1("SEMESTRE 1"),
    S2("SEMESTRE 2");
    private final String label;
    public static SemesterType fromLabel(String label) {
        for (SemesterType semesterType : SemesterType.values()) {
            if (semesterType.label.equals(label)) {
                return semesterType;
            }
        }
        throw new IllegalArgumentException("No semester found for label: " + label);
    }
}
