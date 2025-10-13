package org.teacherdistributionsystem.distribution_system.services.assignment;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.*;
import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;
import org.teacherdistributionsystem.distribution_system.models.projections.ExamForAssignmentProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilityProjection;
import org.teacherdistributionsystem.distribution_system.models.responses.*;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherQuotaService;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherService;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherUnavailabilityService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssignmentAlgorithmService {

    private final TeacherQuotaService teacherQuotaService;
    private final TeacherUnavailabilityService teacherUnavailabilityService;
    private final ExamSessionService examSessionService;
    private final ExamService examService;
    private final TeacherService teacherService;

    // Priority thresholds for cascading relaxation
    private static final int[] PRIORITY_THRESHOLDS = {2, 5, 10};
    private int currentPriorityThreshold = -1; // -1 means no relaxation

    static class Exam {
        String examId;
        int day;
        int seance;
        String salle;
        Long ownerTeacherId;

        public Exam(String examId, int day, int seance, String salle, Long ownerTeacherId) {
            this.examId = examId;
            this.day = day;
            this.seance = seance;
            this.salle = salle;
            this.ownerTeacherId = ownerTeacherId;
        }
    }

    private BoolVar[][] assignment;
    private CpModel model;
    private int numTeachers;
    private int numExams;

    private Long[] teacherIds;
    private String[] teacherNames;
    private Boolean[] teacherParticipateSurveillance;
    private String[] teacherGrades;
    private int[] teacherPriorities;

    private boolean[][][] teacherUnavailable;
    private int[] quotaPerTeacher;
    private List<Exam> exams;
    private final int teachersPerExam = 2;
    private Map<Long, Integer> teacherIdToIndex;
    private ExamSessionDto currentSession;

    public AssignmentAlgorithmService(TeacherService teacherService,
                                      TeacherQuotaService teacherQuotaService,
                                      TeacherUnavailabilityService teacherUnavailabilityService,
                                      ExamSessionService examSessionService,
                                      ExamService examService) {
        Loader.loadNativeLibraries();
        this.teacherService = teacherService;
        this.teacherQuotaService = teacherQuotaService;
        this.teacherUnavailabilityService = teacherUnavailabilityService;
        this.examSessionService = examSessionService;
        this.examService = examService;
    }

    public AssignmentResponseModel executeAssignment(Long sessionId) {
        try {
            loadData(sessionId);

            // First attempt: try with all unavailability constraints (strict mode)
            System.out.println("========================================");
            System.out.println("Starting assignment algorithm for session: " + sessionId);
            System.out.println("========================================");

            currentPriorityThreshold = -1;
            model = new CpModel();
            createVariables();
            addConstraints();

            System.out.println("\n[ATTEMPT 1] Trying with strict unavailability constraints (all teachers)...");
            AssignmentResponseModel firstAttempt = solve();

            // If successful, return the result
            if (firstAttempt.getStatus() == AssignmentStatus.SUCCESS) {
                System.out.println("[SUCCESS] Solution found without relaxing any constraints!");
                System.out.println("========================================\n");
                return firstAttempt;
            }

            // If infeasible, try cascading relaxation of unavailability constraints
            if (firstAttempt.getStatus() == AssignmentStatus.INFEASIBLE) {
                System.out.println("[INFEASIBLE] No solution with strict constraints. Trying cascading relaxation...\n");

                // Try each priority threshold level
                int attemptNumber = 2;
                for (int threshold : PRIORITY_THRESHOLDS) {
                    System.out.println("[ATTEMPT " + attemptNumber + "] Checking if relaxation at threshold " + threshold + " can help...");

                    if (canRelaxationHelp(threshold)) {
                        System.out.println("  → Relaxation at threshold " + threshold + " might help. Trying to solve...");
                        currentPriorityThreshold = threshold;
                        model = new CpModel();
                        createVariables();
                        addConstraints();
                        AssignmentResponseModel attempt = solve();

                        if (attempt.getStatus() == AssignmentStatus.SUCCESS) {
                            // Add note about which teachers had unavailability relaxed
                            String relaxationNote = getRelaxationMessage(threshold);
                            System.out.println("[SUCCESS] Solution found with relaxed constraints!");
                            System.out.println("  → " + relaxationNote);
                            System.out.println("========================================\n");
                            attempt.setMessage(attempt.getMessage() + " " + relaxationNote);
                            return attempt;
                        } else {
                            System.out.println("  → Still infeasible at threshold " + threshold);
                        }
                    } else {
                        System.out.println("  → Relaxation at threshold " + threshold + " won't help. Skipping...");
                    }

                    attemptNumber++;
                }

                // If all relaxation attempts failed, return the original infeasible result
                System.out.println("\n[FAILED] All relaxation attempts exhausted. No feasible solution found.");
                System.out.println("========================================\n");
                return firstAttempt;
            }

            System.out.println("========================================\n");
            return firstAttempt;

        } catch (Exception e) {
            System.err.println("[ERROR] Exception during assignment: " + e.getMessage());
            e.printStackTrace();
            return AssignmentResponseModel.builder()
                    .status(AssignmentStatus.ERROR)
                    .message("Error executing assignment: " + e.getMessage())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }

    private void loadData(Long sessionId) throws BadRequestException {
        System.out.println("\n=== LOADING DATA ===");

        Map<Long, Boolean> map = teacherService.getTeacherParticipeSurveillance();
        numTeachers = map.size();
        teacherIds = map.keySet().toArray(Long[]::new);
        teacherParticipateSurveillance = map.values().toArray(Boolean[]::new);

        teacherGrades = new String[numTeachers];
        teacherNames = new String[numTeachers];
        quotaPerTeacher = new int[numTeachers];
        teacherPriorities = new int[numTeachers];

        Map<Long, String> gradeMap = teacherService.getAllGrades();
        Map<Long, String> nameMap = teacherService.getAllNames();
        Map<Long, Integer> quotaMap = teacherQuotaService.getAllQuotas();

        int participatingCount = 0;
        Map<Integer, Integer> priorityDistribution = new HashMap<>();

        for (int i = 0; i < numTeachers; i++) {
            teacherGrades[i] = gradeMap.get(teacherIds[i]);
            teacherNames[i] = nameMap.getOrDefault(teacherIds[i], "Unknown");
            quotaPerTeacher[i] = quotaMap.getOrDefault(teacherIds[i], 0); // FIX 1: Handle missing quota

            // Get priority from GradeType enum
            try {
                GradeType gradeType = GradeType.valueOf(teacherGrades[i]);
                teacherPriorities[i] = gradeType.getPriority();
            } catch (IllegalArgumentException e) {
                teacherPriorities[i] = Integer.MAX_VALUE; // Unknown grades get lowest priority
            }

            if (teacherParticipateSurveillance[i]) {
                participatingCount++;
                priorityDistribution.merge(teacherPriorities[i], 1, Integer::sum);
            }
        }

        System.out.println("Total teachers: " + numTeachers);
        System.out.println("Participating teachers: " + participatingCount);
        System.out.println("Priority distribution of participating teachers:");
        priorityDistribution.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.println("  Priority " + e.getKey() + ": " + e.getValue() + " teachers"));

        teacherIdToIndex = new HashMap<>();
        for (int i = 0; i < numTeachers; i++) {
            teacherIdToIndex.put(teacherIds[i], i);
        }

        currentSession = examSessionService.getExamSessionDto(sessionId);
        List<TeacherUnavailabilityProjection> teacherUnavailability =
                teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(currentSession.getId());

        System.out.println("\n⚠️ Unavailability Query Check:");
        System.out.println("  Session ID: " + currentSession.getId());
        System.out.println("  Unavailability records returned from DB: " + teacherUnavailability.size());
        if (teacherUnavailability.isEmpty()) {
            System.out.println("  ⚠️ WARNING: No unavailability data found!");
            System.out.println("  Check if:");
            System.out.println("    1. Teachers have declared unavailabilities in the database");
            System.out.println("    2. The session ID matches the unavailability records");
            System.out.println("    3. The query method 'getTeacherUnavailabilitiesBySessionId' is correct");
        } else {
            System.out.println("  Sample unavailability records:");
            teacherUnavailability.stream().limit(5).forEach(u ->
                    System.out.println("    - Teacher ID: " + u.getId() +
                            ", Day: " + u.getNumeroJour() +
                            ", Seance: " + u.getSeance()));
        }

        int numDays = currentSession.getNumExamDays();
        int numSeances = SeanceType.values().length;

        teacherUnavailable = new boolean[numTeachers][numDays][numSeances];

        int unavailabilityCount = 0;
        int skippedCount = 0; // Track skipped records
        Map<Integer, Integer> unavailabilityByPriority = new HashMap<>();

        // FIX 2: Add null check for teacher lookup
        for (TeacherUnavailabilityProjection t : teacherUnavailability) {
            Integer teacherIdx = teacherIdToIndex.get(t.getId());

            // Skip if teacher doesn't exist in our teacher list
            if (teacherIdx == null) {
                skippedCount++;
                System.out.println("  ⚠️ WARNING: Skipping unavailability for unknown teacher ID: " + t.getId() +
                        " (Day: " + t.getNumeroJour() + ", Seance: " + t.getSeance() + ")");
                continue;
            }

            int dayIdx = t.getNumeroJour();
            int seanceIdx = SeanceType.valueOf(t.getSeance()).ordinal();

            if (dayIdx >= 0 && dayIdx < numDays && seanceIdx >= 0 && seanceIdx < numSeances) {
                teacherUnavailable[teacherIdx][dayIdx][seanceIdx] = true;
                unavailabilityByPriority.merge(teacherPriorities[teacherIdx], 1, Integer::sum);
                unavailabilityCount++;
            }
        }

        System.out.println("\nTotal unavailability declarations processed: " + unavailabilityCount);
        if (skippedCount > 0) {
            System.out.println("⚠️ Skipped unavailability records (unknown teachers): " + skippedCount);
        }
        System.out.println("Unavailability by priority:");
        unavailabilityByPriority.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.println("  Priority " + e.getKey() + ": " + e.getValue() + " unavailabilities"));

        List<ExamForAssignmentProjection> examsList = examService.getExamsForAssignment(currentSession.getId());

        exams = new ArrayList<>();
        examsList.forEach(e -> {
            int dayIdx = e.getJourNumero() - 1;
            int seanceIdx = e.getSeance().ordinal();

            if (dayIdx >= 0 && dayIdx < numDays && seanceIdx >= 0 && seanceIdx < numSeances) {
                exams.add(new Exam(e.getId(), dayIdx, seanceIdx, e.getNumRooms(),
                        e.getResponsableId()));
            }
        });
        numExams = exams.size();

        System.out.println("\nTotal exams: " + numExams);
        System.out.println("Total supervisions needed: " + (numExams * teachersPerExam));

        // Analyze capacity
        int totalCapacity = 0;
        for (int i = 0; i < numTeachers; i++) {
            if (teacherParticipateSurveillance[i]) {
                totalCapacity += quotaPerTeacher[i];
            }
        }
        System.out.println("Total supervision capacity: " + totalCapacity);
        System.out.println("Capacity utilization if assigned: " +
                String.format("%.1f%%", (numExams * teachersPerExam * 100.0) / totalCapacity));

        System.out.println("===================\n");
    }

    private void createVariables() {
        assignment = new BoolVar[numTeachers][numExams];

        for (int t = 0; t < numTeachers; t++) {
            for (int e = 0; e < numExams; e++) {
                assignment[t][e] = model.newBoolVar(
                        "T" + teacherIds[t] + "_E" + exams.get(e).examId
                );
            }
        }
    }

    private void addConstraints() {
        System.out.println("\n--- Adding Constraints ---");

        // 1. Each exam needs exactly 2 teachers
        for (int e = 0; e < numExams; e++) {
            LinearExprBuilder sum = LinearExpr.newBuilder();
            for (int t = 0; t < numTeachers; t++) {
                sum.addTerm(assignment[t][e], 1);
            }
            model.addEquality(sum, teachersPerExam);
        }
        System.out.println("✓ Added constraint: Each exam needs exactly " + teachersPerExam + " teachers");

        // 2. Teachers who don't participate can't be assigned
        int nonParticipating = 0;
        for (int t = 0; t < numTeachers; t++) {
            if (!teacherParticipateSurveillance[t]) {
                for (int e = 0; e < numExams; e++) {
                    model.addEquality(assignment[t][e], 0);
                }
                nonParticipating++;
            }
        }
        System.out.println("✓ Added constraint: Non-participating teachers excluded (" + nonParticipating + " teachers)");

        // 3. Unavailability (with priority-based cascading relaxation)
        int unavailabilityConstraintsAdded = 0;
        int unavailabilityConstraintsSkipped = 0;

        for (int t = 0; t < numTeachers; t++) {
            for (int e = 0; e < numExams; e++) {
                Exam exam = exams.get(e);
                if (exam.day < teacherUnavailable[t].length &&
                        exam.seance < teacherUnavailable[t][exam.day].length) {
                    if (teacherUnavailable[t][exam.day][exam.seance]) {

                        // If relaxation is active and teacher's priority exceeds threshold,
                        // skip adding this constraint (allow assignment despite unavailability)
                        if (currentPriorityThreshold >= 0 &&
                                teacherPriorities[t] > currentPriorityThreshold) {
                            unavailabilityConstraintsSkipped++;
                            continue;
                        }

                        // Otherwise, enforce unavailability as hard constraint
                        model.addEquality(assignment[t][e], 0);
                        unavailabilityConstraintsAdded++;
                    }
                }
            }
        }

        if (currentPriorityThreshold >= 0) {
            System.out.println("✓ Unavailability constraints (RELAXED MODE - threshold=" + currentPriorityThreshold + "):");
            System.out.println("    - Constraints enforced: " + unavailabilityConstraintsAdded);
            System.out.println("    - Constraints relaxed: " + unavailabilityConstraintsSkipped);
        } else {
            System.out.println("✓ Added constraint: Teacher unavailability (STRICT MODE - " + unavailabilityConstraintsAdded + " constraints)");
        }

        // 4. Teacher quota
        for (int t = 0; t < numTeachers; t++) {
            LinearExprBuilder sum = LinearExpr.newBuilder();
            for (int e = 0; e < numExams; e++) {
                sum.addTerm(assignment[t][e], 1);
            }
            model.addLessOrEqual(sum, quotaPerTeacher[t]);
        }
        System.out.println("✓ Added constraint: Teacher quota limits");

        // 5. Can't supervise own exam (but can supervise other exams at same time)
        int ownershipConstraints = 0;
        for (int e = 0; e < numExams; e++) {
            Exam exam = exams.get(e);
            Long ownerId = exam.ownerTeacherId;
            if (ownerId != null && teacherIdToIndex.containsKey(ownerId)) {
                int ownerIdx = teacherIdToIndex.get(ownerId);
                // Owner cannot supervise their own exam
                model.addEquality(assignment[ownerIdx][e], 0);
                ownershipConstraints++;
            }
        }
        System.out.println("✓ Added constraint: Can't supervise own exam (" + ownershipConstraints + " constraints)");

        // 6. No time conflicts - teacher can only supervise one exam per time slot
        // Note: A teacher CAN supervise another exam during their own exam's time slot
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
                    // Teacher can supervise at most one exam in this time slot
                    // (even if one of the exams is their own - they're already excluded from that)
                    List<Literal> assignments = new ArrayList<>();
                    for (int e : examsInSlot) {
                        assignments.add(assignment[t][e]);
                    }
                    model.addAtMostOne(assignments);
                    timeConflictConstraints++;
                }
            }
        }
        System.out.println("✓ Added constraint: No time conflicts - max 1 exam per slot (" + timeConflictConstraints + " constraints)");
        System.out.println("--- Constraints Added Successfully ---\n");
    }

    private boolean canRelaxationHelp(int priorityThreshold) {
        // Check if relaxing unavailability for teachers with priority > threshold
        // would provide enough teachers to make the problem feasible
        System.out.println("\n  === Analyzing Relaxation at Threshold " + priorityThreshold + " ===");

        Map<String, List<Integer>> timeSlots = new HashMap<>();
        for (int e = 0; e < numExams; e++) {
            Exam exam = exams.get(e);
            String key = exam.day + "_" + exam.seance;
            timeSlots.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        boolean canHelp = true;
        int problematicSlots = 0;
        int improvementSlots = 0;

        for (Map.Entry<String, List<Integer>> entry : timeSlots.entrySet()) {
            List<Integer> examIndices = entry.getValue();
            Exam firstExam = exams.get(examIndices.getFirst());
            int day = firstExam.day;
            int seance = firstExam.seance;

            int teachersNeeded = examIndices.size() * teachersPerExam;
            int availableStrict = 0;
            int availableWithRelaxation = 0;

            for (int t = 0; t < numTeachers; t++) {
                if (!teacherParticipateSurveillance[t]) continue;

                boolean isAvailable = day < teacherUnavailable[t].length &&
                        seance < teacherUnavailable[t][day].length &&
                        !teacherUnavailable[t][day][seance];

                if (isAvailable) {
                    availableStrict++;
                    availableWithRelaxation++;
                } else if (teacherPriorities[t] > priorityThreshold) {
                    // Teacher is unavailable but can be forced (low priority)
                    availableWithRelaxation++;
                }
            }

            boolean strictlyInsufficient = teachersNeeded > availableStrict;
            boolean relaxedSufficient = teachersNeeded <= availableWithRelaxation;

            if (strictlyInsufficient && relaxedSufficient) {
                improvementSlots++;
            }

            if (teachersNeeded > availableWithRelaxation) {
                canHelp = false;
                problematicSlots++;
                System.out.println("    ❌ Slot Day " + (day+1) + " Seance " + (seance+1) +
                        ": Need " + teachersNeeded + ", Strict=" + availableStrict +
                        ", Relaxed=" + availableWithRelaxation + " - STILL INSUFFICIENT");
            } else if (strictlyInsufficient) {
                System.out.println("    ✓ Slot Day " + (day+1) + " Seance " + (seance+1) +
                        ": Need " + teachersNeeded + ", Strict=" + availableStrict +
                        ", Relaxed=" + availableWithRelaxation + " - IMPROVED");
            }
        }

        System.out.println("  Summary: " + (canHelp ? "✓ CAN HELP" : "❌ WON'T HELP"));
        System.out.println("    - Slots improved by relaxation: " + improvementSlots);
        System.out.println("    - Slots still problematic: " + problematicSlots);
        System.out.println("  ==========================================\n");

        return canHelp;
    }

    private String getRelaxationMessage(int threshold) {
        List<String> relaxedGrades = new ArrayList<>();

        System.out.println("\n--- Relaxed Teacher Grades (priority > " + threshold + ") ---");
        for (GradeType gradeType : GradeType.values()) {
            if (gradeType.getPriority() > threshold) {
                relaxedGrades.add(gradeType.getLabel());
                System.out.println("  • " + gradeType.name() + " - " + gradeType.getLabel() +
                        " (priority: " + gradeType.getPriority() + ")");
            }
        }
        System.out.println("-----------------------------------------------");

        if (relaxedGrades.isEmpty()) {
            return "(Note: All teacher unavailabilities were relaxed)";
        }

        return "(Note: Unavailability relaxed for: " + String.join(", ", relaxedGrades) + ")";
    }

    private AssignmentResponseModel solve() {
        long startTime = System.currentTimeMillis();
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(10.0);

        System.out.println("Solving with OR-Tools CP-SAT...");
        CpSolverStatus status = solver.solve(model);
        double solutionTime = (System.currentTimeMillis() - startTime) / 1000.0;

        System.out.println("Solver Status: " + status);
        System.out.println("Solution Time: " + String.format("%.3f", solutionTime) + " seconds\n");

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            return buildSuccessResponse(solver, status, solutionTime);
        } else if (status == CpSolverStatus.INFEASIBLE) {
            return buildInfeasibleResponse(solutionTime);
        } else {
            return buildTimeoutResponse(status, solutionTime);
        }
    }

    private AssignmentResponseModel buildSuccessResponse(CpSolver solver, CpSolverStatus status, double solutionTime) {
        List<ExamAssignmentModel> examAssignments = new ArrayList<>();
        int totalAssignments = 0;

        // Build exam assignments
        for (int e = 0; e < numExams; e++) {
            Exam exam = exams.get(e);
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
                    .ownerTeacherId(exam.ownerTeacherId)
                    .ownerTeacherName(getTeacherName(exam.ownerTeacherId))
                    .assignedTeachers(assignedTeachers)
                    .build());
        }

        // Build teacher workloads
        List<TeacherWorkloadModel> teacherWorkloads = buildTeacherWorkloads(solver);

        // Build metadata
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

            for (int e = 0; e < numExams; e++) {
                if (solver.booleanValue(assignment[t][e])) {
                    Exam exam = exams.get(e);
                    assignments.add(WorkloadDetailModel.builder()
                            .examId(exam.examId)
                            .day(exam.day + 1)
                            .dayLabel("Day " + (exam.day + 1))
                            .seance(exam.seance + 1)
                            .seanceLabel(getSeanceLabel(exam.seance))
                            .room(exam.salle)
                            .build());
                    assignedCount++;
                }
            }

            double utilization = quotaPerTeacher[t] > 0
                    ? (assignedCount * 100.0) / quotaPerTeacher[t]
                    : 0.0;

            workloads.add(TeacherWorkloadModel.builder()
                    .teacherId(teacherIds[t])
                    .teacherName(teacherNames[t])
                    .grade(teacherGrades[t])
                    .assignedSupervisions(assignedCount)
                    .quotaSupervisions(quotaPerTeacher[t])
                    .utilizationPercentage(utilization)
                    .assignments(assignments)
                    .build());
        }

        return workloads;
    }

    private AssignmentResponseModel buildInfeasibleResponse(double solutionTime) {
        System.out.println("\n=== INFEASIBILITY DIAGNOSIS ===");

        InfeasibilityDiagnosisModel diagnosis = diagnoseInfeasibility();

        return AssignmentResponseModel.builder()
                .status(AssignmentStatus.INFEASIBLE)
                .message("No feasible solution exists with current constraints")
                .diagnosis(diagnosis)
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

    private InfeasibilityDiagnosisModel diagnoseInfeasibility() {
        // Calculate total capacity vs demand
        int totalCapacity = 0;
        int participatingTeachers = 0;
        for (int i = 0; i < numTeachers; i++) {
            if (teacherParticipateSurveillance[i]) {
                totalCapacity += quotaPerTeacher[i];
                participatingTeachers++;
            }
        }
        int totalDemand = numExams * teachersPerExam;

        System.out.println("Overall Capacity Analysis:");
        System.out.println("  Total demand: " + totalDemand + " supervisions");
        System.out.println("  Total capacity: " + totalCapacity + " supervisions");
        System.out.println("  Deficit: " + (totalDemand - totalCapacity));

        if (totalDemand > totalCapacity) {
            System.out.println("  ⚠️ INSUFFICIENT TOTAL CAPACITY - This is the primary issue!");
        }

        // Analyze time slot distribution
        Map<String, List<Integer>> timeSlots = new HashMap<>();
        for (int e = 0; e < numExams; e++) {
            Exam exam = exams.get(e);
            String key = exam.day + "_" + exam.seance;
            timeSlots.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        List<TimeSlotIssueModel> issues = new ArrayList<>();
        int totalProblems = 0;

        System.out.println("\nTime Slot Analysis:");
        for (Map.Entry<String, List<Integer>> entry : timeSlots.entrySet()) {
            List<Integer> examIndices = entry.getValue();
            Exam firstExam = exams.get(examIndices.getFirst());
            int day = firstExam.day;
            int seance = firstExam.seance;

            int teachersNeeded = examIndices.size() * teachersPerExam;
            int availableCount = 0;

            // For each teacher, check if they can supervise at least one exam in this slot
            for (int t = 0; t < numTeachers; t++) {
                if (!teacherParticipateSurveillance[t]) continue;

                // Check if teacher is available for this time slot
                boolean isAvailableInSlot = day < teacherUnavailable[t].length &&
                        seance < teacherUnavailable[t][day].length &&
                        !teacherUnavailable[t][day][seance];

                if (!isAvailableInSlot) continue;

                // Teacher is available in the time slot
                // They can supervise ANY exam in this slot EXCEPT their own
                // Since they can only supervise 1 exam per slot, count them once
                availableCount++;
            }

            boolean isProblem = teachersNeeded > availableCount;
            if (isProblem) {
                totalProblems++;
                System.out.println("  ❌ Day " + (day+1) + " Seance " + (seance+1) +
                        ": " + examIndices.size() + " exams, need " + teachersNeeded +
                        " teachers, available " + availableCount + " (deficit: " + (teachersNeeded - availableCount) + ")");
            }

            issues.add(TimeSlotIssueModel.builder()
                    .day(day + 1)
                    .dayLabel("Day " + (day + 1))
                    .seance(seance + 1)
                    .seanceLabel(getSeanceLabel(seance))
                    .numberOfExams(examIndices.size())
                    .teachersNeeded(teachersNeeded)
                    .teachersAvailable(availableCount)
                    .deficit(Math.max(0, teachersNeeded - availableCount))
                    .isProblem(isProblem)
                    .build());
        }

        System.out.println("\nProblematic time slots: " + totalProblems + " out of " + timeSlots.size());

        // Analyze ownership conflicts
        int totalOwnerships = 0;
        for (Exam exam : exams) {
            if (exam.ownerTeacherId != null) {
                totalOwnerships++;
            }
        }

        System.out.println("\nOwnership Analysis:");
        System.out.println("  Total exams with owners: " + totalOwnerships + " out of " + numExams);
        System.out.println("  Note: Teachers CAN supervise other exams during their own exam's time slot");
        System.out.println("        They just can't supervise their own exam (already enforced by constraint #5)");

        System.out.println("=================================\n");

        List<String> suggestions = new ArrayList<>();

        if (totalDemand > totalCapacity) {
            suggestions.add("⚠️ PRIMARY ISSUE: Total capacity (" + totalCapacity +
                    ") is less than demand (" + totalDemand + "). You need " +
                    (totalDemand - totalCapacity) + " more supervision slots.");
            suggestions.add("Solution: Increase teacher quotas or add more teachers to supervision pool");
        }

        if (totalProblems > 0) {
            suggestions.add("Some time slots have insufficient available teachers");
            suggestions.add("Consider redistributing exams across more time slots");
        }

        suggestions.add("Review exam scheduling to balance teacher demand across time slots");

        return InfeasibilityDiagnosisModel.builder()
                .summary(totalProblems + " time slot(s) have insufficient teachers. " +
                        "Total capacity deficit: " + Math.max(0, totalDemand - totalCapacity))
                .problematicTimeSlots(issues.stream()
                        .filter(TimeSlotIssueModel::getIsProblem)
                        .collect(Collectors.toList()))
                .suggestions(suggestions)
                .statistics(buildStatistics())
                .build();
    }

    private StatisticsModel buildStatistics() {
        int participating = (int) Arrays.stream(teacherParticipateSurveillance)
                .filter(b -> b).count();

        Map<String, List<Integer>> timeSlots = exams.stream()
                .collect(Collectors.groupingBy(
                        e -> e.day + "_" + e.seance,
                        Collectors.mapping(e -> exams.indexOf(e), Collectors.toList())
                ));

        int maxInSlot = timeSlots.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0) * teachersPerExam;

        return StatisticsModel.builder()
                .totalTeachers(numTeachers)
                .participatingTeachers(participating)
                .totalExams(numExams)
                .totalSlotsUsed(timeSlots.size())
                .averageTeachersPerSlot(numExams * teachersPerExam * 1.0 / timeSlots.size())
                .maxTeachersInOneSlot(maxInSlot)
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