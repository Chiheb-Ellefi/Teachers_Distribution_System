package org.teacherdistributionsystem.distribution_system.config;

/**
 * Configuration for assignment algorithm constraints
 * Allows fine-tuning of constraint behavior: HARD (must follow) vs SOFT (prefer but allow)
 */
public class AssignmentConstraintConfig {

    // Constraint enforcement modes
    public enum ConstraintMode {
        DISABLED,  // Constraint not applied at all
        SOFT,      // Preference in objective function (can be violated)
        HARD       // Strict constraint (cannot be violated)
    }

    // ===== CORE CONSTRAINTS (Usually keep HARD) =====

    /**
     * Each exam must have exactly its required number of supervisors
     * Recommendation: HARD (always)
     */
    private ConstraintMode examCoverageMode = ConstraintMode.HARD;

    /**
     * Teachers marked as non-participating cannot be assigned
     * Recommendation: HARD (always)
     */
    private ConstraintMode participationMode = ConstraintMode.HARD;

    /**
     * Teachers cannot supervise exams they own
     * Recommendation: HARD (always)
     */
    private ConstraintMode ownershipExclusionMode = ConstraintMode.HARD;

    /**
     * Teachers cannot be assigned to time slots they marked as unavailable
     * Recommendation: HARD (but algorithm can relax progressively if needed)
     */
    private ConstraintMode unavailabilityMode = ConstraintMode.HARD;

    /**
     * Teachers cannot exceed their assigned quota
     * Recommendation: HARD (always)
     */
    private ConstraintMode quotaLimitMode = ConstraintMode.HARD;

    /**
     * Teachers cannot be in two places at once (one exam per time slot)
     * Recommendation: HARD (always)
     */
    private ConstraintMode timeConflictMode = ConstraintMode.HARD;

    // ===== FAIRNESS CONSTRAINTS (Configurable) =====

    /**
     * At least one exam owner should supervise another exam in the same time slot
     * Recommendation: SOFT (nice to have but can cause infeasibility)
     */
    private ConstraintMode ownerPresenceMode = ConstraintMode.SOFT;

    /**
     * Penalty weight for owner presence violations (only used if SOFT)
     */
    private int ownerPresencePenalty = 2;

    /**
     * Teachers should work in consecutive blocks without gaps in their daily schedule
     * Recommendation: SOFT or DISABLED (can cause infeasibility with unavailability)
     */
    private ConstraintMode noGapsMode = ConstraintMode.DISABLED;

    /**
     * Penalty weight for gap violations (only used if SOFT)
     */
    private int noGapsPenalty = 5;

    /**
     * When enforcing no-gaps, skip teachers who have unavailability that day
     * Only applies if noGapsMode = HARD
     */
    private boolean noGapsSkipUnavailableTeachers = true;

    // ===== OPTIMIZATION PREFERENCES =====

    /**
     * Prefer available teachers for high-conflict time slots
     */
    private boolean optimizeConflictAvoidance = true;

    /**
     * Weight for conflict avoidance in objective
     */
    private int conflictAvoidancePenalty = 1;

    /**
     * Heavy penalty for assigning relaxed teachers to originally unavailable slots
     */
    private int unavailabilityViolationPenalty = 1000;
    private int fairnessWeight = 1000;

    public int getFairnessWeight() {
        return fairnessWeight;
    }

    public void setFairnessWeight(int fairnessWeight) {
        this.fairnessWeight = fairnessWeight;
    }
    // ===== GETTERS AND SETTERS =====

    public ConstraintMode getExamCoverageMode() {
        return examCoverageMode;
    }

    public void setExamCoverageMode(ConstraintMode examCoverageMode) {
        this.examCoverageMode = examCoverageMode;
    }

    public ConstraintMode getParticipationMode() {
        return participationMode;
    }

    public void setParticipationMode(ConstraintMode participationMode) {
        this.participationMode = participationMode;
    }

    public ConstraintMode getOwnershipExclusionMode() {
        return ownershipExclusionMode;
    }

    public void setOwnershipExclusionMode(ConstraintMode ownershipExclusionMode) {
        this.ownershipExclusionMode = ownershipExclusionMode;
    }

    public ConstraintMode getUnavailabilityMode() {
        return unavailabilityMode;
    }

    public void setUnavailabilityMode(ConstraintMode unavailabilityMode) {
        this.unavailabilityMode = unavailabilityMode;
    }

