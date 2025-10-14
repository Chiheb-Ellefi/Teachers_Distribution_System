package org.teacherdistributionsystem.distribution_system.enums;


import lombok.Getter;

@Getter
// TESTING CONFIGURATION: Dramatically reduce quotas to force infeasibility
// This will test if the cascading relaxation works for low-priority teachers

public enum GradeType {
    PR("Professeur", 8, 0),                           // 8 × 8 = 64
    PES("Professeur de l'Enseignement Supérieur", 8, 0),
    MC("Maître de Conférences", 8, 1),                // 3 × 8 = 24
    MA("Maître Assistant", 6, 2),                     // 48 × 6 = 288
    PTC("Professeur Technologue", 6, 2),
    AC("Assistant Contractuel", 6, 3),                // 8 × 6 = 48
    AS("Assistant", 6, 5),                            // 3 × 6 = 18
    EX("Examinateur externe", 7, 4),                  // 3 × 7 = 21
    V("Vacataire", 5, 10),                            // 14 × 5 = 70
    VA("Vacataire Associé", 5, 10);// Keep at 10

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

/*
EXPECTED BEHAVIOR WITH THESE QUOTAS:

Current situation:
- 263 exams × 2 teachers = 526 supervisions needed
- With reduced quotas, total capacity will be < 526
- This creates artificial scarcity

Priority Distribution (from your logs):
- Priority 0: 8 teachers  × 3 quota = 24 supervisions
- Priority 1: 3 teachers  × 4 quota = 12 supervisions
- Priority 2: 48 teachers × 3-4 quota = ~168 supervisions
- Priority 3: 8 teachers  × 4 quota = 32 supervisions
- Priority 4: 3 teachers  × 6 quota = 18 supervisions
- Priority 5: 3 teachers  × 4 quota = 12 supervisions
- Priority 10: 14 teachers × 6-10 quota = ~112 supervisions
Total: ~378 supervisions (vs 526 needed) ❌ INSUFFICIENT

What should happen:
1. [ATTEMPT 1] STRICT MODE → INFEASIBLE (not enough capacity + unavailability)
2. [ATTEMPT 2] Relax priority > 2 → Still might be tight
3. [ATTEMPT 3] Relax priority > 5 → Should help more
4. [ATTEMPT 4] Relax priority > 10 → If needed (unlikely since V/VA have high quotas)

The algorithm should be forced to use Vacataires (V) and Vacataires Associés (VA)
even when they declared unavailability, to make the solution feasible.
*/