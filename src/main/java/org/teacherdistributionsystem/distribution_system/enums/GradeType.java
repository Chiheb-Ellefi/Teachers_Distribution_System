package org.teacherdistributionsystem.distribution_system.enums;


import lombok.Getter;

@Getter
public enum GradeType {
    PR("Professeur", 2, 0),
    PES("Professeur de l’Enseignement Supérieur", 2, 0),
    MC("Maître de Conférences", 3, 1),
    MA("Maître Assistant", 4, 2),
    PTC("Professeur Technologue", 4, 2),
    AC("Assistant Contractuel", 7, 3),
    AS("Assistant", 7, 3),
    EX("Examinateur externe", 7, 4),
    V("Vacataire", 7, 4),
    VA("Vacataire Associé", 7, 4);

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
