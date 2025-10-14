package org.teacherdistributionsystem.distribution_system.enums;


import lombok.Getter;

@Getter


public enum GradeType {
    PES("Professeur de l’Enseignement Supérieur", 8, 0),
    PR("Professeur", 8, 1),
    MC("Maître de Conférences", 8, 2),
    MA("Maître Assistant", 6, 3),
    PTC("Professeur Technologue", 6, 3),
    EX("Examinateur Externe", 7, 4),
    AC("Assistant Contractuel", 6, 5),
    AS("Assistant", 6, 6),
    VA("Vacataire Associé", 5, 8),
    V("Vacataire", 5, 9);


    private final String label;
    private final int defaultQuota;
    private final int priority;

    GradeType(String label, int defaultQuota, int priority) {
        this.label = label;
        this.defaultQuota = defaultQuota;
        this.priority = priority;
    }

    public static GradeType fromCode(String code) {
        return GradeType.valueOf(code);
    }
}

