package org.teacherdistributionsystem.distribution_system.services.assignment;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.config.AssignmentConstraintConfig;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.*;
import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;
import org.teacherdistributionsystem.distribution_system.models.others.*;
import org.teacherdistributionsystem.distribution_system.models.projections.ExamForAssignmentProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilityProjection;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.*;
import org.teacherdistributionsystem.distribution_system.services.teacher.QuotaPerGradeService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherQuotaService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherUnavailabilityService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;


@Service
public class AssignmentAlgorithmService {
    private AssignmentConstraintConfig config;
    private final TeacherQuotaService teacherQuotaService;
    private final TeacherUnavailabilityService teacherUnavailabilityService;
    private final ExamSessionService examSessionService;
    private final ExamService examService;
    private final TeacherService teacherService;
    private final QuotaPerGradeService quotaPerGradeService;

    static class Exam {
        String examId;
        int day;
        int seance;
        String salle;
        Long ownerTeacherId;
        int requiredSupervisors;

        // Add temporal fields
        LocalDate examDate;
        LocalTime startTime;
        LocalTime endTime;

        public Exam(String examId, int day, int seance, String salle, Long ownerTeacherId,
                    int requiredSupervisors, LocalDate examDate, LocalTime startTime, LocalTime endTime) {
            this.examId = examId;
            this.day = day;
            this.seance = seance;
            this.salle = salle;
            this.ownerTeacherId = ownerTeacherId;
            this.requiredSupervisors = requiredSupervisors;
            this.examDate = examDate;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    static class TeacherContribution {
        int teacherIdx;
        int contribution;

        public TeacherContribution(int teacherIdx, int contribution) {
            this.teacherIdx = teacherIdx;
            this.contribution = contribution;
        }
    }

    private BoolVar[][] assignment;
    private CpModel model;
    private int numTeachers;
    private int numExams;

    private Long[] teacherIds;
    private String[] teacherNames;
    private String[] teacherEmails;
    private Boolean[] teacherParticipateSurveillance;
    private String[] teacherGrades;
    private int[] teacherPriorities;

    private boolean[][][] teacherUnavailable;
    private int[] baseQuotas;
    private int[] effectiveQuotas;
    private Set<Integer> teachersWithRelaxedUnavailability;

    private List<Exam> exams;
    private Map<Long, Integer> teacherIdToIndex;
    private ExamSessionDto currentSession;
    private int totalConstraintsAdded = 0;
    private int relaxationAttemptNumber = 0;
    private Map<String, Set<Long>> examOwnersByKey;
    public AssignmentAlgorithmService(TeacherService teacherService,
                                      TeacherQuotaService teacherQuotaService,
                                      TeacherUnavailabilityService teacherUnavailabilityService,
                                      ExamSessionService examSessionService,
                                      ExamService examService,
                                      QuotaPerGradeService quotaPerGradeService) {
        Loader.loadNativeLibraries();
        this.teacherService = teacherService;
        this.teacherQuotaService = teacherQuotaService;
        this.teacherUnavailabilityService = teacherUnavailabilityService;
        this.examSessionService = examSessionService;
        this.examService = examService;
        this.quotaPerGradeService = quotaPerGradeService;


        this.config = AssignmentConstraintConfig.defaultConfig();
    }
    public void setConfig(AssignmentConstraintConfig config) {
        this.config = config;
    }


    @Async
    public CompletableFuture<AssignmentResponseModel> executeAssignmentWithConfig(
            Long sessionId,
            AssignmentConstraintConfig customConfig) {

        AssignmentConstraintConfig originalConfig = this.config;
        this.config = customConfig;

        try {
            return executeAssignment(sessionId);
        } finally {

            this.config = originalConfig;
        }
    }
    @Async
    public CompletableFuture<AssignmentResponseModel> executeAssignment(Long sessionId) {
        try {
            loadData(sessionId);
            teachersWithRelaxedUnavailability = new HashSet<>();

            System.out.println("========================================");
            System.out.println("HUMAN-LIKE ASSIGNMENT STRATEGY");
            System.out.println("========================================");

            calculateEffectiveQuotas();

            // Calculate total supervision needed based on EACH exam's requirements
            int totalSupervisionNeeded = 0;
            for (Exam exam : exams) {
                totalSupervisionNeeded += exam.requiredSupervisors;
            }

            int totalCapacity = calculateTotalCapacity();
            int availableExamSlots = calculateAvailableExamSlots();

            System.out.println("\n=== PHASE 1: CAPACITY CHECK ===");
            System.out.println("Total supervisions needed: " + totalSupervisionNeeded);
            System.out.println("Total teacher capacity (quotas): " + totalCapacity);
            System.out.println("Available exam-teacher slot pairs: " + availableExamSlots);

            boolean sufficientCapacity = totalCapacity >= totalSupervisionNeeded;
            boolean sufficientAvailableSlots = availableExamSlots >= totalSupervisionNeeded;

            System.out.println("\nFeasibility indicators:");
            System.out.println("  Quota capacity sufficient: " + (sufficientCapacity ? "✓" : "✗"));
            System.out.println("  Available slots sufficient: " + (sufficientAvailableSlots ? "✓" : "✗"));



        // PHASE 1: Try solving with strict unavailability + priority assignments
            System.out.println("\n=== PHASE 2: TRY WITH STRICT UNAVAILABILITY ===");
            System.out.println("Strategy: Use optimization to prefer available teachers for conflict slots");

            model = new CpModel();
            createVariables();
            addConstraintsWithPriority();
            AssignmentResponseModel result = solve();

            if (result.getStatus() == AssignmentStatus.SUCCESS) {
                System.out.println("[SUCCESS] Solution found with all unavailability respected!");
                System.out.println("========================================\n");
                return CompletableFuture.completedFuture(result);
            }

// Check if it's a timeout vs true infeasibility
            if (result.getStatus() == AssignmentStatus.TIMEOUT) {
                System.out.println("[TIMEOUT] Solver couldn't complete within time limit.");
                System.out.println("Increasing time limit and retrying...");

                // Retry with longer timeout
                CpSolver solver = new CpSolver();
                solver.getParameters().setMaxTimeInSeconds(30.0);
                long startTime = System.currentTimeMillis();
                CpSolverStatus status = solver.solve(model);
                double solutionTime = (System.currentTimeMillis() - startTime) / 1000.0;

                System.out.println("Extended solve status: " + status + " (Time: " + String.format("%.3f", solutionTime) + "s)");

                if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
                    result = buildSuccessResponse(solver, status, solutionTime);
                    System.out.println("[SUCCESS] Solution found with extended timeout!");
                    System.out.println("========================================\n");
                    return CompletableFuture.completedFuture(result);
                } else if (status == CpSolverStatus.UNKNOWN) {
                    System.out.println("[STILL TIMEOUT] Even with 30s, couldn't solve.");
                    System.out.println("This suggests the problem may be very constrained.");
                    System.out.println("Proceeding to relaxation...");
                }
            }

// PHASE 2: If truly infeasible (not just timeout), try progressive relaxation
            if (result.getStatus() == AssignmentStatus.INFEASIBLE) {
                System.out.println("[INFEASIBLE] Could not solve with strict unavailability.");
            } else {
                System.out.println("[UNABLE TO SOLVE] Status: " + result.getStatus());
            }

// Check if relaxation can help
            if (!sufficientCapacity) {
                System.out.println("\n[ANALYSIS] Insufficient total capacity.");
                System.out.println("  Need to relax unavailability to reach capacity.");
            } else {
                System.out.println("\n[ANALYSIS] Capacity is sufficient but constraints conflict.");
                System.out.println("  This could be due to:");
                System.out.println("  - Time slot distribution (too many exams at certain times)");
                System.out.println("  - Ownership conflicts (owners can't cover their own slots)");
                System.out.println("  - Unavailability clustering");
                System.out.println("  - Equal assignment constraints for same-grade teachers");
                System.out.println("  Attempting progressive relaxation...");
            }


            // Always attempt relaxation if strict mode failed
            System.out.println("\n=== PHASE 3: PROGRESSIVE RELAXATION ===");
            return CompletableFuture.completedFuture(attemptProgressiveRelaxation(totalSupervisionNeeded));

        } catch (jakarta.persistence.EntityNotFoundException e) {
            System.err.println("[ERROR] Entity not found: " + e.getMessage());
            return CompletableFuture.failedFuture(e);

        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    private void loadData(Long sessionId)  {
        System.out.println("\n=== LOADING DATA ===");

        Map<Long, Boolean> map = teacherService.getTeacherParticipeSurveillance();
        numTeachers = map.size();
        teacherIds = map.keySet().toArray(Long[]::new);
        teacherParticipateSurveillance = map.values().toArray(Boolean[]::new);

        teacherGrades = new String[numTeachers];
        teacherNames = new String[numTeachers];
        teacherEmails = new String[numTeachers];
        baseQuotas = new int[numTeachers];
        effectiveQuotas = new int[numTeachers];
        teacherPriorities = new int[numTeachers];

        Map<Long, String> gradeMap = teacherService.getAllGrades();
        Map<Long, String> nameMap = teacherService.getAllNames();
        Map<Long, Integer> quotaMap = teacherQuotaService.getAllQuotas(sessionId);
        Map<Long, String> emailMap = teacherService.getAllEmails();
        Map<GradeType, Integer> priorityPerGradeMap = quotaPerGradeService.getPrioritiesByGrade();

        for (int i = 0; i < numTeachers; i++) {
            teacherGrades[i] = gradeMap.get(teacherIds[i]);
            teacherNames[i] = nameMap.getOrDefault(teacherIds[i], "Unknown");
            teacherEmails[i] = emailMap.getOrDefault(teacherIds[i], "Unknown");
            baseQuotas[i] = quotaMap.getOrDefault(teacherIds[i], 0);


            try {
                GradeType gradeType = GradeType.valueOf(teacherGrades[i]);
                teacherPriorities[i] = priorityPerGradeMap.getOrDefault(gradeType, Integer.MAX_VALUE);
            } catch (IllegalArgumentException | NullPointerException e) {
                System.err.println("Warning: Invalid or missing grade for teacher " + teacherIds[i] + ": " + teacherGrades[i]);
                teacherPriorities[i] = Integer.MAX_VALUE;
            }
        }


        teacherIdToIndex = new HashMap<>();
        for (int i = 0; i < numTeachers; i++) {
            teacherIdToIndex.put(teacherIds[i], i);
        }

        currentSession = examSessionService.getExamSessionDto(sessionId);



        int numDays = currentSession.getNumExamDays();
        int numSeances = SeanceType.values().length;

        List<TeacherUnavailabilityProjection> teacherUnavailability =
                teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(currentSession.getId());

        teacherUnavailable = new boolean[numTeachers][numDays][numSeances];

        for (TeacherUnavailabilityProjection t : teacherUnavailability) {
            Integer teacherIdx = teacherIdToIndex.get(t.getId());
            if (teacherIdx == null) continue;

            int dayIdx = t.getNumeroJour();
            int seanceIdx = SeanceType.valueOf(t.getSeance()).ordinal();

            if (dayIdx >= 0 && dayIdx < numDays && seanceIdx >= 0 && seanceIdx < numSeances) {
                teacherUnavailable[teacherIdx][dayIdx][seanceIdx] = true;
            }
        }

        // ========================================
        // FIX: Deduplicate exams and handle dynamic supervisor counts
        // ========================================
        List<ExamForAssignmentProjection> examsList = examService.getExamsForAssignment(currentSession.getId());

        // Group exams by logical key (day + seance + room)
        Map<String, List<ExamForAssignmentProjection>> examGroups = new HashMap<>();

        for (ExamForAssignmentProjection e : examsList) {
            int dayIdx = e.getJourNumero() - 1;
            int seanceIdx = e.getSeance().ordinal();

            if (dayIdx >= 0 && dayIdx < numDays && seanceIdx >= 0 && seanceIdx < numSeances) {
                String examKey = dayIdx + "_" + seanceIdx + "_" + e.getNumRooms();
                examGroups.computeIfAbsent(examKey, k -> new ArrayList<>()).add(e);
            }
        }

        // Create ONE exam per logical group
        exams = new ArrayList<>();
        examOwnersByKey = new HashMap<>();

        int totalSupervisorsNeeded = 0;
        int minSupervisors = Integer.MAX_VALUE;
        int maxSupervisors = Integer.MIN_VALUE;

        for (Map.Entry<String, List<ExamForAssignmentProjection>> entry : examGroups.entrySet()) {
            String examKey = entry.getKey();
            List<ExamForAssignmentProjection> group = entry.getValue();

            // Use the first exam as representative
            ExamForAssignmentProjection representative = group.get(0);
            int dayIdx = representative.getJourNumero() - 1;
            int seanceIdx = representative.getSeance().ordinal();

            // IMPORTANT: All rows for same logical exam should have same requiredSupervisors
            // Use the first one (they should all be identical)
            int requiredSupervisors = representative.getRequiredSupervisors();

            // Validate consistency (optional but recommended)
            for (ExamForAssignmentProjection exam : group) {
                if (exam.getRequiredSupervisors() != requiredSupervisors) {
                    System.err.println("WARNING: Inconsistent requiredSupervisors for exam at " + examKey);
                }
            }

            // Collect ALL owner IDs for this logical exam
            Set<Long> ownerIds = new HashSet<>();
            for (ExamForAssignmentProjection exam : group) {
                if (exam.getResponsableId() != null) {
                    ownerIds.add(exam.getResponsableId());
                }
            }

            examOwnersByKey.put(examKey, ownerIds);

            Long representativeOwnerId = ownerIds.isEmpty() ? null : ownerIds.iterator().next();

            exams.add(new Exam(
                    representative.getId(),
                    dayIdx,
                    seanceIdx,
                    representative.getNumRooms(),
                    representativeOwnerId,
                    requiredSupervisors,
                    representative.getExamDate(),
                    representative.getStartTime(),
                    representative.getEndTime()
            ));

            totalSupervisorsNeeded += requiredSupervisors;
            minSupervisors = Math.min(minSupervisors, requiredSupervisors);
            maxSupervisors = Math.max(maxSupervisors, requiredSupervisors);
        }

        numExams = exams.size();

        System.out.println("Teachers: " + numTeachers + ", Exams: " + numExams);
        System.out.println("Original DB rows: " + examsList.size() + ", Deduplicated logical exams: " + numExams);
        System.out.println("Total supervisors needed: " + totalSupervisorsNeeded);
        System.out.println("Supervisor requirements - Min: " + minSupervisors + ", Max: " + maxSupervisors);

        // Log any exams with multiple owners
        int multiOwnerCount = 0;
        for (Set<Long> owners : examOwnersByKey.values()) {
            if (owners.size() > 1) {
                multiOwnerCount++;
            }
        }
        if (multiOwnerCount > 0) {
            System.out.println("Exams with multiple owners: " + multiOwnerCount);
        }

        System.out.println("===================\n");
    }



    private void calculateEffectiveQuotas() {
        // IMPORTANT: Quota is a limit on total assignments, NOT reduced by unavailability
        // Unavailability blocks specific exams, but doesn't reduce the quota itself
        // The solver will naturally assign up to quota only to AVAILABLE exams

        for (int t = 0; t < numTeachers; t++) {
            if (!teacherParticipateSurveillance[t]) {
                effectiveQuotas[t] = 0;
                continue;
            }

            // Always use base quota
            // The unavailability constraints will prevent assignment to specific exams
            effectiveQuotas[t] = baseQuotas[t];
        }

        System.out.println("\nQuota calculation:");
        System.out.println("  Using BASE quotas (unavailability handled by constraints, not quota reduction)");
    }

    private int countUnavailableSlots(int teacherIdx) {
        int count = 0;
        for (Exam exam : exams) {
            if (exam.day < teacherUnavailable[teacherIdx].length &&
                    exam.seance < teacherUnavailable[teacherIdx][exam.day].length) {
                if (teacherUnavailable[teacherIdx][exam.day][exam.seance]) {
                    count++;
                }
            }
        }
        return count;
    }

    private int calculateTotalCapacity() {
        int total = 0;
        for (int t = 0; t < numTeachers; t++) {
            if (teacherParticipateSurveillance[t]) {
                total += baseQuotas[t];
            }
        }
        return total;
    }

    private int calculateAvailableExamSlots() {
        // Count how many (teacher, exam) pairs are available
        // (not blocked by unavailability or ownership)
        int available = 0;

        for (int t = 0; t < numTeachers; t++) {
            if (!teacherParticipateSurveillance[t]) continue;
            if (teachersWithRelaxedUnavailability.contains(t)) {
                // All exams available for this teacher (except owned)
                available += numExams;
                // Subtract owned exams
                for (Exam exam : exams) {
                    if (exam.ownerTeacherId != null && exam.ownerTeacherId.equals(teacherIds[t])) {
                        available--;
                    }
                }
            } else {
                // Count only available slots
                for (int e = 0; e < numExams; e++) {
                    Exam exam = exams.get(e);

                    // Skip if teacher owns this exam
                    if (exam.ownerTeacherId != null && exam.ownerTeacherId.equals(teacherIds[t])) {
                        continue;
                    }

                    // Skip if teacher is unavailable
                    if (exam.day < teacherUnavailable[t].length &&
                            exam.seance < teacherUnavailable[t][exam.day].length &&
                            teacherUnavailable[t][exam.day][exam.seance]) {
                        continue;
                    }

                    available++;
                }
            }
        }

        return available;
    }

    private AssignmentResponseModel attemptProgressiveRelaxation(int totalSupervisionNeeded) {
        // Calculate theoretical maximum capacity (all teachers, all unavailability ignored)
        int maxTheoreticalCapacity = calculateTotalCapacity();

        System.out.println("\nCapacity Analysis:");
        System.out.println("  Supervisions needed: " + totalSupervisionNeeded);
        System.out.println("  Current total capacity: " + maxTheoreticalCapacity);
        System.out.println("  Deficit: " + (totalSupervisionNeeded - maxTheoreticalCapacity));

        // If even ignoring ALL unavailability we don't have enough capacity, stop immediately
        if (maxTheoreticalCapacity < totalSupervisionNeeded) {
            System.out.println("\n❌ IMPOSSIBLE: Even with all unavailability ignored,");
            System.out.println("   total capacity (" + maxTheoreticalCapacity + ") < needed (" + totalSupervisionNeeded + ")");
            System.out.println("\n💡 SOLUTIONS:");
            System.out.println("   1. Increase teacher quotas (need +" + (totalSupervisionNeeded - maxTheoreticalCapacity) + " more slots)");
            System.out.println("   2. Reduce number of exams");
            System.out.println("   3. Add more teachers to supervision pool");

            return AssignmentResponseModel.builder()
                    .status(AssignmentStatus.INFEASIBLE)
                    .message("Insufficient total capacity: " + maxTheoreticalCapacity +
                            " < " + totalSupervisionNeeded +
                            ". Need to increase quotas by " + (totalSupervisionNeeded - maxTheoreticalCapacity))
                    .metadata(AssignmentMetadata.builder()
                            .sessionId(currentSession.getId())
                            .sessionName(currentSession.getSessionLibelle())
                            .totalExams(numExams)
                            .totalTeachers(numTeachers)
                            .build())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        System.out.println("  ✓ Theoretical capacity sufficient - trying progressive relaxation\n");

        // Sort teachers by priority (lowest first)
        List<Integer> participatingTeacherIndices = new ArrayList<>();
        for (int t = 0; t < numTeachers; t++) {
            if (teacherParticipateSurveillance[t]) {
                participatingTeacherIndices.add(t);
            }
        }
        participatingTeacherIndices.sort(Comparator.comparingInt(t -> -teacherPriorities[t]));

        // Build contributions list
        List<TeacherContribution> contributions = new ArrayList<>();
        for (int teacherIdx : participatingTeacherIndices) {
            int unavailableSlots = countUnavailableSlots(teacherIdx);
            if (unavailableSlots > 0) {
                contributions.add(new TeacherContribution(teacherIdx, unavailableSlots));
            }
        }

        System.out.println("Teachers available for relaxation: " + contributions.size());

        // Try relaxing in small batches
        relaxationAttemptNumber = 0;
        int maxAttempts = 15; // Increased to allow more attempts

        // Start with smaller batches for more gradual relaxation
        int batchSize = Math.max(1, contributions.size() / 20); // 5% at a time

        while (!contributions.isEmpty() && relaxationAttemptNumber <= maxAttempts) {
            relaxationAttemptNumber++;
            int teachersToAdd = Math.min(batchSize, contributions.size());

            System.out.println("\n[RELAXATION ATTEMPT " + relaxationAttemptNumber + "] Adding " +
                    teachersToAdd + " teacher(s)");

            // Relax batch
            List<TeacherContribution> batch = contributions.subList(0, teachersToAdd);
            for (TeacherContribution tc : batch) {
                teachersWithRelaxedUnavailability.add(tc.teacherIdx);
                System.out.println("  + " + teacherNames[tc.teacherIdx] +
                        " (Priority: " + teacherPriorities[tc.teacherIdx] +
                        ", restores " + tc.contribution + " exam slots)");
            }
            contributions = contributions.subList(teachersToAdd, contributions.size());

            // Show progress
            int relaxedCount = teachersWithRelaxedUnavailability.size();
            double relaxedPercentage = (relaxedCount * 100.0) / numTeachers;
            System.out.println("  → Total relaxed: " + relaxedCount + "/" + numTeachers +
                    " (" + String.format("%.1f%%", relaxedPercentage) + ")");

            // Try solving
            model = new CpModel();
            createVariables();
            addConstraintsWithPriority();

            AssignmentResponseModel result = solve();
            if (result.getStatus() == AssignmentStatus.SUCCESS) {
                // Update metadata
                result.getMetadata().setIsOptimal(false);
                result.getMetadata().setRelaxedTeachersCount(teachersWithRelaxedUnavailability.size());
                result.getMetadata().setTotalConstraints(totalConstraintsAdded);
                result.getMetadata().setRelaxationAttempts(relaxationAttemptNumber);

                // Clear message
                result.setMessage(
                        "Solution found with relaxed constraints. " +
                                teachersWithRelaxedUnavailability.size() + " teacher(s) assigned to originally unavailable slots. " +
                                "Found after " + relaxationAttemptNumber + " relaxation attempts."
                );


                return result;
            }

            // If we've relaxed >50% of teachers and still infeasible, likely structural issue
            if (relaxedPercentage > 50) {
                System.out.println("\n⚠️  WARNING: Relaxed over 50% of teachers with no solution");
                System.out.println("   This suggests structural constraints beyond unavailability:");
                System.out.println("   - Time slot conflicts (too many exams at same time)");
                System.out.println("   - Ownership conflicts (teacher owns exam during high-demand slot)");
                System.out.println("   - Quota distribution issues");
            }

            relaxationAttemptNumber++;

            // Increase batch size gradually for faster convergence
            if (relaxationAttemptNumber > 3) {
                batchSize = Math.max(batchSize, contributions.size() / 10); // 10% batches
            }
            if (relaxationAttemptNumber > 7) {
                batchSize = Math.max(batchSize, contributions.size() / 5); // 20% batches
            }
        }

        System.out.println("\n[FAILED] Could not find solution even with relaxation");
        System.out.println("Final status: " + teachersWithRelaxedUnavailability.size() + " teachers relaxed");
        System.out.println("Total attempts: " + relaxationAttemptNumber);

        return buildInfeasibleResponse(0.0);
    }

    private void createVariables() {
        assignment = new BoolVar[numTeachers][numExams];
        for (int t = 0; t < numTeachers; t++) {
            for (int e = 0; e < numExams; e++) {
                assignment[t][e] = model.newBoolVar("T" + teacherIds[t] + "_E" + exams.get(e).examId);
            }
        }
    }

    private void addConstraintsWithPriority() {
        System.out.println("\n--- Adding Constraints with Configuration ---");
        System.out.println("Config: Owner Presence=" + config.getOwnerPresenceMode() +
                ", No Gaps=" + config.getNoGapsMode());
        totalConstraintsAdded = 0;

        // 1. Each exam needs exactly its required number of teachers
        if (config.getExamCoverageMode() == AssignmentConstraintConfig.ConstraintMode.HARD) {
            for (int e = 0; e < numExams; e++) {
                Exam exam = exams.get(e);
                LinearExprBuilder sum = LinearExpr.newBuilder();
                for (int t = 0; t < numTeachers; t++) {
                    sum.addTerm(assignment[t][e], 1);
                }
                model.addEquality(sum, exam.requiredSupervisors);
                totalConstraintsAdded++;
            }
            System.out.println("✓ Exam coverage: HARD (each exam needs exact supervisors)");
        }

        // 2. Non-participating teachers excluded
        if (config.getParticipationMode() == AssignmentConstraintConfig.ConstraintMode.HARD) {
            int nonParticipatingConstraints = 0;
            for (int t = 0; t < numTeachers; t++) {
                if (!teacherParticipateSurveillance[t]) {
                    for (int e = 0; e < numExams; e++) {
                        model.addEquality(assignment[t][e], 0);
                        nonParticipatingConstraints++;
                    }
                }
            }
            totalConstraintsAdded += nonParticipatingConstraints;
            System.out.println("✓ Participation: HARD (non-participants excluded)");
        }

        // 3. Exam ownership constraints
        int ownershipConstraints = 0;

        // 3a. Cannot supervise own exam
        if (config.getOwnershipExclusionMode() == AssignmentConstraintConfig.ConstraintMode.HARD) {
            for (int e = 0; e < numExams; e++) {
                Exam exam = exams.get(e);
                String examKey = exam.day + "_" + exam.seance + "_" + exam.salle;
                Set<Long> ownerIds = examOwnersByKey.getOrDefault(examKey, new HashSet<>());

                if (ownerIds.isEmpty()) continue;

                for (Long ownerId : ownerIds) {
                    if (teacherIdToIndex.containsKey(ownerId)) {
                        int ownerIdx = teacherIdToIndex.get(ownerId);
                        model.addEquality(assignment[ownerIdx][e], 0);
                        ownershipConstraints++;
                    }
                }
            }
            totalConstraintsAdded += ownershipConstraints;
            System.out.println("✓ Ownership exclusion: HARD (" + ownershipConstraints + " constraints)");
        }

        // 3b. Owner presence in same slot (configurable)
        int ownerPresenceConstraints = 0;

        if (config.getOwnerPresenceMode() == AssignmentConstraintConfig.ConstraintMode.HARD) {
            // HARD: At least one owner MUST supervise another exam in same slot
            Map<String, List<Integer>> examsBySlot = new HashMap<>();
            for (int e = 0; e < numExams; e++) {
                Exam exam = exams.get(e);
                String slotKey = exam.day + "_" + exam.seance;
                examsBySlot.computeIfAbsent(slotKey, k -> new ArrayList<>()).add(e);
            }

            for (int e = 0; e < numExams; e++) {
                Exam exam = exams.get(e);
                String examKey = exam.day + "_" + exam.seance + "_" + exam.salle;
                Set<Long> ownerIds = examOwnersByKey.getOrDefault(examKey, new HashSet<>());

                if (ownerIds.isEmpty()) continue;

                List<Integer> participatingOwners = ownerIds.stream()
                        .filter(teacherIdToIndex::containsKey)
                        .map(teacherIdToIndex::get)
                        .filter(idx -> teacherParticipateSurveillance[idx])
                        .toList();

                if (participatingOwners.isEmpty()) continue;

                String slotKey = exam.day + "_" + exam.seance;
                List<Integer> otherExamIndices = new ArrayList<>();

                for (int otherExamIdx : examsBySlot.get(slotKey)) {
                    if (otherExamIdx == e) continue;

                    Exam otherExam = exams.get(otherExamIdx);
                    String otherExamKey = otherExam.day + "_" + otherExam.seance + "_" + otherExam.salle;
                    Set<Long> otherOwners = examOwnersByKey.getOrDefault(otherExamKey, new HashSet<>());

                    boolean atLeastOneOwnerCanSupervise = false;
                    for (int ownerIdx : participatingOwners) {
                        Long ownerId = teacherIds[ownerIdx];
                        if (!otherOwners.contains(ownerId)) {
                            atLeastOneOwnerCanSupervise = true;
                            break;
                        }
                    }

                    if (atLeastOneOwnerCanSupervise) {
                        otherExamIndices.add(otherExamIdx);
                    }
                }

                if (otherExamIndices.isEmpty()) continue;

                LinearExprBuilder atLeastOneOwnerPresent = LinearExpr.newBuilder();

                for (int ownerIdx : participatingOwners) {
                    Long ownerId = teacherIds[ownerIdx];

                    for (int otherExamIdx : otherExamIndices) {
                        Exam otherExam = exams.get(otherExamIdx);
                        String otherExamKey = otherExam.day + "_" + otherExam.seance + "_" + otherExam.salle;
                        Set<Long> otherOwners = examOwnersByKey.getOrDefault(otherExamKey, new HashSet<>());

                        if (!otherOwners.contains(ownerId)) {
                            atLeastOneOwnerPresent.addTerm(assignment[ownerIdx][otherExamIdx], 1);
                        }
                    }
                }

                model.addGreaterOrEqual(atLeastOneOwnerPresent, 1);
                ownerPresenceConstraints++;
            }

            totalConstraintsAdded += ownerPresenceConstraints;
            System.out.println("✓ Owner presence: HARD (" + ownerPresenceConstraints + " constraints)");
            System.out.println("  WARNING: This may cause infeasibility if owners are unavailable");
        } else if (config.getOwnerPresenceMode() == AssignmentConstraintConfig.ConstraintMode.SOFT) {
            System.out.println("✓ Owner presence: SOFT (will be optimized, not required)");
        } else {
            System.out.println("✓ Owner presence: DISABLED");
        }

        // 4. Unavailability (only for non-relaxed teachers)
        if (config.getUnavailabilityMode() == AssignmentConstraintConfig.ConstraintMode.HARD) {
            int unavailabilityConstraints = 0;
            for (int t = 0; t < numTeachers; t++) {
                if (teachersWithRelaxedUnavailability.contains(t)) {
                    continue;
                }

                for (int e = 0; e < numExams; e++) {
                    Exam exam = exams.get(e);
                    if (exam.day < teacherUnavailable[t].length &&
                            exam.seance < teacherUnavailable[t][exam.day].length) {
                        if (teacherUnavailable[t][exam.day][exam.seance]) {
                            model.addEquality(assignment[t][e], 0);
                            unavailabilityConstraints++;
                        }
                    }
                }
            }
            totalConstraintsAdded += unavailabilityConstraints;
            System.out.println("✓ Unavailability: HARD (" + unavailabilityConstraints + " constraints, " +
                    teachersWithRelaxedUnavailability.size() + " teachers relaxed)");
        }

        // 5. Teacher quota limits
        if (config.getQuotaLimitMode() == AssignmentConstraintConfig.ConstraintMode.HARD) {
            for (int t = 0; t < numTeachers; t++) {
                LinearExprBuilder sum = LinearExpr.newBuilder();
                for (int e = 0; e < numExams; e++) {
                    sum.addTerm(assignment[t][e], 1);
                }
                model.addLessOrEqual(sum, effectiveQuotas[t]);
                totalConstraintsAdded++;
            }
            System.out.println("✓ Quota limits: HARD");
        }

        // 6. No time conflicts - one exam per slot
        if (config.getTimeConflictMode() == AssignmentConstraintConfig.ConstraintMode.HARD) {
            int timeConflictConstraints = 0;
            for (int t = 0; t < numTeachers; t++) {
                Map<String, List<Integer>> timeSlots = new HashMap<>();
                for (int e = 0; e < numExams; e++) {
                    Exam exam = exams.get(e);
                    String key = exam.day + "_" + exam.seance;
                    timeSlots.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
                }
                for (List<Integer> examsInSlot : timeSlots.values()) {
                    if (examsInSlot.size() > 1) {
                        List<Literal> assignments = new ArrayList<>();
                        for (int e : examsInSlot) {
                            assignments.add(assignment[t][e]);
                        }
                        model.addAtMostOne(assignments);
                        timeConflictConstraints++;
                    }
                }
            }
            totalConstraintsAdded += timeConflictConstraints;
            System.out.println("✓ Time conflicts: HARD");
        }

        // 7. No gaps in daily schedule (configurable)
        int noGapsConstraints = 0;
        Map<Integer, BoolVar> gapVariables = new HashMap<>(); // For SOFT mode

        if (config.getNoGapsMode() == AssignmentConstraintConfig.ConstraintMode.HARD) {
            // HARD mode: Enforce no gaps as constraints
            for (int t = 0; t < numTeachers; t++) {
                if (!teacherParticipateSurveillance[t]) continue;

                Map<Integer, Map<Integer, List<Integer>>> examsByDaySeance = new HashMap<>();

                for (int e = 0; e < numExams; e++) {
                    Exam exam = exams.get(e);
                    examsByDaySeance
                            .computeIfAbsent(exam.day, k -> new HashMap<>())
                            .computeIfAbsent(exam.seance, k -> new ArrayList<>())
                            .add(e);
                }

                for (Map.Entry<Integer, Map<Integer, List<Integer>>> dayEntry : examsByDaySeance.entrySet()) {
                    int day = dayEntry.getKey();
                    Map<Integer, List<Integer>> seanceToExams = dayEntry.getValue();

                    List<Integer> availableSeances = new ArrayList<>(seanceToExams.keySet());
                    Collections.sort(availableSeances);

                    if (availableSeances.size() <= 2) continue;

                    // Check if teacher has unavailability on this day
                    boolean hasUnavailabilityOnDay = false;
                    if (config.isNoGapsSkipUnavailableTeachers() &&
                            !teachersWithRelaxedUnavailability.contains(t)) {
                        for (int seance : availableSeances) {
                            if (day < teacherUnavailable[t].length &&
                                    seance < teacherUnavailable[t][day].length &&
                                    teacherUnavailable[t][day][seance]) {
                                hasUnavailabilityOnDay = true;
                                break;
                            }
                        }
                    }

                    if (hasUnavailabilityOnDay && config.isNoGapsSkipUnavailableTeachers()) {
                        continue; // Skip this teacher/day
                    }

                    Map<Integer, BoolVar> worksInSeance = new HashMap<>();

                    for (int seance : availableSeances) {
                        BoolVar works = model.newBoolVar("T" + t + "_Day" + day + "_Seance" + seance);
                        worksInSeance.put(seance, works);

                        List<Integer> examsInSeance = seanceToExams.get(seance);
                        LinearExprBuilder sum = LinearExpr.newBuilder();
                        for (int examIdx : examsInSeance) {
                            sum.addTerm(assignment[t][examIdx], 1);
                        }

                        model.addGreaterOrEqual(sum, works);
                        model.addLessOrEqual(sum, LinearExpr.term(works, examsInSeance.size()));
                    }

                    for (int i = 0; i < availableSeances.size() - 2; i++) {
                        int seanceStart = availableSeances.get(i);
                        int seanceMiddle = availableSeances.get(i + 1);
                        int seanceEnd = availableSeances.get(i + 2);

                        LinearExprBuilder noGap = LinearExpr.newBuilder();
                        noGap.addTerm(worksInSeance.get(seanceStart), 1);
                        noGap.addTerm(worksInSeance.get(seanceEnd), 1);
                        noGap.addTerm(worksInSeance.get(seanceMiddle), -1);

                        model.addLessOrEqual(noGap, 1);
                        noGapsConstraints++;
                    }
                }
            }

            totalConstraintsAdded += noGapsConstraints;
            System.out.println("✓ No gaps: HARD (" + noGapsConstraints + " constraints, " +
                    (config.isNoGapsSkipUnavailableTeachers() ?
                            "skipping teachers with unavailability" : "all teachers") + ")");

        } else if (config.getNoGapsMode() == AssignmentConstraintConfig.ConstraintMode.SOFT) {
            // SOFT mode: Track gaps as variables for objective
            int gapVarCount = 0;
            for (int t = 0; t < numTeachers; t++) {
                if (!teacherParticipateSurveillance[t]) continue;

                Map<Integer, Map<Integer, List<Integer>>> examsByDaySeance = new HashMap<>();

                for (int e = 0; e < numExams; e++) {
                    Exam exam = exams.get(e);
                    examsByDaySeance
                            .computeIfAbsent(exam.day, k -> new HashMap<>())
                            .computeIfAbsent(exam.seance, k -> new ArrayList<>())
                            .add(e);
                }

                for (Map.Entry<Integer, Map<Integer, List<Integer>>> dayEntry : examsByDaySeance.entrySet()) {
                    int day = dayEntry.getKey();
                    Map<Integer, List<Integer>> seanceToExams = dayEntry.getValue();

                    List<Integer> availableSeances = new ArrayList<>(seanceToExams.keySet());
                    Collections.sort(availableSeances);

                    if (availableSeances.size() <= 2) continue;

                    Map<Integer, BoolVar> worksInSeance = new HashMap<>();

                    for (int seance : availableSeances) {
                        BoolVar works = model.newBoolVar("T" + t + "_Day" + day + "_Seance" + seance + "_soft");
                        worksInSeance.put(seance, works);

                        List<Integer> examsInSeance = seanceToExams.get(seance);
                        LinearExprBuilder sum = LinearExpr.newBuilder();
                        for (int examIdx : examsInSeance) {
                            sum.addTerm(assignment[t][examIdx], 1);
                        }

                        model.addGreaterOrEqual(sum, works);
                        model.addLessOrEqual(sum, LinearExpr.term(works, examsInSeance.size()));
                    }

                    for (int i = 0; i < availableSeances.size() - 2; i++) {
                        int seanceStart = availableSeances.get(i);
                        int seanceMiddle = availableSeances.get(i + 1);
                        int seanceEnd = availableSeances.get(i + 2);

                        BoolVar hasGap = model.newBoolVar("gap_T" + t + "_D" + day + "_" + i);

                        LinearExprBuilder gapSum = LinearExpr.newBuilder();
                        gapSum.addTerm(worksInSeance.get(seanceStart), 1);
                        gapSum.addTerm(worksInSeance.get(seanceEnd), 1);
                        gapSum.addTerm(worksInSeance.get(seanceMiddle), -1);

                        IntVar gapSumVar = model.newIntVar(-1, 3, "gapSum_" + t + "_" + day + "_" + i);
                        model.addEquality(gapSumVar, gapSum);

                        // hasGap = 1 iff gapSumVar >= 2
                        model.addGreaterOrEqual(LinearExpr.affine(gapSumVar, 1, -1), hasGap);
                        model.addLessOrEqual(gapSumVar, LinearExpr.affine(hasGap, 3, -1));

                        gapVariables.put(gapVarCount++, hasGap);
                    }
                }
            }

            System.out.println("✓ No gaps: SOFT (" + gapVarCount + " potential gaps will be penalized)");

        } else {
            System.out.println("✓ No gaps: DISABLED");
        }

// 8. Equal assignments for same grade with quota adjustment (fairness constraint)
        if (config.getEqualAssignmentMode() == AssignmentConstraintConfig.ConstraintMode.HARD) {
            int equalAssignmentConstraints = 0;

            // Group teachers by grade
            Map<String, List<Integer>> teachersByGrade = new HashMap<>();
            for (int t = 0; t < numTeachers; t++) {
                if (teacherParticipateSurveillance[t] && effectiveQuotas[t] > 0) {
                    String grade = teacherGrades[t];
                    if (grade != null && !grade.isEmpty()) {
                        teachersByGrade.computeIfAbsent(grade, k -> new ArrayList<>()).add(t);
                    }
                }
            }

            // For each grade group with 2+ teachers, enforce proportional assignments
            for (Map.Entry<String, List<Integer>> entry : teachersByGrade.entrySet()) {
                String grade = entry.getKey();
                List<Integer> teachers = entry.getValue();

                if (teachers.size() < 2) continue; // Skip if only one teacher has this grade

                // Find the most common quota in this grade (baseline)
                Map<Integer, Integer> quotaFrequency = new HashMap<>();
                for (int t : teachers) {
                    quotaFrequency.put(effectiveQuotas[t],
                            quotaFrequency.getOrDefault(effectiveQuotas[t], 0) + 1);
                }

                int baselineQuota = quotaFrequency.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(effectiveQuotas[teachers.get(0)]);

                // Find a reference teacher with baseline quota
                int referenceTeacher = teachers.stream()
                        .filter(t -> effectiveQuotas[t] == baselineQuota)
                        .findFirst()
                        .orElse(teachers.get(0));

                // Enforce: assignments[teacher] = assignments[reference] + (quota[teacher] - baseline)
                for (int teacher : teachers) {
                    if (teacher == referenceTeacher) continue;

                    int quotaDifference = effectiveQuotas[teacher] - baselineQuota;

                    LinearExprBuilder sumTeacher = LinearExpr.newBuilder();
                    LinearExprBuilder sumReference = LinearExpr.newBuilder();

                    for (int e = 0; e < numExams; e++) {
                        sumTeacher.addTerm(assignment[teacher][e], 1);
                        sumReference.addTerm(assignment[referenceTeacher][e], 1);
                    }

                    // sumTeacher = sumReference + quotaDifference
                    model.addEquality(sumTeacher, LinearExpr.affine(sumReference, 1, quotaDifference));
                    equalAssignmentConstraints++;
                }
            }

            totalConstraintsAdded += equalAssignmentConstraints;
            System.out.println("✓ Equal assignments for same grade (quota-adjusted): HARD (" +
                    equalAssignmentConstraints + " constraints across " +
                    teachersByGrade.size() + " grade groups)");

            // Log grade group details
            for (Map.Entry<String, List<Integer>> entry : teachersByGrade.entrySet()) {
                if (entry.getValue().size() > 1) {
                    String grade = entry.getKey();
                    List<Integer> teachers = entry.getValue();

                    Map<Integer, Long> quotaDistribution = teachers.stream()
                            .collect(java.util.stream.Collectors.groupingBy(
                                    t -> effectiveQuotas[t],
                                    java.util.stream.Collectors.counting()
                            ));

                    System.out.println("  Grade " + grade + ": " + teachers.size() +
                            " teachers, quotas: " + quotaDistribution);
                }
            }
        }



        // 9. PRIORITY STRATEGY: Build optimization objective
        System.out.println("\n--- Building Optimization Objective ---");

        LinearExprBuilder objectiveBuilder = LinearExpr.newBuilder();
        int totalPenaltyTerms = 0;

        // 8a. Build conflict map
        int[] examConflictScore = new int[numExams];

        if (config.isOptimizeConflictAvoidance()) {
            for (int e = 0; e < numExams; e++) {
                Exam exam = exams.get(e);
                int conflictCount = 0;

                for (int t = 0; t < numTeachers; t++) {
                    if (!teacherParticipateSurveillance[t] || teachersWithRelaxedUnavailability.contains(t)) {
                        continue;
                    }

                    if (exam.day < teacherUnavailable[t].length &&
                            exam.seance < teacherUnavailable[t][exam.day].length &&
                            teacherUnavailable[t][exam.day][exam.seance]) {
                        conflictCount++;
                    }
                }
                examConflictScore[e] = conflictCount;
            }
        }

        // 8b. Penalty for unavailability violations (relaxed teachers)
        int unavailabilityViolationTerms = 0;
        for (int e = 0; e < numExams; e++) {
            Exam exam = exams.get(e);

            for (int t = 0; t < numTeachers; t++) {
                if (!teacherParticipateSurveillance[t]) continue;

                if (teachersWithRelaxedUnavailability.contains(t)) {
                    if (exam.day < teacherUnavailable[t].length &&
                            exam.seance < teacherUnavailable[t][exam.day].length &&
                            teacherUnavailable[t][exam.day][exam.seance]) {
                        objectiveBuilder.addTerm(assignment[t][e], config.getUnavailabilityViolationPenalty());
                        unavailabilityViolationTerms++;
                    }
                }

                // Conflict avoidance penalty
                if (config.isOptimizeConflictAvoidance() && examConflictScore[e] > 0) {
                    objectiveBuilder.addTerm(assignment[t][e], examConflictScore[e] * config.getConflictAvoidancePenalty());
                }
            }
        }

        if (unavailabilityViolationTerms > 0) {
            System.out.println("  - Unavailability violations: " + unavailabilityViolationTerms +
                    " terms (weight: " + config.getUnavailabilityViolationPenalty() + ")");
            totalPenaltyTerms += unavailabilityViolationTerms;
        }

        if (config.isOptimizeConflictAvoidance()) {
            int conflictTerms = (int) Arrays.stream(examConflictScore).filter(s -> s > 0).count() * numTeachers;
            System.out.println("  - Conflict avoidance: " + conflictTerms +
                    " terms (weight: " + config.getConflictAvoidancePenalty() + ")");
            totalPenaltyTerms += conflictTerms;
        }

        // 8c. SOFT Owner presence preference
        int ownerPresenceBonuses = 0;

        if (config.getOwnerPresenceMode() == AssignmentConstraintConfig.ConstraintMode.SOFT) {
            Map<String, List<Integer>> examsBySlot = new HashMap<>();
            for (int e = 0; e < numExams; e++) {
                Exam exam = exams.get(e);
                String slotKey = exam.day + "_" + exam.seance;
                examsBySlot.computeIfAbsent(slotKey, k -> new ArrayList<>()).add(e);
            }

            for (Map.Entry<String, List<Integer>> slotEntry : examsBySlot.entrySet()) {
                List<Integer> examsInSlot = slotEntry.getValue();

                if (examsInSlot.size() <= 1) continue;

                for (int e : examsInSlot) {
                    Exam exam = exams.get(e);
                    String examKey = exam.day + "_" + exam.seance + "_" + exam.salle;
                    Set<Long> ownerIds = examOwnersByKey.getOrDefault(examKey, new HashSet<>());

                    if (ownerIds.isEmpty()) continue;

                    for (Long ownerId : ownerIds) {
                        if (!teacherIdToIndex.containsKey(ownerId)) continue;
                        int ownerIdx = teacherIdToIndex.get(ownerId);

                        if (!teacherParticipateSurveillance[ownerIdx]) continue;

                        for (int otherExamIdx : examsInSlot) {
                            if (otherExamIdx == e) continue;

                            Exam otherExam = exams.get(otherExamIdx);
                            String otherExamKey = otherExam.day + "_" + otherExam.seance + "_" + otherExam.salle;
                            Set<Long> otherOwners = examOwnersByKey.getOrDefault(otherExamKey, new HashSet<>());

                            if (!otherOwners.contains(ownerId)) {
                                // BONUS (negative = reward when minimizing)
                                objectiveBuilder.addTerm(assignment[ownerIdx][otherExamIdx],
                                        -config.getOwnerPresencePenalty());
                                ownerPresenceBonuses++;
                            }
                        }
                    }
                }
            }

            System.out.println("  - Owner presence bonus: " + ownerPresenceBonuses +
                    " opportunities (bonus: -" + config.getOwnerPresencePenalty() + ")");
            totalPenaltyTerms += ownerPresenceBonuses;
        }

        // 8d. SOFT Gap penalties
        if (config.getNoGapsMode() == AssignmentConstraintConfig.ConstraintMode.SOFT) {
            for (BoolVar gapVar : gapVariables.values()) {
                objectiveBuilder.addTerm(gapVar, config.getNoGapsPenalty());
            }
            System.out.println("  - Gap penalties: " + gapVariables.size() +
                    " gaps (weight: " + config.getNoGapsPenalty() + ")");
            totalPenaltyTerms += gapVariables.size();
        }


        // Set objective to minimize
        if (totalPenaltyTerms > 0) {
            model.minimize(objectiveBuilder);
            System.out.println("\n✓ Objective function set: Minimize penalties (" + totalPenaltyTerms + " terms)");
        } else {
            System.out.println("\n✓ No objective needed - standard assignment");
        }

        System.out.println("----------------------------------------------");
        System.out.println("Total hard constraints: " + totalConstraintsAdded);
        System.out.println("----------------------------------------------\n");
    }


    private AssignmentResponseModel solve() {
        long startTime = System.currentTimeMillis();
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(10.0);

        CpSolverStatus status = solver.solve(model);
        double solutionTime = (System.currentTimeMillis() - startTime) / 1000.0;

        System.out.println("Status: " + status + " (Time: " + String.format("%.3f", solutionTime) + "s)");

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            AssignmentResponseModel response = buildSuccessResponse(solver, status, solutionTime);
            // Mark if this required relaxation
            if (!teachersWithRelaxedUnavailability.isEmpty()) {
                response.getMetadata().setIsOptimal(false); // Not optimal for original problem
            }
            return response;
        } else if (status == CpSolverStatus.INFEASIBLE) {
            return buildInfeasibleResponse(solutionTime);
        } else {
            return buildTimeoutResponse(status, solutionTime);
        }
    }

    private AssignmentResponseModel buildSuccessResponse(CpSolver solver, CpSolverStatus status, double solutionTime) {
        List<ExamAssignmentModel> examAssignments = new ArrayList<>();
        int totalAssignments = 0;

        for (int e = 0; e < numExams; e++) {
            Exam exam = exams.get(e);

            // Build the same key used during deduplication
            String examKey = exam.day + "_" + exam.seance + "_" + exam.salle;

            List<AssignedTeacherModel> assignedTeachers = new ArrayList<>();

            for (int t = 0; t < numTeachers; t++) {
                if (solver.booleanValue(assignment[t][e])) {
                    assignedTeachers.add(AssignedTeacherModel.builder()
                            .teacherId(teacherIds[t])
                            .teacherName(teacherNames[t])
                            .teacherGrade(teacherGrades[t])
                            .build());
                    totalAssignments++;
                }
            }

            examAssignments.add(ExamAssignmentModel.builder()
                    .examId(exam.examId)
                    .day(exam.day + 1)
                    .dayLabel("Day " + (exam.day + 1))
                    .seance(exam.seance + 1)
                    .seanceLabel(getSeanceLabel(exam.seance))
                    .room(exam.salle)
                    .requiredSupervisors(exam.requiredSupervisors)
                    .ownerTeacherId(exam.ownerTeacherId)
                    .ownerTeacherName(getTeacherName(exam.ownerTeacherId))
                    // Use KEY to lookup temporal data
                    .examDate(exam.examDate)
                    .startTime(exam.startTime)
                    .endTime(exam.endTime)
                    .assignedTeachers(assignedTeachers)
                    .build());
        }

        List<TeacherWorkloadModel> teacherWorkloads = buildTeacherWorkloads(solver);

        int participatingCount = (int) Arrays.stream(teacherParticipateSurveillance)
                .filter(b -> b).count();

        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .sessionId(currentSession.getId())
                .sessionName(currentSession.getSessionLibelle())
                .totalExams(numExams)
                .totalTeachers(numTeachers)
                .participatingTeachers(participatingCount)
                .solutionTimeSeconds(solutionTime)
                .isOptimal(status == CpSolverStatus.OPTIMAL)
                .totalAssignmentsMade(totalAssignments)
                .totalConstraints(totalConstraintsAdded)
                .relaxationAttempts(relaxationAttemptNumber)
                .build();

        return AssignmentResponseModel.builder()
                .status(AssignmentStatus.SUCCESS)
                .message(status == CpSolverStatus.OPTIMAL ? "Optimal solution found" : "Feasible solution found")
                .metadata(metadata)
                .examAssignments(examAssignments)
                .teacherWorkloads(teacherWorkloads)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private List<TeacherWorkloadModel> buildTeacherWorkloads(CpSolver solver) {
        List<TeacherWorkloadModel> workloads = new ArrayList<>();

        for (int t = 0; t < numTeachers; t++) {
            List<WorkloadDetailModel> assignments = new ArrayList<>();
            int assignedCount = 0;
            int unavailabilityCredit = 0;

            Set<String> assignedTimeSlots = new HashSet<>();
            for (int e = 0; e < numExams; e++) {
                if (solver.booleanValue(assignment[t][e])) {
                    Exam exam = exams.get(e);
                    String timeSlotKey = exam.day + "_" + exam.seance;
                    assignedTimeSlots.add(timeSlotKey);

                    // Build exam key for temporal data lookup
                    String examKey = exam.day + "_" + exam.seance + "_" + exam.salle;

                    assignments.add(WorkloadDetailModel.builder()
                            .examId(exam.examId)
                            .day(exam.day + 1)
                            .dayLabel("Day " + (exam.day + 1))
                            .seance(exam.seance + 1)
                            .seanceLabel(getSeanceLabel(exam.seance))
                            .room(exam.salle)
                            // Use KEY to lookup temporal data
                            .examDate(exam.examDate)
                            .startTime(exam.startTime)
                            .endTime(exam.endTime)
                            .build());
                    assignedCount++;
                }
            }

            // Count unavailability requests that were respected
            for (int e = 0; e < numExams; e++) {
                Exam exam = exams.get(e);

                // Check if teacher marked this exam's time slot as unavailable
                if (exam.day < teacherUnavailable[t].length &&
                        exam.seance < teacherUnavailable[t][exam.day].length &&
                        teacherUnavailable[t][exam.day][exam.seance]) {

                    String timeSlotKey = exam.day + "_" + exam.seance;

                    // If teacher is NOT assigned to this time slot, credit them
                    if (!assignedTimeSlots.contains(timeSlotKey)) {
                        unavailabilityCredit++;
                    }
                }
            }

            double utilization = effectiveQuotas[t] > 0
                    ? (assignedCount * 100.0) / effectiveQuotas[t]
                    : 0.0;

            workloads.add(TeacherWorkloadModel.builder()
                    .teacherId(teacherIds[t])
                    .teacherName(teacherNames[t])
                    .grade(teacherGrades[t])
                    .email(teacherEmails[t])
                    .assignedSupervisions(assignedCount)
                    .quotaSupervisions(effectiveQuotas[t])
                    .unavailabilityCredit(unavailabilityCredit)
                    .utilizationPercentage(utilization)
                    .assignments(assignments)
                    .build());
        }

        return workloads;
    }
    private AssignmentResponseModel buildInfeasibleResponse(double solutionTime) {
        return AssignmentResponseModel.builder()
                .status(AssignmentStatus.INFEASIBLE)
                .message("No feasible solution exists")
                .metadata(AssignmentMetadata.builder()
                        .sessionId(currentSession.getId())
                        .sessionName(currentSession.getSessionLibelle())
                        .totalExams(numExams)
                        .totalTeachers(numTeachers)
                        .solutionTimeSeconds(solutionTime)
                        .build())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private AssignmentResponseModel buildTimeoutResponse(CpSolverStatus status, double solutionTime) {
        return AssignmentResponseModel.builder()
                .status(AssignmentStatus.TIMEOUT)
                .message("Solver timed out. Status: " + status)
                .metadata(AssignmentMetadata.builder()
                        .sessionId(currentSession.getId())
                        .sessionName(currentSession.getSessionLibelle())
                        .totalExams(numExams)
                        .totalTeachers(numTeachers)
                        .solutionTimeSeconds(solutionTime)
                        .build())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private String getSeanceLabel(int seanceOrdinal) {
        SeanceType[] values = SeanceType.values();
        if (seanceOrdinal >= 0 && seanceOrdinal < values.length) {
            return values[seanceOrdinal].name();
        }
        return "Unknown";
    }

    private String getTeacherName(Long teacherId) {
        if (teacherId == null) return "N/A";
        Integer idx = teacherIdToIndex.get(teacherId);
        return idx != null ? teacherNames[idx] : "Unknown";
    }
}