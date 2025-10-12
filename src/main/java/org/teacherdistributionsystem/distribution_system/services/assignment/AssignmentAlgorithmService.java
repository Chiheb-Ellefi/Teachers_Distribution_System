package org.teacherdistributionsystem.distribution_system.services.assignment;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.*;
import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;
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

    private BoolVar[][] assignment;
    private final CpModel model;
    private int numTeachers;
    private int numExams;

    private Long[] teacherIds;
    private String[] teacherNames;
    private Boolean[] teacherParticipateSurveillance;
    private String[] teacherGrades;
    private String[] teacherDepartments;
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
        model = new CpModel();
        this.teacherService = teacherService;
        this.teacherQuotaService = teacherQuotaService;
        this.teacherUnavailabilityService = teacherUnavailabilityService;
        this.examSessionService = examSessionService;
        this.examService = examService;
    }

    /**
     * Main method to execute the assignment algorithm
     */
    public AssignmentResponseModel executeAssignment(Long sessionId) {
        try {
            loadData(sessionId);
            createVariables();
            addConstraints();
            return solve();
        } catch (Exception e) {
            return AssignmentResponseModel.builder()
                    .status(AssignmentStatus.ERROR)
                    .message("Error executing assignment: " + e.getMessage())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }

    private void loadData(Long sessionId) throws BadRequestException {
        Map<Long, Boolean> map = teacherService.getTeacherParticipeSurveillance();
        numTeachers = map.size();
        teacherIds = map.keySet().toArray(Long[]::new);
        teacherParticipateSurveillance = map.values().toArray(Boolean[]::new);

        teacherGrades = new String[numTeachers];
        teacherNames = new String[numTeachers];
        teacherDepartments = new String[numTeachers];
        quotaPerTeacher = new int[numTeachers];

        Map<Long, String> gradeMap = teacherService.getAllGrades();
        Map<Long, String> nameMap = teacherService.getAllNames();
        Map<Long, Integer> quotaMap = teacherQuotaService.getAllQuotas();

        for (int i = 0; i < numTeachers; i++) {
            teacherGrades[i] = gradeMap.get(teacherIds[i]);
            teacherNames[i] = nameMap.getOrDefault(teacherIds[i], "Unknown");
            quotaPerTeacher[i] = quotaMap.get(teacherIds[i]);
        }

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

        teacherUnavailability.forEach(t -> {
            int teacherIdx = teacherIdToIndex.get(t.getId());
            int dayIdx = t.getNumeroJour();
            int seanceIdx = SeanceType.valueOf(t.getSeance()).ordinal();

            if (dayIdx >= 0 && dayIdx < numDays && seanceIdx >= 0 && seanceIdx < numSeances) {
                teacherUnavailable[teacherIdx][dayIdx][seanceIdx] = true;
            }
        });

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
        // 1. Each exam needs exactly 2 teachers
        for (int e = 0; e < numExams; e++) {
            LinearExprBuilder sum = LinearExpr.newBuilder();
            for (int t = 0; t < numTeachers; t++) {
                sum.addTerm(assignment[t][e], 1);
            }
            model.addEquality(sum, teachersPerExam);
        }

        // 2. Teachers who don't participate can't be assigned
        for (int t = 0; t < numTeachers; t++) {
            if (!teacherParticipateSurveillance[t]) {
                for (int e = 0; e < numExams; e++) {
                    model.addEquality(assignment[t][e], 0);
                }
            }
        }

        // 3. Unavailability
        for (int t = 0; t < numTeachers; t++) {
            for (int e = 0; e < numExams; e++) {
                Exam exam = exams.get(e);
                if (exam.day < teacherUnavailable[t].length &&
                        exam.seance < teacherUnavailable[t][exam.day].length) {
                    if (teacherUnavailable[t][exam.day][exam.seance]) {
                        model.addEquality(assignment[t][e], 0);
                    }
                }
            }
        }

        // 4. Teacher quota
        for (int t = 0; t < numTeachers; t++) {
            LinearExprBuilder sum = LinearExpr.newBuilder();
            for (int e = 0; e < numExams; e++) {
                sum.addTerm(assignment[t][e], 1);
            }
            model.addLessOrEqual(sum, quotaPerTeacher[t]);
        }

        // 5. Can't supervise own subject
        for (int e = 0; e < numExams; e++) {
            Exam exam = exams.get(e);
            Long ownerId = exam.ownerTeacherId;
            if (ownerId != null && teacherIdToIndex.containsKey(ownerId)) {
                int ownerIdx = teacherIdToIndex.get(ownerId);
                model.addEquality(assignment[ownerIdx][e], 0);
            }
        }

        // 6. No time conflicts
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
                }
            }
        }
    }

    private AssignmentResponseModel solve() {
        long startTime = System.currentTimeMillis();
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(10.0);

        CpSolverStatus status = solver.solve(model);
        double solutionTime = (System.currentTimeMillis() - startTime) / 1000.0;

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
                    .day(exam.day + 1) // Convert to 1-indexed for display
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
        Map<String, List<Integer>> timeSlots = new HashMap<>();
        for (int e = 0; e < numExams; e++) {
            Exam exam = exams.get(e);
            String key = exam.day + "_" + exam.seance;
            timeSlots.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        List<TimeSlotIssueModel> issues = new ArrayList<>();
        int totalProblems = 0;

        for (Map.Entry<String, List<Integer>> entry : timeSlots.entrySet()) {
            List<Integer> examIndices = entry.getValue();
            Exam firstExam = exams.get(examIndices.getFirst());
            int day = firstExam.day;
            int seance = firstExam.seance;

            int teachersNeeded = examIndices.size() * teachersPerExam;
            int available = 0;

            for (int t = 0; t < numTeachers; t++) {
                if (teacherParticipateSurveillance[t] &&
                        day < teacherUnavailable[t].length &&
                        seance < teacherUnavailable[t][day].length &&
                        !teacherUnavailable[t][day][seance]) {
                    available++;
                }
            }

            boolean isProblem = teachersNeeded > available;
            if (isProblem) totalProblems++;

            issues.add(TimeSlotIssueModel.builder()
                    .day(day + 1)
                    .dayLabel("Day " + (day + 1))
                    .seance(seance + 1)
                    .seanceLabel(getSeanceLabel(seance))
                    .numberOfExams(examIndices.size())
                    .teachersNeeded(teachersNeeded)
                    .teachersAvailable(available)
                    .deficit(Math.max(0, teachersNeeded - available))
                    .isProblem(isProblem)
                    .build());
        }

        List<String> suggestions = Arrays.asList(
                "Add more teachers to the supervision pool",
                "Increase teacher quotas for overloaded teachers",
                "Reduce teacher unavailabilities where possible",
                "Distribute exams across more time slots",
                "Review exam scheduling to balance teacher demand"
        );

        return InfeasibilityDiagnosisModel.builder()
                .summary(totalProblems + " time slot(s) have insufficient teachers")
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