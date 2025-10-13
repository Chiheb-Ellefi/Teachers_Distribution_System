package org.teacherdistributionsystem.distribution_system.enums;


import lombok.Getter;

@Getter
public enum GradeType {
    PR("Professeur", 4, 0),
    PES("Professeur de l’Enseignement Supérieur", 4, 0),
    MC("Maître de Conférences", 6, 1),
    MA("Maître Assistant", 6, 2),
    PTC("Professeur Technologue", 7, 2),
    AC("Assistant Contractuel", 10, 3),
    AS("Assistant", 7, 5),
    EX("Examinateur externe", 10, 4),
    V("Vacataire", 7, 10),
    VA("Vacataire Associé", 10, 4);

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
