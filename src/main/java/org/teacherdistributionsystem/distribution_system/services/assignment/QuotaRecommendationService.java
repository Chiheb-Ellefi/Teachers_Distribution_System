package org.teacherdistributionsystem.distribution_system.services.assignment;

import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;
import org.teacherdistributionsystem.distribution_system.models.projections.ExamForAssignmentProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilityProjection;
import org.teacherdistributionsystem.distribution_system.services.teacher.QuotaPerGradeService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherQuotaService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherUnavailabilityService;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.ExamSessionDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuotaRecommendationService {

    private final TeacherService teacherService;
    private final TeacherQuotaService teacherQuotaService;
    private final TeacherUnavailabilityService teacherUnavailabilityService;
    private final ExamSessionService examSessionService;
    private final ExamService examService;
    private final QuotaPerGradeService quotaPerGradeService;

    public QuotaRecommendationService(TeacherService teacherService,
                                      TeacherQuotaService teacherQuotaService,
                                      TeacherUnavailabilityService teacherUnavailabilityService,
                                      ExamSessionService examSessionService,
                                      ExamService examService,
                                      QuotaPerGradeService quotaPerGradeService) {
        this.teacherService = teacherService;
        this.teacherQuotaService = teacherQuotaService;
        this.teacherUnavailabilityService = teacherUnavailabilityService;
        this.examSessionService = examSessionService;
        this.examService = examService;
        this.quotaPerGradeService = quotaPerGradeService;
    }

    /**
     * Analyzes current quota distribution and recommends fair quotas per grade
     */
    public QuotaRecommendationResponse analyzeAndRecommendQuotas(Long sessionId) {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║   QUOTA FAIRNESS ANALYSIS & RECOMMENDATIONS    ║");
        System.out.println("╔════════════════════════════════════════════════╗\n");

        // Load session data
        ExamSessionDto session = examSessionService.getExamSessionDto(sessionId);

        // Calculate total supervision needs
        List<ExamForAssignmentProjection> exams = examService.getExamsForAssignment(sessionId);
        int totalSupervisionNeeded = calculateTotalSupervisionNeeded(exams);

        System.out.println("📊 Session Overview:");
        System.out.println("   Session: " + session.getSessionLibelle());
        System.out.println("   Total supervisions needed: " + totalSupervisionNeeded);
        System.out.println();

        // Analyze teachers by grade
        Map<GradeType, GradeAnalysis> gradeAnalysis = analyzeTeachersByGrade(sessionId, session);

        // Calculate current vs recommended quotas
        QuotaRecommendations recommendations = calculateRecommendedQuotas(
                gradeAnalysis, totalSupervisionNeeded, sessionId);

        // Generate detailed report
        return buildRecommendationResponse(session, gradeAnalysis, recommendations, totalSupervisionNeeded);
    }

    /**
     * Calculate total supervision slots needed
     */
    private int calculateTotalSupervisionNeeded(List<ExamForAssignmentProjection> exams) {
        // Group by logical exam (day + seance + room) to avoid duplicates
        Map<String, Integer> uniqueExams = new HashMap<>();

        for (ExamForAssignmentProjection exam : exams) {
            String key = exam.getJourNumero() + "_" + exam.getSeance() + "_" + exam.getNumRooms();
            uniqueExams.putIfAbsent(key, exam.getRequiredSupervisors());
        }

        return uniqueExams.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Analyze teachers grouped by grade
     */
    private Map<GradeType, GradeAnalysis> analyzeTeachersByGrade(Long sessionId, ExamSessionDto session) {
        Map<Long, Boolean> participationMap = teacherService.getTeacherParticipeSurveillance();
        Map<Long, String> gradeMap = teacherService.getAllGrades();
        Map<Long, Integer> quotaMap = teacherQuotaService.getAllQuotas(sessionId);

        // Get unavailability data
        List<TeacherUnavailabilityProjection> unavailabilityList =
                teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(sessionId);

        Map<Long, Set<String>> unavailabilityByTeacher = new HashMap<>();
        for (TeacherUnavailabilityProjection u : unavailabilityList) {
            String slot = u.getNumeroJour() + "_" + u.getSeance();
            unavailabilityByTeacher.computeIfAbsent(u.getId(), k -> new HashSet<>()).add(slot);
        }

        int totalTimeSlots = session.getNumExamDays() * SeanceType.values().length;

        // Group by grade
        Map<GradeType, GradeAnalysis> analysis = new HashMap<>();

        for (Map.Entry<Long, String> entry : gradeMap.entrySet()) {
            Long teacherId = entry.getKey();

            // Skip non-participating teachers
            if (!participationMap.getOrDefault(teacherId, false)) {
                continue;
            }

            try {
                GradeType grade = GradeType.valueOf(entry.getValue());
                int quota = quotaMap.getOrDefault(teacherId, 0);
                int unavailableSlots = unavailabilityByTeacher.getOrDefault(teacherId, Collections.emptySet()).size();
                double availabilityRate = totalTimeSlots > 0 ?
                        ((totalTimeSlots - unavailableSlots) * 100.0) / totalTimeSlots : 100.0;

                GradeAnalysis gradeAnalysis = analysis.computeIfAbsent(grade,
                        k -> new GradeAnalysis(grade));

                gradeAnalysis.addTeacher(teacherId, quota, unavailableSlots, availabilityRate);

            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Invalid grade for teacher " + teacherId);
            }
        }

        return analysis;
    }
    /**
     * Calculate priority multipliers for each grade
     * STRONGER VERSION: More differentiation between priorities
     * Priority 1 (highest) = 0.5x multiplier (half the work)
     * Priority 5 (middle) = 1.0x multiplier (baseline)
     * Priority 9 (lowest) = 1.5x multiplier (50% more work)
     */
    private Map<GradeType, Double> calculatePriorityMultipliers(
            Map<GradeType, GradeAnalysis> gradeAnalysis,
            Map<GradeType, Integer> currentPriorities) {

        Map<GradeType, Double> multipliers = new HashMap<>();

        // Find min and max priorities
        int minPriority = currentPriorities.values().stream()
                .min(Integer::compareTo)
                .orElse(1);
        int maxPriority = currentPriorities.values().stream()
                .max(Integer::compareTo)
                .orElse(9);

        // Calculate range for normalization
        int priorityRange = maxPriority - minPriority;

        if (priorityRange == 0) {
            // All priorities are the same, use multiplier of 1.0 for all
            for (GradeType grade : gradeAnalysis.keySet()) {
                multipliers.put(grade, 1.0);
            }
            return multipliers;
        }

        // IMPROVED: Stronger multiplier range for more differentiation
        // Formula: multiplier = 0.5 + (1.0 * normalizedPriority)
        // This gives range from 0.5 (high priority) to 1.5 (low priority)
        // That's a 3x difference between highest and lowest priority!
        for (Map.Entry<GradeType, GradeAnalysis> entry : gradeAnalysis.entrySet()) {
            GradeType grade = entry.getKey();
            int priority = currentPriorities.getOrDefault(grade, 5);

            // Normalize priority to 0-1 range
            double normalizedPriority = (double) (priority - minPriority) / priorityRange;

            // Calculate multiplier with STRONGER differentiation
            double multiplier = 0.5 + (1.0 * normalizedPriority);

            multipliers.put(grade, multiplier);
        }

        return multipliers;
    }


    /**
     * Calculate recommended quotas using multiple fairness strategies
     * FIXED VERSION: Priorities now have much stronger weight
     */
    private QuotaRecommendations calculateRecommendedQuotas(
            Map<GradeType, GradeAnalysis> gradeAnalysis,
            int totalSupervisionNeeded,
            Long sessionId) {

        QuotaRecommendations recommendations = new QuotaRecommendations();

        int totalParticipatingTeachers = gradeAnalysis.values().stream()
                .mapToInt(g -> g.teacherCount)
                .sum();

        if (totalParticipatingTeachers == 0) {
            return recommendations;
        }

        System.out.println("📈 Analysis by Grade:\n");

        // Get current priorities and default quotas
        Map<GradeType, Integer> currentPriorities = quotaPerGradeService.getPrioritiesByGrade();
        Map<GradeType, Integer> defaultQuotas = quotaPerGradeService.getDefaultQuotasByGrade();

        // Calculate priority-based weights (STRONGER VERSION)
        Map<GradeType, Double> priorityMultipliers = calculatePriorityMultipliers(
                gradeAnalysis, currentPriorities);

        // Calculate baseline quota (if all grades were equal)
        double baselineQuota = (double) totalSupervisionNeeded / totalParticipatingTeachers;

        System.out.println("🎯 Priority System Analysis:");
        System.out.println("   Baseline quota (if all equal): " + String.format("%.1f", baselineQuota));
        System.out.println("   Priority scale: 1 (highest/less work) → 9 (lowest/more work)");
        System.out.println("   Multiplier range: 0.5x (priority 1) to 1.5x (priority 9)\n");

        // IMPORTANT: Calculate total weighted capacity to ensure we meet supervision needs
        double totalWeightedCapacity = gradeAnalysis.entrySet().stream()
                .mapToDouble(e -> {
                    GradeType grade = e.getKey();
                    GradeAnalysis analysis = e.getValue();
                    double multiplier = priorityMultipliers.get(grade);
                    return baselineQuota * multiplier * analysis.teacherCount;
                })
                .sum();

        // Adjustment factor to ensure total capacity meets needs
        double adjustmentFactor = totalSupervisionNeeded / totalWeightedCapacity;

        System.out.println("   Total weighted capacity: " + String.format("%.1f", totalWeightedCapacity));
        System.out.println("   Adjustment factor: " + String.format("%.3f", adjustmentFactor) + "\n");

        for (Map.Entry<GradeType, GradeAnalysis> entry : gradeAnalysis.entrySet()) {
            GradeType grade = entry.getKey();
            GradeAnalysis analysis = entry.getValue();

            // Calculate metrics
            double avgAvailability = analysis.getAverageAvailability();
            double currentAvgQuota = analysis.getAverageQuota();
            int currentTotalQuota = analysis.getTotalQuota();
            int priority = currentPriorities.getOrDefault(grade, 5);
            int defaultQuota = defaultQuotas.getOrDefault(grade, (int) baselineQuota);
            double priorityMultiplier = priorityMultipliers.get(grade);
            double equalShareQuota = baselineQuota;

            // STRATEGY 1: Priority-based distribution (ADJUSTED TO MEET TOTAL NEEDS)
            double priorityBasedQuota = baselineQuota * priorityMultiplier * adjustmentFactor;

            // STRATEGY 2: Availability-adjusted with priority
            // Apply MINOR adjustment for availability (only ±10%)
            double availabilityWeight = avgAvailability / 100.0;
            double availabilityAdjustedQuota = priorityBasedQuota * (0.95 + 0.10 * availabilityWeight);

            // STRATEGY 3: Capacity-based (considering unavailability)
            double capacityAdjustedQuota = priorityBasedQuota;
            if (avgAvailability < 70) {
                capacityAdjustedQuota *= 0.90; // 10% reduction for very low availability
            } else if (avgAvailability < 85) {
                capacityAdjustedQuota *= 0.95; // 5% reduction for low availability
            }

            // STRATEGY 4: Default quota adjusted for capacity (minor weight)
            double scaleFactor = (double) totalSupervisionNeeded /
                    (gradeAnalysis.values().stream()
                            .mapToDouble(g -> defaultQuotas.getOrDefault(g.grade, 10) * g.teacherCount)
                            .sum());
            double scaledDefaultQuota = defaultQuota * scaleFactor;

            // STRATEGY 5: PRIORITY-FOCUSED (RECOMMENDED)
            // NEW WEIGHTS: Priority dominates (70%), availability (15%), capacity (10%), default (5%)
            double recommendedQuota = Math.round(
                    priorityBasedQuota * 0.70 +           // INCREASED from 50% to 70%
                            availabilityAdjustedQuota * 0.15 +  // DECREASED from 20% to 15%
                            capacityAdjustedQuota * 0.10 +      // DECREASED from 20% to 10%
                            scaledDefaultQuota * 0.05           // DECREASED from 10% to 5%
            );

            // Ensure minimum quota of 1 for participating teachers
            recommendedQuota = Math.max(1, recommendedQuota);

            // Calculate change from current
            double quotaChange = recommendedQuota - currentAvgQuota;
            double changePercentage = currentAvgQuota > 0 ?
                    (quotaChange * 100.0) / currentAvgQuota : 0;

            GradeQuotaRecommendation rec = new GradeQuotaRecommendation(
                    grade,
                    analysis.teacherCount,
                    currentAvgQuota,
                    currentTotalQuota,
                    avgAvailability,
                    recommendedQuota,
                    (int) Math.round(recommendedQuota * analysis.teacherCount),
                    quotaChange,
                    changePercentage,
                    priority
            );

            recommendations.addGradeRecommendation(rec);

            // Print analysis with clearer priority impact
            System.out.println("┌─ " + grade + " ─────────────────────");
            System.out.println("│  Teachers: " + analysis.teacherCount);
            System.out.println("│  Priority: " + priority + " → Multiplier: " + String.format("%.2fx", priorityMultiplier));
            System.out.println("│  Current avg quota: " + String.format("%.1f", currentAvgQuota) +
                    " (total: " + currentTotalQuota + ")");
            System.out.println("│  Availability: " + String.format("%.1f%%", avgAvailability));
            System.out.println("│");
            System.out.println("│  📊 Strategy Breakdown:");
            System.out.println("│    • Equal share (no priority): " + String.format("%.1f", equalShareQuota));
            System.out.println("│    • Priority-based: " + String.format("%.1f", priorityBasedQuota) +
                    " (" + String.format("%+.1f", priorityBasedQuota - equalShareQuota) + ")");
            System.out.println("│    • Availability-adjusted: " + String.format("%.1f", availabilityAdjustedQuota));
            System.out.println("│    • Capacity-adjusted: " + String.format("%.1f", capacityAdjustedQuota));
            System.out.println("│");
            System.out.println("│  🎯 RECOMMENDED: " + String.format("%.1f", recommendedQuota) +
                    " per teacher");
            System.out.println("│     Change: " + (quotaChange >= 0 ? "+" : "") +
                    String.format("%.1f", quotaChange) +
                    " (" + (changePercentage >= 0 ? "+" : "") +
                    String.format("%.1f%%", changePercentage) + ")");
            System.out.println("│     Total for grade: " + rec.recommendedTotalQuota +
                    " (vs current: " + currentTotalQuota + ")");
            System.out.println("└──────────────────────────────────\n");
        }

        // Calculate fairness metrics
        recommendations.calculateFairnessMetrics();

        // Verify total capacity
        int totalRecommended = recommendations.getTotalRecommendedCapacity();
        System.out.println("🔍 Verification:");
        System.out.println("   Total recommended capacity: " + totalRecommended);
        System.out.println("   Total needed: " + totalSupervisionNeeded);
        System.out.println("   Difference: " + (totalRecommended - totalSupervisionNeeded));
        System.out.println();

        return recommendations;
    }
    /**
     * Build complete recommendation response
     */
    private QuotaRecommendationResponse buildRecommendationResponse(
            ExamSessionDto session,
            Map<GradeType, GradeAnalysis> gradeAnalysis,
            QuotaRecommendations recommendations,
            int totalSupervisionNeeded) {

        // Calculate current fairness
        double currentStdDev = calculateQuotaStandardDeviation(gradeAnalysis, false);
        double recommendedStdDev = recommendations.standardDeviation;

        double currentRange = calculateQuotaRange(gradeAnalysis, false);
        double recommendedRange = recommendations.quotaRange;

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📊 FAIRNESS METRICS");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println();
        System.out.println("Current Distribution:");
        System.out.println("  • Std Deviation: " + String.format("%.2f", currentStdDev));
        System.out.println("  • Range (max-min): " + String.format("%.2f", currentRange));
        System.out.println("  • Fairness Score: " + getFairnessScore(currentStdDev));
        System.out.println();
        System.out.println("Recommended Distribution:");
        System.out.println("  • Std Deviation: " + String.format("%.2f", recommendedStdDev));
        System.out.println("  • Range (max-min): " + String.format("%.2f", recommendedRange));
        System.out.println("  • Fairness Score: " + getFairnessScore(recommendedStdDev));
        System.out.println();

        double improvement = ((currentStdDev - recommendedStdDev) / currentStdDev) * 100;
        if (improvement > 0) {
            System.out.println("✅ IMPROVEMENT: " + String.format("%.1f%%", improvement) + " more fair");
        } else if (improvement < -5) {
            System.out.println("⚠️  WARNING: Recommended quotas may be less fair");
        } else {
            System.out.println("ℹ️  Similar fairness level");
        }
        System.out.println();

        // Capacity check
        int currentTotalCapacity = gradeAnalysis.values().stream()
                .mapToInt(GradeAnalysis::getTotalQuota)
                .sum();
        int recommendedTotalCapacity = recommendations.getTotalRecommendedCapacity();

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("💼 CAPACITY ANALYSIS");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println();
        System.out.println("Total supervision needed: " + totalSupervisionNeeded);
        System.out.println("Current total capacity: " + currentTotalCapacity);
        System.out.println("Recommended total capacity: " + recommendedTotalCapacity);
        System.out.println();

        if (recommendedTotalCapacity >= totalSupervisionNeeded) {
            System.out.println("✅ Recommended capacity is SUFFICIENT");
        } else {
            int deficit = totalSupervisionNeeded - recommendedTotalCapacity;
            System.out.println("⚠️  WARNING: Recommended capacity is SHORT by " + deficit + " slots");
            System.out.println("   Consider increasing quotas or reducing exam requirements");
        }
        System.out.println();

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("💡 RECOMMENDATIONS");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println();

        List<String> actionItems = generateActionItems(recommendations, currentStdDev, recommendedStdDev);
        for (int i = 0; i < actionItems.size(); i++) {
            System.out.println((i + 1) + ". " + actionItems.get(i));
        }

        System.out.println();
        System.out.println("╚════════════════════════════════════════════════╝\n");

        return QuotaRecommendationResponse.builder()
                .sessionId(session.getId())
                .sessionName(session.getSessionLibelle())
                .totalSupervisionNeeded(totalSupervisionNeeded)
                .currentTotalCapacity(currentTotalCapacity)
                .recommendedTotalCapacity(recommendedTotalCapacity)
                .currentStandardDeviation(currentStdDev)
                .recommendedStandardDeviation(recommendedStdDev)
                .currentQuotaRange(currentRange)
                .recommendedQuotaRange(recommendedRange)
                .fairnessImprovement(improvement)
                .gradeRecommendations(recommendations.recommendations)
                .actionItems(actionItems)
                .build();
    }

    private double calculateQuotaStandardDeviation(Map<GradeType, GradeAnalysis> gradeAnalysis, boolean useRecommended) {
        List<Double> quotas = gradeAnalysis.values().stream()
                .map(g -> useRecommended ? 0 : g.getAverageQuota()) // Placeholder for now
                .collect(Collectors.toList());

        if (quotas.isEmpty()) return 0;

        double avg = quotas.stream().mapToDouble(d -> d).average().orElse(0);
        double variance = quotas.stream()
                .mapToDouble(q -> Math.pow(q - avg, 2))
                .average().orElse(0);

        return Math.sqrt(variance);
    }

    private double calculateQuotaRange(Map<GradeType, GradeAnalysis> gradeAnalysis, boolean useRecommended) {
        List<Double> quotas = gradeAnalysis.values().stream()
                .map(g -> useRecommended ? 0 : g.getAverageQuota())
                .collect(Collectors.toList());

        if (quotas.isEmpty()) return 0;

        double min = quotas.stream().mapToDouble(d -> d).min().orElse(0);
        double max = quotas.stream().mapToDouble(d -> d).max().orElse(0);

        return max - min;
    }

    private String getFairnessScore(double stdDev) {
        if (stdDev < 1.0) return "EXCELLENT ⭐⭐⭐⭐⭐";
        if (stdDev < 2.0) return "VERY GOOD ⭐⭐⭐⭐";
        if (stdDev < 3.0) return "GOOD ⭐⭐⭐";
        if (stdDev < 5.0) return "FAIR ⭐⭐";
        return "NEEDS IMPROVEMENT ⭐";
    }

    private List<String> generateActionItems(QuotaRecommendations recommendations,
                                             double currentStdDev, double recommendedStdDev) {
        List<String> items = new ArrayList<>();

        if (recommendedStdDev < currentStdDev) {
            items.add("Apply recommended quotas to improve fairness by " +
                    String.format("%.1f%%", ((currentStdDev - recommendedStdDev) / currentStdDev) * 100));
        }

        // Check for grades with large changes
        for (GradeQuotaRecommendation rec : recommendations.recommendations) {
            if (Math.abs(rec.changePercentage) > 20) {
                String direction = rec.quotaChange > 0 ? "INCREASE" : "DECREASE";
                items.add(direction + " quota for " + rec.grade + " by " +
                        String.format("%.0f", Math.abs(rec.quotaChange)) + " slots per teacher");
            }
        }

        // Check for grades with low availability
        for (GradeQuotaRecommendation rec : recommendations.recommendations) {
            if (rec.averageAvailability < 70) {
                items.add("Address high unavailability for " + rec.grade + " (" +
                        String.format("%.1f%%", 100 - rec.averageAvailability) + " unavailable)");
            }
        }

        if (items.isEmpty()) {
            items.add("Current quotas are well-balanced. No major changes needed.");
        }

        return items;
    }

    // ============================================
    // INNER CLASSES FOR ANALYSIS
    // ============================================

    static class GradeAnalysis {
        GradeType grade;
        int teacherCount = 0;
        int totalQuota = 0;
        int totalUnavailableSlots = 0;
        double totalAvailabilityRate = 0;
        List<Long> teacherIds = new ArrayList<>();

        public GradeAnalysis(GradeType grade) {
            this.grade = grade;
        }

        public void addTeacher(Long teacherId, int quota, int unavailableSlots, double availabilityRate) {
            teacherIds.add(teacherId);
            teacherCount++;
            totalQuota += quota;
            totalUnavailableSlots += unavailableSlots;
            totalAvailabilityRate += availabilityRate;
        }

        public double getAverageQuota() {
            return teacherCount > 0 ? (double) totalQuota / teacherCount : 0;
        }

        public double getAverageAvailability() {
            return teacherCount > 0 ? totalAvailabilityRate / teacherCount : 100;
        }

        public int getTotalQuota() {
            return totalQuota;
        }
    }

    static class QuotaRecommendations {
        List<GradeQuotaRecommendation> recommendations = new ArrayList<>();
        double standardDeviation;
        double quotaRange;

        public void addGradeRecommendation(GradeQuotaRecommendation rec) {
            recommendations.add(rec);
        }

        public void calculateFairnessMetrics() {
            if (recommendations.isEmpty()) return;

            double avg = recommendations.stream()
                    .mapToDouble(r -> r.recommendedAvgQuota)
                    .average().orElse(0);

            double variance = recommendations.stream()
                    .mapToDouble(r -> Math.pow(r.recommendedAvgQuota - avg, 2))
                    .average().orElse(0);

            standardDeviation = Math.sqrt(variance);

            double min = recommendations.stream()
                    .mapToDouble(r -> r.recommendedAvgQuota)
                    .min().orElse(0);
            double max = recommendations.stream()
                    .mapToDouble(r -> r.recommendedAvgQuota)
                    .max().orElse(0);

            quotaRange = max - min;
        }

        public int getTotalRecommendedCapacity() {
            return recommendations.stream()
                    .mapToInt(r -> r.recommendedTotalQuota)
                    .sum();
        }
    }

    // ============================================
    // RESPONSE MODELS
    // ============================================

    public static class QuotaRecommendationResponse {
        private Long sessionId;
        private String sessionName;
        private int totalSupervisionNeeded;
        private int currentTotalCapacity;
        private int recommendedTotalCapacity;
        private double currentStandardDeviation;
        private double recommendedStandardDeviation;
        private double currentQuotaRange;
        private double recommendedQuotaRange;
        private double fairnessImprovement;
        private List<GradeQuotaRecommendation> gradeRecommendations;
        private List<String> actionItems;

        public static QuotaRecommendationResponseBuilder builder() {
            return new QuotaRecommendationResponseBuilder();
        }

        // Getters and setters...
        public Long getSessionId() { return sessionId; }
        public String getSessionName() { return sessionName; }
        public int getTotalSupervisionNeeded() { return totalSupervisionNeeded; }
        public int getCurrentTotalCapacity() { return currentTotalCapacity; }
        public int getRecommendedTotalCapacity() { return recommendedTotalCapacity; }
        public double getCurrentStandardDeviation() { return currentStandardDeviation; }
        public double getRecommendedStandardDeviation() { return recommendedStandardDeviation; }
        public double getCurrentQuotaRange() { return currentQuotaRange; }
        public double getRecommendedQuotaRange() { return recommendedQuotaRange; }
        public double getFairnessImprovement() { return fairnessImprovement; }
        public List<GradeQuotaRecommendation> getGradeRecommendations() { return gradeRecommendations; }
        public List<String> getActionItems() { return actionItems; }

        public static class QuotaRecommendationResponseBuilder {
            private QuotaRecommendationResponse response = new QuotaRecommendationResponse();

            public QuotaRecommendationResponseBuilder sessionId(Long sessionId) {
                response.sessionId = sessionId;
                return this;
            }
            public QuotaRecommendationResponseBuilder sessionName(String sessionName) {
                response.sessionName = sessionName;
                return this;
            }
            public QuotaRecommendationResponseBuilder totalSupervisionNeeded(int totalSupervisionNeeded) {
                response.totalSupervisionNeeded = totalSupervisionNeeded;
                return this;
            }
            public QuotaRecommendationResponseBuilder currentTotalCapacity(int currentTotalCapacity) {
                response.currentTotalCapacity = currentTotalCapacity;
                return this;
            }
            public QuotaRecommendationResponseBuilder recommendedTotalCapacity(int recommendedTotalCapacity) {
                response.recommendedTotalCapacity = recommendedTotalCapacity;
                return this;
            }
            public QuotaRecommendationResponseBuilder currentStandardDeviation(double currentStandardDeviation) {
                response.currentStandardDeviation = currentStandardDeviation;
                return this;
            }
            public QuotaRecommendationResponseBuilder recommendedStandardDeviation(double recommendedStandardDeviation) {
                response.recommendedStandardDeviation = recommendedStandardDeviation;
                return this;
            }
            public QuotaRecommendationResponseBuilder currentQuotaRange(double currentQuotaRange) {
                response.currentQuotaRange = currentQuotaRange;
                return this;
            }
            public QuotaRecommendationResponseBuilder recommendedQuotaRange(double recommendedQuotaRange) {
                response.recommendedQuotaRange = recommendedQuotaRange;
                return this;
            }
            public QuotaRecommendationResponseBuilder fairnessImprovement(double fairnessImprovement) {
                response.fairnessImprovement = fairnessImprovement;
                return this;
            }
            public QuotaRecommendationResponseBuilder gradeRecommendations(List<GradeQuotaRecommendation> gradeRecommendations) {
                response.gradeRecommendations = gradeRecommendations;
                return this;
            }
            public QuotaRecommendationResponseBuilder actionItems(List<String> actionItems) {
                response.actionItems = actionItems;
                return this;
            }
            public QuotaRecommendationResponse build() {
                return response;
            }
        }
    }

    public static class GradeQuotaRecommendation {
        private GradeType grade;
        private int teacherCount;
        private double currentAvgQuota;
        private int currentTotalQuota;
        private double averageAvailability;
        private double recommendedAvgQuota;
        private int recommendedTotalQuota;
        private double quotaChange;
        private double changePercentage;
        private int currentPriority;

        public GradeQuotaRecommendation(GradeType grade, int teacherCount,
                                        double currentAvgQuota, int currentTotalQuota,
                                        double averageAvailability, double recommendedAvgQuota,
                                        int recommendedTotalQuota, double quotaChange,
                                        double changePercentage, int currentPriority) {
            this.grade = grade;
            this.teacherCount = teacherCount;
            this.currentAvgQuota = currentAvgQuota;
            this.currentTotalQuota = currentTotalQuota;
            this.averageAvailability = averageAvailability;
            this.recommendedAvgQuota = recommendedAvgQuota;
            this.recommendedTotalQuota = recommendedTotalQuota;
            this.quotaChange = quotaChange;
            this.changePercentage = changePercentage;
            this.currentPriority = currentPriority;
        }


        public GradeType getGrade() { return grade; }
        public int getTeacherCount() { return teacherCount; }
        public double getCurrentAvgQuota() { return currentAvgQuota; }
        public int getCurrentTotalQuota() { return currentTotalQuota; }
        public double getAverageAvailability() { return averageAvailability; }
        public double getRecommendedAvgQuota() { return recommendedAvgQuota; }
        public int getRecommendedTotalQuota() { return recommendedTotalQuota; }
        public double getQuotaChange() { return quotaChange; }
        public double getChangePercentage() { return changePercentage; }
        public int getCurrentPriority() { return currentPriority; }
    }
}