    public ConstraintMode getQuotaLimitMode() {
        return quotaLimitMode;
    }

    public void setQuotaLimitMode(ConstraintMode quotaLimitMode) {
        this.quotaLimitMode = quotaLimitMode;
    }

    public ConstraintMode getTimeConflictMode() {
        return timeConflictMode;
    }

    public void setTimeConflictMode(ConstraintMode timeConflictMode) {
        this.timeConflictMode = timeConflictMode;
    }

    public ConstraintMode getOwnerPresenceMode() {
        return ownerPresenceMode;
    }

    public void setOwnerPresenceMode(ConstraintMode ownerPresenceMode) {
        this.ownerPresenceMode = ownerPresenceMode;
    }

    public int getOwnerPresencePenalty() {
        return ownerPresencePenalty;
    }

    public void setOwnerPresencePenalty(int ownerPresencePenalty) {
        this.ownerPresencePenalty = ownerPresencePenalty;
    }

    public ConstraintMode getNoGapsMode() {
        return noGapsMode;
    }

    public void setNoGapsMode(ConstraintMode noGapsMode) {
        this.noGapsMode = noGapsMode;
    }

    public int getNoGapsPenalty() {
        return noGapsPenalty;
    }

    public void setNoGapsPenalty(int noGapsPenalty) {
        this.noGapsPenalty = noGapsPenalty;
    }

    public boolean isNoGapsSkipUnavailableTeachers() {
        return noGapsSkipUnavailableTeachers;
    }

    public void setNoGapsSkipUnavailableTeachers(boolean noGapsSkipUnavailableTeachers) {
        this.noGapsSkipUnavailableTeachers = noGapsSkipUnavailableTeachers;
    }

    public boolean isOptimizeConflictAvoidance() {
        return optimizeConflictAvoidance;
    }

    public void setOptimizeConflictAvoidance(boolean optimizeConflictAvoidance) {
        this.optimizeConflictAvoidance = optimizeConflictAvoidance;
    }

    public int getConflictAvoidancePenalty() {
        return conflictAvoidancePenalty;
    }

    public void setConflictAvoidancePenalty(int conflictAvoidancePenalty) {
        this.conflictAvoidancePenalty = conflictAvoidancePenalty;
    }

    public int getUnavailabilityViolationPenalty() {
        return unavailabilityViolationPenalty;
    }

    public void setUnavailabilityViolationPenalty(int unavailabilityViolationPenalty) {
        this.unavailabilityViolationPenalty = unavailabilityViolationPenalty;
    }

    // ===== PRESET CONFIGURATIONS =====

    /**
     * Default configuration - balanced between feasibility and fairness
     */
    public static AssignmentConstraintConfig defaultConfig() {
        AssignmentConstraintConfig config = new AssignmentConstraintConfig();
        config.setFairnessWeight(1000); // Fairness is HIGH priority
        return config;
    }

    /**
     * Strict configuration - all fairness constraints as HARD
     * Warning: May cause infeasibility
     */
    public static AssignmentConstraintConfig strictConfig() {
        AssignmentConstraintConfig config = new AssignmentConstraintConfig();
        config.setOwnerPresenceMode(ConstraintMode.HARD);
        config.setNoGapsMode(ConstraintMode.HARD);
        config.setNoGapsSkipUnavailableTeachers(true);
        config.setFairnessWeight(1000);
        return config;
    }

    /**
     * Relaxed configuration - all fairness constraints as SOFT or DISABLED
     * Guarantees feasibility if capacity is sufficient
     */
    public static AssignmentConstraintConfig relaxedConfig() {
        AssignmentConstraintConfig config = new AssignmentConstraintConfig();
        config.setOwnerPresenceMode(ConstraintMode.SOFT);
        config.setNoGapsMode(ConstraintMode.DISABLED);
        return config;
    }

    /**
     * Maximum fairness - all fairness as SOFT preferences
     * Good balance between fairness and feasibility
     */
    public static AssignmentConstraintConfig fairnessOptimizedConfig() {
        AssignmentConstraintConfig config = new AssignmentConstraintConfig();
        config.setOwnerPresenceMode(ConstraintMode.SOFT);
        config.setOwnerPresencePenalty(10);
        config.setNoGapsMode(ConstraintMode.SOFT);
        config.setNoGapsPenalty(5);
        return config;
    }
}