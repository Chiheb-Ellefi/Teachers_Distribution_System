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

    static class TeacherContribution {
        int teacherIdx;
        int contribution; // Number of slots this teacher can contribute

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
    private Boolean[] teacherParticipateSurveillance;
    private String[] teacherGrades;
    private int[] teacherPriorities;

    private boolean[][][] teacherUnavailable; // Original unavailability data
    private int[] baseQuotas; // Original quotas from database
    private int[] effectiveQuotas; // Adjusted quotas after considering unavailability
    private Set<Integer> teachersWithRelaxedUnavailability; // Teachers whose unavailability we're ignoring

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
            teachersWithRelaxedUnavailability = new HashSet<>();

            System.out.println("========================================");
            System.out.println("Starting assignment algorithm for session: " + sessionId);
            System.out.println("========================================");

            // Calculate initial effective quotas (base quota - unavailability slots)
            calculateEffectiveQuotas();

            int totalSupervisionNeeded = numExams * teachersPerExam;
            int totalEffectiveCapacity = calculateTotalEffectiveCapacity();

            System.out.println("\n=== INITIAL CAPACITY CHECK ===");
            System.out.println("Total supervisions needed: " + totalSupervisionNeeded);
            System.out.println("Total effective capacity (after unavailability): " + totalEffectiveCapacity);

            // ATTEMPT 1: Try with all unavailability respected
            if (totalEffectiveCapacity >= totalSupervisionNeeded) {
                System.out.println("✓ Sufficient capacity with all unavailability respected");
                System.out.println("\n[ATTEMPT 1] Solving with strict unavailability constraints...");

                model = new CpModel();
                createVariables();
                addConstraints();
                AssignmentResponseModel result = solve();

                if (result.getStatus() == AssignmentStatus.SUCCESS) {
                    System.out.println("[SUCCESS] Solution found with all unavailability respected!");
                    System.out.println("========================================\n");
                    return result;
                }

                System.out.println("[INFEASIBLE] Even though capacity is sufficient, no valid assignment found.");
                System.out.println("This may be due to time slot conflicts or other constraints.");
            } else {
                System.out.println("✗ Insufficient capacity - need to relax unavailability");
                System.out.println("Deficit: " + (totalSupervisionNeeded - totalEffectiveCapacity) + " supervisions");
            }

            // ATTEMPT 2+: Progressively relax unavailability for low-priority teachers
            System.out.println("\n=== RELAXING UNAVAILABILITY FOR LOW-PRIORITY TEACHERS ===");

            // Group participating teachers by priority (highest priority first)
            List<Integer> participatingTeacherIndices = new ArrayList<>();
            for (int t = 0; t < numTeachers; t++) {
                if (teacherParticipateSurveillance[t]) {
                    participatingTeacherIndices.add(t);
                }
            }

            // Sort by priority (ascending = low priority first for relaxation)
            participatingTeacherIndices.sort(Comparator.comparingInt(t -> -teacherPriorities[t]));

            System.out.println("Teachers sorted by priority (lowest first):");
            for (int t : participatingTeacherIndices) {
                int unavailableSlots = countUnavailableSlots(t);
                System.out.println("  Teacher " + teacherIds[t] + " (" + teacherNames[t] + ")" +
                        " - Priority: " + teacherPriorities[t] +
                        " - Grade: " + teacherGrades[t] +
                        " - Base Quota: " + baseQuotas[t] +
                        " - Unavailable Slots: " + unavailableSlots +
                        " - Effective Quota: " + effectiveQuotas[t]);
            }

            // Calculate how many teachers we need to relax to meet capacity
            int capacityDeficit = totalSupervisionNeeded - totalEffectiveCapacity;
            System.out.println("\nCapacity deficit to fill: " + capacityDeficit);

            // Build list of teachers with their potential contribution
            List<TeacherContribution> contributions = new ArrayList<>();
            for (int teacherIdx : participatingTeacherIndices) {
                int unavailableSlots = countUnavailableSlots(teacherIdx);
                if (unavailableSlots > 0) {
                    contributions.add(new TeacherContribution(teacherIdx, unavailableSlots));
                }
            }

            // Try batch relaxation strategy
            int attemptNumber = 2;
            int batchSize = Math.max(1, (int) Math.ceil(contributions.size() * 0.2)); // Start with 20% of teachers

            while (!contributions.isEmpty()) {
                // Calculate how many teachers we need based on current deficit
                int currentDeficit = totalSupervisionNeeded - calculateTotalEffectiveCapacity();
                if (currentDeficit <= 0) {
                    System.out.println("\n✓ Sufficient capacity reached!");
                    break;
                }

                // Estimate how many teachers we need
                int avgContribution = contributions.stream()
                        .mapToInt(c -> c.contribution)
                        .sum() / contributions.size();
                int estimatedTeachersNeeded = Math.max(1, (currentDeficit + avgContribution - 1) / avgContribution);

                // Add a buffer (50% more) to account for constraint conflicts
                int teachersToAdd = (int) Math.ceil(estimatedTeachersNeeded * 1.5);
                teachersToAdd = Math.min(teachersToAdd, contributions.size()); // Don't exceed available
                teachersToAdd = Math.max(teachersToAdd, batchSize); // At least the batch size

                System.out.println("\n[ATTEMPT " + attemptNumber + "] Adding batch of " + teachersToAdd + " teacher(s)");
                System.out.println("  Current deficit: " + currentDeficit);
                System.out.println("  Estimated teachers needed: " + estimatedTeachersNeeded +
                        " (adding " + teachersToAdd + " with buffer)");

                // Relax the next batch of teachers
                List<TeacherContribution> batch = contributions.subList(0, Math.min(teachersToAdd, contributions.size()));
                int totalRestoredSlots = 0;

                for (TeacherContribution tc : batch) {
                    teachersWithRelaxedUnavailability.add(tc.teacherIdx);
                    effectiveQuotas[tc.teacherIdx] = baseQuotas[tc.teacherIdx];
                    totalRestoredSlots += tc.contribution;

                    System.out.println("  + Teacher " + teacherIds[tc.teacherIdx] +
                            " (" + teacherNames[tc.teacherIdx] + ") - Priority: " +
                            teacherPriorities[tc.teacherIdx] +
                            " - Restoring " + tc.contribution + " slots");
                }

                contributions = contributions.subList(Math.min(teachersToAdd, contributions.size()), contributions.size());

                int newCapacity = calculateTotalEffectiveCapacity();
                System.out.println("  Total restored slots: " + totalRestoredSlots);
                System.out.println("  New total capacity: " + newCapacity + " / " + totalSupervisionNeeded);

                // Try to solve with current batch
                model = new CpModel();
                createVariables();
                addConstraints();
                AssignmentResponseModel result = solve();

                if (result.getStatus() == AssignmentStatus.SUCCESS) {
                    String relaxationNote = getRelaxationMessage();
                    System.out.println("[SUCCESS] Solution found with relaxed unavailability!");
                    System.out.println("  → " + relaxationNote);
                    System.out.println("========================================\n");
                    result.setMessage(result.getMessage() + " " + relaxationNote);
                    return result;
                }

                System.out.println("  → Still infeasible, trying next batch...");
                attemptNumber++;

                // If we still have deficit and teachers available, continue
                // Otherwise break to avoid infinite loop
                if (contributions.isEmpty() || newCapacity >= totalSupervisionNeeded) {
                    break;
                }
            }

            // If we exhausted all teachers and still no solution
            System.out.println("\n[FAILED] All relaxation attempts exhausted. No feasible solution found.");
            System.out.println("========================================\n");

            return buildInfeasibleResponse(0.0);

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
        baseQuotas = new int[numTeachers];
        effectiveQuotas = new int[numTeachers];
        teacherPriorities = new int[numTeachers];

        Map<Long, String> gradeMap = teacherService.getAllGrades();
        Map<Long, String> nameMap = teacherService.getAllNames();
        Map<Long, Integer> quotaMap = teacherQuotaService.getAllQuotas();

        int participatingCount = 0;
        Map<Integer, Integer> priorityDistribution = new HashMap<>();

        for (int i = 0; i < numTeachers; i++) {
            teacherGrades[i] = gradeMap.get(teacherIds[i]);
            teacherNames[i] = nameMap.getOrDefault(teacherIds[i], "Unknown");
            baseQuotas[i] = quotaMap.getOrDefault(teacherIds[i], 0);

            try {
                GradeType gradeType = GradeType.valueOf(teacherGrades[i]);
                teacherPriorities[i] = gradeType.getPriority();
            } catch (IllegalArgumentException e) {
                teacherPriorities[i] = Integer.MAX_VALUE;
            }

            if (teacherParticipateSurveillance[i]) {
                participatingCount++;
                priorityDistribution.merge(teacherPriorities[i], 1, Integer::sum);
            }
        }

        System.out.println("Total teachers: " + numTeachers);
        System.out.println("Participating teachers: " + participatingCount);
        System.out.println("Priority distribution:");
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

        int numDays = currentSession.getNumExamDays();
        int numSeances = SeanceType.values().length;
        teacherUnavailable = new boolean[numTeachers][numDays][numSeances];

        int unavailabilityCount = 0;
        for (TeacherUnavailabilityProjection t : teacherUnavailability) {
            Integer teacherIdx = teacherIdToIndex.get(t.getId());
            if (teacherIdx == null) continue;

            int dayIdx = t.getNumeroJour();
            int seanceIdx = SeanceType.valueOf(t.getSeance()).ordinal();

            if (dayIdx >= 0 && dayIdx < numDays && seanceIdx >= 0 && seanceIdx < numSeances) {
                teacherUnavailable[teacherIdx][dayIdx][seanceIdx] = true;
                unavailabilityCount++;
            }
        }

        System.out.println("Total unavailability declarations: " + unavailabilityCount);

        List<ExamForAssignmentProjection> examsList = examService.getExamsForAssignment(currentSession.getId());
        exams = new ArrayList<>();
        examsList.forEach(e -> {
            int dayIdx = e.getJourNumero() - 1;
            int seanceIdx = e.getSeance().ordinal();
            if (dayIdx >= 0 && dayIdx < numDays && seanceIdx >= 0 && seanceIdx < numSeances) {
                exams.add(new Exam(e.getId(), dayIdx, seanceIdx, e.getNumRooms(), e.getResponsableId()));
            }
        });
        numExams = exams.size();

        System.out.println("Total exams: " + numExams);
        System.out.println("===================\n");
    }

    private void calculateEffectiveQuotas() {
        System.out.println("\n=== CALCULATING EFFECTIVE QUOTAS ===");

        for (int t = 0; t < numTeachers; t++) {
            if (!teacherParticipateSurveillance[t]) {
                effectiveQuotas[t] = 0;
                continue;
            }

            // If this teacher's unavailability is relaxed, use full quota
            if (teachersWithRelaxedUnavailability.contains(t)) {
                effectiveQuotas[t] = baseQuotas[t];
                continue;
            }

            // Otherwise, subtract unavailable slots from quota
            int unavailableSlots = countUnavailableSlots(t);
            effectiveQuotas[t] = Math.max(0, baseQuotas[t] - unavailableSlots);

            System.out.println("Teacher " + teacherIds[t] + ": Base=" + baseQuotas[t] +
                    ", Unavailable=" + unavailableSlots +
                    ", Effective=" + effectiveQuotas[t]);
        }

        System.out.println("===================================\n");
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

    private int calculateTotalEffectiveCapacity() {
        int total = 0;
        for (int t = 0; t < numTeachers; t++) {
            if (teacherParticipateSurveillance[t]) {
                total += effectiveQuotas[t];
            }
        }
        return total;
    }

    private void createVariables() {
        assignment = new BoolVar[numTeachers][numExams];
        for (int t = 0; t < numTeachers; t++) {
            for (int e = 0; e < numExams; e++) {
                assignment[t][e] = model.newBoolVar("T" + teacherIds[t] + "_E" + exams.get(e).examId);
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
        System.out.println("✓ Each exam needs exactly " + teachersPerExam + " teachers");

        // 2. Non-participating teachers can't be assigned
        int nonParticipating = 0;
        for (int t = 0; t < numTeachers; t++) {
            if (!teacherParticipateSurveillance[t]) {
                for (int e = 0; e < numExams; e++) {
                    model.addEquality(assignment[t][e], 0);
                }
                nonParticipating++;
            }
        }
        System.out.println("✓ Non-participating teachers excluded (" + nonParticipating + " teachers)");

        // 3. Unavailability (only for teachers whose unavailability is NOT relaxed)
        int unavailabilityConstraints = 0;
        for (int t = 0; t < numTeachers; t++) {
            // Skip unavailability for teachers in the relaxed set
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
        System.out.println("✓ Unavailability constraints: " + unavailabilityConstraints +
                " (" + teachersWithRelaxedUnavailability.size() + " teachers have relaxed unavailability)");

        // 4. Teacher effective quota
        for (int t = 0; t < numTeachers; t++) {
            LinearExprBuilder sum = LinearExpr.newBuilder();
            for (int e = 0; e < numExams; e++) {
                sum.addTerm(assignment[t][e], 1);
            }
            model.addLessOrEqual(sum, effectiveQuotas[t]);
        }
        System.out.println("✓ Teacher quota limits (using effective quotas)");

        // 5. Can't supervise own exam
        int ownershipConstraints = 0;
        for (int e = 0; e < numExams; e++) {
            Exam exam = exams.get(e);
            if (exam.ownerTeacherId != null && teacherIdToIndex.containsKey(exam.ownerTeacherId)) {
                int ownerIdx = teacherIdToIndex.get(exam.ownerTeacherId);
                model.addEquality(assignment[ownerIdx][e], 0);
                ownershipConstraints++;
            }
        }
        System.out.println("✓ Can't supervise own exam (" + ownershipConstraints + " constraints)");

        // 6. No time conflicts
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
        System.out.println("✓ No time conflicts (" + timeConflictConstraints + " constraints)");
        System.out.println("-------------------------------\n");
    }

    private String getRelaxationMessage() {
        if (teachersWithRelaxedUnavailability.isEmpty()) {
            return "";
        }

        List<String> relaxedTeachers = new ArrayList<>();
        for (int t : teachersWithRelaxedUnavailability) {
            relaxedTeachers.add(teacherNames[t] + " (" + teacherGrades[t] + ")");
        }

        return "(Note: Unavailability ignored for " + teachersWithRelaxedUnavailability.size() +
                " low-priority teacher(s): " + String.join(", ", relaxedTeachers) + ")";
    }

    private AssignmentResponseModel solve() {
        long startTime = System.currentTimeMillis();
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(10.0);

        CpSolverStatus status = solver.solve(model);
        double solutionTime = (System.currentTimeMillis() - startTime) / 1000.0;

        System.out.println("Solver Status: " + status);
        System.out.println("Solution Time: " + String.format("%.3f", solutionTime) + " seconds");

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

            double utilization = effectiveQuotas[t] > 0
                    ? (assignedCount * 100.0) / effectiveQuotas[t]
                    : 0.0;

            workloads.add(TeacherWorkloadModel.builder()
                    .teacherId(teacherIds[t])
                    .teacherName(teacherNames[t])
                    .grade(teacherGrades[t])
                    .assignedSupervisions(assignedCount)
                    .quotaSupervisions(effectiveQuotas[t])
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