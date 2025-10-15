package org.teacherdistributionsystem.distribution_system.enums;

import lombok.Getter;

@Getter
public enum GradeType {
    PES("Professeur de l'Enseignement Supérieur"),
    PR("Professeur"),
    MC("Maître de Conférences"),
    MA("Maître Assistant"),
    PTC("Professeur Technologue"),
    EX("Examinateur Externe"),
    AC("Assistant Contractuel"),
    AS("Assistant"),
    VA("Vacataire Associé"),
    V("Vacataire");

    private final String label;

    GradeType(String label) {
        this.label = label;
    }

    public static GradeType fromCode(String code) {
        return GradeType.valueOf(code);
    }
}