package org.teacherdistributionsystem.distribution_system.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.teacherdistributionsystem.distribution_system.config.AssignmentConstraintConfig;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.ExamSessionDto;
import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;
import org.teacherdistributionsystem.distribution_system.enums.SeanceType;
import org.teacherdistributionsystem.distribution_system.models.projections.ExamForAssignmentProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilityProjection;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.AssignmentResponseModel;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentAlgorithmService;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamService;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamSessionService;
import org.teacherdistributionsystem.distribution_system.services.teacher.QuotaPerGradeService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherQuotaService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherUnavailabilityService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentAlgorithmServiceTest {

    @Mock
    private TeacherService teacherService;

    @Mock
    private TeacherQuotaService teacherQuotaService;

    @Mock
    private TeacherUnavailabilityService teacherUnavailabilityService;

    @Mock
    private ExamSessionService examSessionService;

    @Mock
    private ExamService examService;

    @Mock
    private QuotaPerGradeService quotaPerGradeService;

    @InjectMocks
    private AssignmentAlgorithmService assignmentService;

    private static final Long SESSION_ID = 1L;

    @BeforeEach
    void setUp() {
        // Setup default configuration
        assignmentService.setConfig(AssignmentConstraintConfig.defaultConfig());
    }

    // ============= BASIC FEASIBILITY TESTS =============

    @Test
    void testSimpleAssignment_WithSufficientCapacity() throws ExecutionException, InterruptedException {
        // Setup: 2 teachers, 1 exam needing 2 supervisors
        setupBasicScenario(2, 1, 2, 2);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertNotNull(result);
        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());
        assertEquals(1, result.getExamAssignments().size());
        assertEquals(2, result.getExamAssignments().get(0).getAssignedTeachers().size());
    }

    @Test
    void testAssignment_InsufficientCapacity_ShouldFail() throws ExecutionException, InterruptedException {
        // Setup: 2 teachers with quota 1 each, but exam needs 3 supervisors
        setupBasicScenario(2, 1, 3, 1);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertNotNull(result);
        assertEquals(AssignmentStatus.INFEASIBLE, result.getStatus());
        assertTrue(result.getMessage().contains("Insufficient total capacity"));
    }

    @Test
    void testAssignment_ExactCapacityMatch() throws ExecutionException, InterruptedException {
        // Setup: 3 teachers with quota 2 each = 6 total, 2 exams needing 3 each = 6 total
        Map<Long, Boolean> participationMap = Map.of(
                1L, true,
                2L, true,
                3L, true
        );

        Map<Long, String> gradeMap = Map.of(
                1L, "PR",
                2L, "PR",
                3L, "MA"
        );

        Map<Long, String> nameMap = Map.of(
                1L, "Teacher 1",
                2L, "Teacher 2",
                3L, "Teacher 3"
        );

        Map<Long, String> emailMap = Map.of(
                1L, "t1@test.com",
                2L, "t2@test.com",
                3L, "t3@test.com"
        );

        Map<Long, Integer> quotaMap = Map.of(
                1L, 2,
                2L, 2,
                3L, 2
        );

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(gradeMap);
        when(teacherService.getAllNames()).thenReturn(nameMap);
        when(teacherService.getAllEmails()).thenReturn(emailMap);
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        List<ExamForAssignmentProjection> exams = Arrays.asList(
                createExamProjection("E1", 1, SeanceType.S1, "R1", null, 3),
                createExamProjection("E2", 1, SeanceType.S3, "R2", null, 3)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(Collections.emptyList());

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());
        assertEquals(2, result.getExamAssignments().size());
        assertEquals(6, result.getMetadata().getTotalAssignmentsMade());
    }

    // ============= UNAVAILABILITY TESTS =============

    @Test
    void testAssignment_WithUnavailability_ShouldRespect() throws ExecutionException, InterruptedException {
        // Setup: 3 teachers, 1 exam, teacher 1 is unavailable
        Map<Long, Boolean> participationMap = Map.of(1L, true, 2L, true, 3L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 2, 2L, 2, 3L, 2);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", 2L, "PR", 3L, "MA"));
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "Teacher 1", 2L, "Teacher 2", 3L, "Teacher 3"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "t1@test.com", 2L, "t2@test.com", 3L, "t3@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        List<ExamForAssignmentProjection> exams = List.of(
                createExamProjection("E1", 1, SeanceType.S1, "R1", null, 2)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);

        // Teacher 1 is unavailable during exam time
        List<TeacherUnavailabilityProjection> unavailability = List.of(
                createUnavailability(1L, 0, "S1")
        );
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(unavailability);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());

        // Verify teacher 1 is NOT assigned
        List<Long> assignedTeacherIds = result.getExamAssignments().get(0)
                .getAssignedTeachers().stream()
                .map(t -> t.getTeacherId())
                .toList();

        assertFalse(assignedTeacherIds.contains(1L));
        assertTrue(assignedTeacherIds.contains(2L) || assignedTeacherIds.contains(3L));
    }

    @Test
    void testAssignment_WithRelaxation_WhenUnavailabilityCausesInfeasibility()
            throws ExecutionException, InterruptedException {
        // Setup: All teachers unavailable for exam time, but have capacity
        Map<Long, Boolean> participationMap = Map.of(1L, true, 2L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 2, 2L, 2);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", 2L, "PR"));
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "Teacher 1", 2L, "Teacher 2"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "t1@test.com", 2L, "t2@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        List<ExamForAssignmentProjection> exams = List.of(
                createExamProjection("E1", 1, SeanceType.S1, "R1", null, 2)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);

        // Both teachers unavailable
        List<TeacherUnavailabilityProjection> unavailability = Arrays.asList(
                createUnavailability(1L, 0, "S1"),
                createUnavailability(2L, 0, "S1")
        );
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(unavailability);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());
        assertTrue(result.getMessage().contains("relaxed"));
        assertTrue(result.getMetadata().getRelaxedTeachersCount() > 0);
    }

    // ============= OWNERSHIP TESTS =============

    @Test
    void testAssignment_OwnerExclusion_TeacherCannotSuperviseOwnExam()
            throws ExecutionException, InterruptedException {
        // Setup: Teacher 1 owns exam, should not be assigned to it
        setupBasicScenario(3, 1, 2, 2);

        // Override exam to have owner
        List<ExamForAssignmentProjection> exams = List.of(
                createExamProjection("E1", 1, SeanceType.S1, "R1", 1L, 2)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());

        List<Long> assignedTeacherIds = result.getExamAssignments().get(0)
                .getAssignedTeachers().stream()
                .map(t -> t.getTeacherId())
                .toList();

        assertFalse(assignedTeacherIds.contains(1L), "Owner should not supervise own exam");
    }

    @Test
    void testAssignment_OwnerPresence_HardMode_OwnerMustBeInSlot()
            throws ExecutionException, InterruptedException {
        // Configure HARD owner presence using setters
        AssignmentConstraintConfig config = new AssignmentConstraintConfig();
        config.setOwnerPresenceMode(AssignmentConstraintConfig.ConstraintMode.HARD);
        assignmentService.setConfig(config);

        // Setup: 2 exams in same slot, one owned by teacher 1
        Map<Long, Boolean> participationMap = Map.of(1L, true, 2L, true, 3L, true, 4L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 2, 2L, 2, 3L, 2, 4L, 2);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", 2L, "PR",
                3L, "MA", 4L, "MA"));
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "Owner", 2L, "Teacher 2", 3L, "Teacher 3", 4L, "Teacher 4"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "owner@test.com", 2L, "t2@test.com",
                3L, "t3@test.com", 4L, "t4@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        // Two exams in same slot, teacher 1 owns first exam
        List<ExamForAssignmentProjection> exams = Arrays.asList(
                createExamProjection("E1", 1, SeanceType.S1, "R1", 1L, 2),
                createExamProjection("E2", 1, SeanceType.S1, "R2", null, 2)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(Collections.emptyList());

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());

        // Teacher 1 (owner) should be assigned to E2 (not E1)
        List<Long> e2Teachers = result.getExamAssignments().stream()
                .filter(e -> e.getExamId().equals("E2"))
                .findFirst()
                .get()
                .getAssignedTeachers().stream()
                .map(t -> t.getTeacherId())
                .toList();

        assertTrue(e2Teachers.contains(1L), "Owner should be present in same slot");
    }

    // ============= QUOTA TESTS =============

    @Test
    void testAssignment_RespectsQuotaLimits() throws ExecutionException, InterruptedException {
        // Setup: 1 teacher with quota 1, 2 exams needing 1 supervisor each
        Map<Long, Boolean> participationMap = Map.of(1L, true, 2L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 1, 2L, 2); // Teacher 1 limited to 1

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", 2L, "PR"));
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "Teacher 1", 2L, "Teacher 2"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "t1@test.com", 2L, "t2@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        List<ExamForAssignmentProjection> exams = Arrays.asList(
                createExamProjection("E1", 1, SeanceType.S1, "R1", null, 1),
                createExamProjection("E2", 1, SeanceType.S3, "R2", null, 1)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(Collections.emptyList());

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());

        // Check teacher 1's workload
        long teacher1Assignments = result.getTeacherWorkloads().stream()
                .filter(w -> w.getTeacherId().equals(1L))
                .findFirst()
                .get()
                .getAssignedSupervisions();

        assertTrue(teacher1Assignments <= 1, "Teacher 1 should not exceed quota of 1");
    }

    @Test
    void testAssignment_NonParticipatingTeachersExcluded() throws ExecutionException, InterruptedException {
        // Setup: Teacher 1 not participating
        Map<Long, Boolean> participationMap = Map.of(1L, false, 2L, true, 3L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 5, 2L, 2, 3L, 2);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", 2L, "PR", 3L, "MA"));
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "Non-participant", 2L, "Teacher 2", 3L, "Teacher 3"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "np@test.com", 2L, "t2@test.com", 3L, "t3@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        List<ExamForAssignmentProjection> exams = List.of(
                createExamProjection("E1", 1, SeanceType.S1, "R1", null, 2)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(Collections.emptyList());

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());

        // Teacher 1 should have 0 assignments
        long teacher1Assignments = result.getTeacherWorkloads().stream()
                .filter(w -> w.getTeacherId().equals(1L))
                .findFirst()
                .get()
                .getAssignedSupervisions();

        assertEquals(0, teacher1Assignments);
    }

    // ============= TIME CONFLICT TESTS =============

    @Test
    void testAssignment_NoTimeConflicts_OneExamPerSlot() throws ExecutionException, InterruptedException {
        // Setup: 2 exams in same slot, teacher should only be assigned to one
        Map<Long, Boolean> participationMap = Map.of(1L, true, 2L, true, 3L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 5, 2L, 5, 3L, 5);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", 2L, "PR", 3L, "MA"));
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "Teacher 1", 2L, "Teacher 2", 3L, "Teacher 3"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "t1@test.com", 2L, "t2@test.com", 3L, "t3@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        // Two exams at same time
        List<ExamForAssignmentProjection> exams = Arrays.asList(
                createExamProjection("E1", 1, SeanceType.S1, "R1", null, 1),
                createExamProjection("E2", 1, SeanceType.S1, "R2", null, 1)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(Collections.emptyList());

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());

        // Check each teacher assigned to max one exam in that slot
        for (var workload : result.getTeacherWorkloads()) {
            long sameSlotAssignments = workload.getAssignments().stream()
                    .filter(a -> a.getDay() == 1 && a.getSeanceLabel().equals("S1"))
                    .count();
            assertTrue(sameSlotAssignments <= 1,
                    "Teacher should not have multiple exams in same slot");
        }
    }

    // ============= CUSTOM CONFIGURATION TESTS =============
    @Test
    void testAssignment_WithCustomConfig() throws ExecutionException, InterruptedException {
        AssignmentConstraintConfig customConfig = new AssignmentConstraintConfig();
        customConfig.setOwnerPresenceMode(AssignmentConstraintConfig.ConstraintMode.SOFT);
        customConfig.setNoGapsMode(AssignmentConstraintConfig.ConstraintMode.DISABLED);
        customConfig.setUnavailabilityViolationPenalty(1000);

        setupBasicScenario(3, 2, 2, 2);

        CompletableFuture<AssignmentResponseModel> future =
                assignmentService.executeAssignmentWithConfig(SESSION_ID, customConfig);
        AssignmentResponseModel result = future.get();

        assertNotNull(result);
        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());
    }
    // ============= WORKLOAD TESTS =============

    @Test
    void testAssignment_WorkloadCalculation() throws ExecutionException, InterruptedException {
        setupBasicScenario(2, 2, 2, 3);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());
        assertNotNull(result.getTeacherWorkloads());
        assertEquals(2, result.getTeacherWorkloads().size());

        for (var workload : result.getTeacherWorkloads()) {
            assertTrue(workload.getAssignedSupervisions() <= workload.getQuotaSupervisions());
            assertTrue(workload.getUtilizationPercentage() >= 0);
            assertTrue(workload.getUtilizationPercentage() <= 100);
        }
    }

    @Test
    void testAssignment_UnavailabilityCreditCalculation() throws ExecutionException, InterruptedException {
        Map<Long, Boolean> participationMap = Map.of(1L, true, 2L, true, 3L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 2, 2L, 2, 3L, 2);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", 2L, "PR", 3L, "MA"));
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "Teacher 1", 2L, "Teacher 2", 3L, "Teacher 3"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "t1@test.com", 2L, "t2@test.com", 3L, "t3@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        List<ExamForAssignmentProjection> exams = List.of(
                createExamProjection("E1", 1, SeanceType.S1, "R1", null, 2)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);

        // Teacher 1 unavailable
        List<TeacherUnavailabilityProjection> unavailability = List.of(
                createUnavailability(1L, 0, "S1")
        );
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(unavailability);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        // Teacher 1 should have unavailability credit
        var teacher1Workload = result.getTeacherWorkloads().stream()
                .filter(w -> w.getTeacherId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertTrue(teacher1Workload.getUnavailabilityCredit() > 0);
    }

    // ============= HELPER METHODS =============

    private void setupBasicScenario(int numTeachers, int numExams,
                                    int supervisorsPerExam, int quotaPerTeacher) {
        Map<Long, Boolean> participationMap = new LinkedHashMap<>();
        Map<Long, String> gradeMap = new LinkedHashMap<>();
        Map<Long, String> nameMap = new LinkedHashMap<>();
        Map<Long, String> emailMap = new LinkedHashMap<>();
        Map<Long, Integer> quotaMap = new LinkedHashMap<>();

        for (long i = 1; i <= numTeachers; i++) {
            participationMap.put(i, true);
            gradeMap.put(i, i % 2 == 0 ? "PR" : "MA");
            nameMap.put(i, "Teacher " + i);
            emailMap.put(i, "teacher" + i + "@test.com");
            quotaMap.put(i, quotaPerTeacher);
        }

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(gradeMap);
        when(teacherService.getAllNames()).thenReturn(nameMap);
        when(teacherService.getAllEmails()).thenReturn(emailMap);
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        List<ExamForAssignmentProjection> exams = new ArrayList<>();
        for (int i = 1; i <= numExams; i++) {
            exams.add(createExamProjection(
                    "E" + i,
                    1,
                    i % 2 == 0 ? SeanceType.S3 : SeanceType.S1,
                    "R" + i,
                    null,
                    supervisorsPerExam
            ));
        }
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(Collections.emptyList());
    }

    private ExamSessionDto createExamSession(int numDays, String name) {
        ExamSessionDto session = new ExamSessionDto();
        session.setId(SESSION_ID);
        session.setSessionLibelle(name);
        session.setNumExamDays(numDays);
        return session;
    }

    private ExamForAssignmentProjection createExamProjection(
            String id, int day, SeanceType seance, String room,
            Long ownerId, int requiredSupervisors) {

        return new ExamForAssignmentProjection() {
            @Override
            public String getId() { return id; }

            @Override
            public Integer getJourNumero() { return day; }

            @Override
            public SeanceType getSeance() { return seance; }

            @Override
            public String getNumRooms() { return room; }

            @Override
            public Long getResponsableId() { return ownerId; }

            @Override
            public Integer getRequiredSupervisors() { return requiredSupervisors; }

            @Override
            public LocalDate getExamDate() {
                return LocalDate.of(2025, 1, day);
            }

            @Override
            public LocalTime getStartTime() {
                return seance == SeanceType.S1 ?
                        LocalTime.of(8, 0) : LocalTime.of(14, 0);
            }

            @Override
            public LocalTime getEndTime() {
                return seance == SeanceType.S1 ?
                        LocalTime.of(12, 0) : LocalTime.of(18, 0);
            }
        };
    }

    private TeacherUnavailabilityProjection createUnavailability(
            Long teacherId, int day, String seance) {

        return new TeacherUnavailabilityProjection() {
            @Override
            public Long getId() { return teacherId; }

            @Override
            public Integer getNumeroJour() { return day; }

            @Override
            public String getSeance() { return seance; }
        };
    }

    private Map<GradeType, Integer> getDefaultPriorities() {
        Map<GradeType, Integer> priorities = new HashMap<>();
        priorities.put(GradeType.PR, 1);
        priorities.put(GradeType.MA, 2);
        priorities.put(GradeType.MC, 3);
        priorities.put(GradeType.PTC, 4);
        priorities.put(GradeType.AS, 5);
        return priorities;
    }

    // ============= DEDUPLICATION TESTS =============

    @Test
    void testAssignment_ExamDeduplication_MultipleOwnersHandled()
            throws ExecutionException, InterruptedException {
        // Setup: Same logical exam appears twice with different owners
        Map<Long, Boolean> participationMap = Map.of(1L, true, 2L, true, 3L, true, 4L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 2, 2L, 2, 3L, 2, 4L, 2);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", 2L, "PR",
                3L, "MA", 4L, "MA"));
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "Owner 1", 2L, "Owner 2", 3L, "Teacher 3", 4L, "Teacher 4"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "o1@test.com", 2L, "o2@test.com",
                3L, "t3@test.com", 4L, "t4@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        // Same exam (day 1, S1, room R1) but two DB rows with different owners
        List<ExamForAssignmentProjection> exams = Arrays.asList(
                createExamProjection("E1", 1, SeanceType.S1, "R1", 1L, 2),
                createExamProjection("E1", 1, SeanceType.S1, "R1", 2L, 2)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(Collections.emptyList());

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());

        // Should be only 1 logical exam after deduplication
        assertEquals(1, result.getExamAssignments().size());

        // Neither owner should be assigned
        List<Long> assignedIds = result.getExamAssignments().get(0)
                .getAssignedTeachers().stream()
                .map(t -> t.getTeacherId())
                .toList();

        assertFalse(assignedIds.contains(1L), "Owner 1 should not be assigned");
        assertFalse(assignedIds.contains(2L), "Owner 2 should not be assigned");
    }




    @Test
    void testAssignment_NoGapsSoftMode_PenalizesGaps()
            throws ExecutionException, InterruptedException {
        AssignmentConstraintConfig config = new AssignmentConstraintConfig();
        config.setNoGapsMode(AssignmentConstraintConfig.ConstraintMode.SOFT);
        config.setNoGapsPenalty(100);
        config.setOwnerPresencePenalty(5);
        config.setNoGapsSkipUnavailableTeachers(false);
        config.setOptimizeConflictAvoidance(true);
        config.setConflictAvoidancePenalty(1);
        config.setUnavailabilityViolationPenalty(1000);
        assignmentService.setConfig(config);

        setupBasicScenario(2, 3, 1, 5);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        // Should succeed (soft constraint)
        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());
    }

    // ============= METADATA TESTS =============

    @Test
    void testAssignment_MetadataPopulatedCorrectly()
            throws ExecutionException, InterruptedException {
        setupBasicScenario(3, 2, 2, 2);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertNotNull(result.getMetadata());
        assertEquals(SESSION_ID, result.getMetadata().getSessionId());
        assertEquals("Test Session", result.getMetadata().getSessionName());
        assertEquals(2, result.getMetadata().getTotalExams());
        assertEquals(3, result.getMetadata().getTotalTeachers());
        assertEquals(3, result.getMetadata().getParticipatingTeachers());
        assertTrue(result.getMetadata().getSolutionTimeSeconds() >= 0);
        assertNotNull(result.getGeneratedAt());
    }

    // ============= EDGE CASES =============

    @Test
    void testAssignment_NoExams_ShouldSucceed() throws ExecutionException, InterruptedException {
        Map<Long, Boolean> participationMap = Map.of(1L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 5);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(1L, "PR"));
        when(teacherService.getAllNames()).thenReturn(Map.of(1L, "Teacher 1"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(1L, "t1@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(Collections.emptyList());
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(Collections.emptyList());

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());
        assertEquals(0, result.getExamAssignments().size());
    }

    @Test
    void testAssignment_AllTeachersUnavailable_RequiresRelaxation()
            throws ExecutionException, InterruptedException {
        Map<Long, Boolean> participationMap = Map.of(1L, true, 2L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 2, 2L, 2);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", 2L, "PR"));
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "Teacher 1", 2L, "Teacher 2"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "t1@test.com", 2L, "t2@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        List<ExamForAssignmentProjection> exams = List.of(
                createExamProjection("E1", 1, SeanceType.S1, "R1", null, 2)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);

        // All teachers unavailable
        List<TeacherUnavailabilityProjection> unavailability = Arrays.asList(
                createUnavailability(1L, 0, "S1"),
                createUnavailability(2L, 0, "S1")
        );
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(unavailability);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());
        assertTrue(result.getMessage().contains("relaxed"));
        assertEquals(2, result.getMetadata().getRelaxedTeachersCount());
    }

    @Test
    void testAssignment_ZeroQuotaTeachers_NotAssigned()
            throws ExecutionException, InterruptedException {
        Map<Long, Boolean> participationMap = Map.of(1L, true, 2L, true, 3L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 0, 2L, 2, 3L, 2);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", 2L, "PR",
                3L, "MA"));
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "Zero Quota", 2L, "Teacher 2", 3L, "Teacher 3"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "zq@test.com", 2L, "t2@test.com", 3L, "t3@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        List<ExamForAssignmentProjection> exams = List.of(
                createExamProjection("E1", 1, SeanceType.S1, "R1", null, 2)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(Collections.emptyList());

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());

        // Teacher 1 should have 0 assignments
        var teacher1Workload = result.getTeacherWorkloads().stream()
                .filter(w -> w.getTeacherId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertEquals(0, teacher1Workload.getAssignedSupervisions());
    }

    // ============= PRIORITY TESTS =============

    @Test
    void testAssignment_TeacherPriorityRespected_InRelaxation()
            throws ExecutionException, InterruptedException {
        // High priority teachers should be relaxed last
        Map<Long, Boolean> participationMap = Map.of(1L, true, 2L, true);
        Map<Long, Integer> quotaMap = Map.of(1L, 3, 2L, 3);

        when(teacherService.getTeacherParticipeSurveillance()).thenReturn(participationMap);
        when(teacherService.getAllGrades()).thenReturn(Map.of(
                1L, "PR", // Higher priority
                2L, "MA"));   // Lower priority
        when(teacherService.getAllNames()).thenReturn(Map.of(
                1L, "High Priority", 2L, "Low Priority"));
        when(teacherService.getAllEmails()).thenReturn(Map.of(
                1L, "hp@test.com", 2L, "lp@test.com"));
        when(teacherQuotaService.getAllQuotas(SESSION_ID)).thenReturn(quotaMap);
        when(quotaPerGradeService.getPrioritiesByGrade()).thenReturn(getDefaultPriorities());

        ExamSessionDto session = createExamSession(5, "Test Session");
        when(examSessionService.getExamSessionDto(SESSION_ID)).thenReturn(session);

        List<ExamForAssignmentProjection> exams = List.of(
                createExamProjection("E1", 1, SeanceType.S1, "R1", null, 2)
        );
        when(examService.getExamsForAssignment(SESSION_ID)).thenReturn(exams);

        // Both unavailable
        List<TeacherUnavailabilityProjection> unavailability = Arrays.asList(
                createUnavailability(1L, 0, "S1"),
                createUnavailability(2L, 0, "S1")
        );
        when(teacherUnavailabilityService.getTeacherUnavailabilitiesBySessionId(SESSION_ID))
                .thenReturn(unavailability);

        CompletableFuture<AssignmentResponseModel> future = assignmentService.executeAssignment(SESSION_ID);
        AssignmentResponseModel result = future.get();

        assertEquals(AssignmentStatus.SUCCESS, result.getStatus());
        // Both should be relaxed since both needed
        assertEquals(2, result.getMetadata().getRelaxedTeachersCount());
    }
}