package org.teacherdistributionsystem.distribution_system.enums;

import lombok.Getter;

@Getter
public enum GradeType {
    PES("Professeur de l'Enseignement Secondaire"),
    PR("Professeur"),
    MC("Maître de Conférences"),
    MA("Maître Assistant"),
    PTC("Professeur Tronc Commun"),
    EX("Expert"),
    AC("Assistant Contractuel"),
    AS("Assistant"),
    V("Vacataire");

    private final String label;

    GradeType(String label) {
        this.label = label;
    }

    public static GradeType fromCode(String code) {
        return GradeType.valueOf(code);
    }
}