package org.teacherdistributionsystem.distribution_system.services.assignment;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherExamAssignmentDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.AssignmentSession;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;
import org.teacherdistributionsystem.distribution_system.mappers.assignment.TeacherExamAssignmentMapper;
import org.teacherdistributionsystem.distribution_system.models.others.AssignedTeacherModel;
import org.teacherdistributionsystem.distribution_system.models.others.AssignmentMetadata;
import org.teacherdistributionsystem.distribution_system.models.others.ExamAssignmentModel;
import org.teacherdistributionsystem.distribution_system.models.others.TeacherWorkloadModel;
import org.teacherdistributionsystem.distribution_system.models.projections.AssignmentDetailsProjection;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.*;
import org.teacherdistributionsystem.distribution_system.models.responses.teacher.TeacherResponse;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.AssignmentSessionRepository;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.TeacherExamAssignmentRepository;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherRepository;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentPersistenceService {

    private final TeacherExamAssignmentRepository assignmentRepository;
    private final AssignmentSessionRepository sessionRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherService teacherService;


    @Transactional
    public void saveAssignmentResults(AssignmentResponseModel response) {
        if (response.getStatus() != AssignmentStatus.SUCCESS) {
            saveSessionMetadata(response);
            return;
        }

        Long sessionId = response.getMetadata().getSessionId();

        assignmentRepository.deactivateAllForSession(sessionId);

        List<TeacherExamAssignment> assignments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (ExamAssignmentModel examModel : response.getExamAssignments()) {
            for (AssignedTeacherModel teacherModel : examModel.getAssignedTeachers()) {
                TeacherExamAssignment assignment = TeacherExamAssignment.builder()
                        .sessionId(sessionId)
                        .examId(examModel.getExamId())
                        .teacherId(teacherModel.getTeacherId())
                        .examDay(examModel.getDay())
                        .seance(examModel.getSeance())
                        .assignedAt(now)
                        .startTime(examModel.getStartTime())
                        .endTime(examModel.getEndTime())
                        .examDate(examModel.getExamDate())
                        .isActive(true)
                        .build();


                assignments.add(assignment);

            }
        }
        for(TeacherWorkloadModel workloadModel : response.getTeacherWorkloads()){
            updateTeacherCredit(workloadModel.getTeacherId(),workloadModel.getUnavailabilityCredit());
        }

        assignmentRepository.saveAll(assignments);

        saveSessionMetadata(response);
    }

    private void saveSessionMetadata(AssignmentResponseModel response) {
        AssignmentMetadata metadata = response.getMetadata();

        AssignmentSession session = AssignmentSession.builder()
                .examSessionId(metadata.getSessionId())
                .status(response.getStatus())
                .isOptimal(metadata.getIsOptimal())
                .totalAssignments(metadata.getTotalAssignmentsMade())
                .solutionTimeSeconds(metadata.getSolutionTimeSeconds())
                .createdAt(LocalDateTime.now())
                .build();

        sessionRepository.save(session);
    }
    @Async
    @Transactional
    public CompletableFuture<Void> saveAssignmentResultsAsync(AssignmentResponseModel response) {
        saveAssignmentResults(response);
        return CompletableFuture.completedFuture(null);
    }

    @Transactional(readOnly = true)
    public List<TeacherExamAssignmentDto> getAssignmentsForSession(Long sessionId) {
        return assignmentRepository.findBySessionIdAndIsActiveTrue(sessionId).stream().map(TeacherExamAssignmentMapper::toDto).collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<TeacherExamAssignmentDto> getTeacherAssignments(Long teacherId, Long sessionId,boolean light) {
        return light?assignmentRepository.findByTeacherIdAndSessionIdAndIsActiveTrue(teacherId, sessionId).stream().map(TeacherExamAssignmentMapper::toLightDto).collect(Collectors.toList()):
                assignmentRepository.findByTeacherIdAndSessionIdAndIsActiveTrue(teacherId, sessionId).stream().map(TeacherExamAssignmentMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherExamAssignmentDto> getExamAssignments(String examId, Long sessionId) {
        return assignmentRepository.findByExamIdAndSessionIdAndIsActiveTrue(examId, sessionId).stream().map(TeacherExamAssignmentMapper::toDto).collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public boolean hasAssignments(Long sessionId) {
        return assignmentRepository.existsBySessionIdAndIsActiveTrue(sessionId);
    }


    @Transactional(readOnly = true)
    public AssignmentSession getSessionMetadata(Long examSessionId) {
        return sessionRepository.findByExamSessionId(examSessionId);
    }


    @Transactional
    public void deleteAssignments(Long sessionId) {
        assignmentRepository.deactivateAllForSession(sessionId);

        AssignmentSession session = sessionRepository.findByExamSessionId(sessionId);
        if (session != null) {
            sessionRepository.delete(session);
        }
    }

    private void updateTeacherCredit(Long teacherId, Integer credit){
        teacherRepository.updateQuotaCreditById(teacherId,credit);
    }
    @Transactional(readOnly = true)
    public Map<Integer, Map<Integer, Map<String, DaySeanceGroupAssignments>>> getAssignmentsBySession(Long sessionId) {
        List<AssignmentDetailsProjection> assignments = assignmentRepository.getAllBySession(sessionId);

        if (assignments.isEmpty()) {
            return new HashMap<>();
        }

        // Get all teachers at once
        List<Long> teacherIds = assignments.stream()
                .map(AssignmentDetailsProjection::getTeacherId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Teacher> teacherMap = teacherRepository.findAllById(teacherIds)
                .stream()
                .collect(Collectors.toMap(Teacher::getId, teacher -> teacher));

        // Group by day, seance, and then by room (numRooms)
        Map<Integer, Map<Integer, Map<String, List<AssignmentDetailsProjection>>>> groupedByDaySeanceRoom =
                assignments.stream()
                        .collect(Collectors.groupingBy(
                                AssignmentDetailsProjection::getExamDay,
                                Collectors.groupingBy(
                                        AssignmentDetailsProjection::getSeance,
                                        Collectors.groupingBy(
                                                AssignmentDetailsProjection::getNumRooms
                                        )
                                )
                        ));

        // Convert to final structure
        Map<Integer, Map<Integer, Map<String, DaySeanceGroupAssignments>>> result = new TreeMap<>();

        groupedByDaySeanceRoom.forEach((day, seanceMap) -> {
            Map<Integer, Map<String, DaySeanceGroupAssignments>> seanceAssignments = new TreeMap<>();

            seanceMap.forEach((seance, roomMap) -> {
                Map<String, DaySeanceGroupAssignments> roomAssignments = new TreeMap<>();

                roomMap.forEach((room, roomAssignmentsList) -> {
                    AssignmentDetailsProjection first = roomAssignmentsList.get(0);

                    List<TeacherResponse> supervisors = roomAssignmentsList.stream()
                            .map(assignment -> {
                                Teacher teacher = teacherMap.get(assignment.getTeacherId());
                                return teacherToResponse(teacher);
                            })
                            .collect(Collectors.toList());

                    DaySeanceGroupAssignments daySeanceGroup = DaySeanceGroupAssignments.builder()
                            .examDay(first.getExamDay())
                            .seance(first.getSeance())
                            .examDate(first.getExamDate())
                            .dayOfWeek(first.getExamDate().getDayOfWeek().toString())
                            .startTime(first.getStartTime())
                            .endTime(first.getEndTime())
                            .numRooms(first.getNumRooms())
                            .supervisors(supervisors)
                            .build();

                    roomAssignments.put(room, daySeanceGroup);
                });

                seanceAssignments.put(seance, roomAssignments);
            });

            result.put(day, seanceAssignments);
        });

        return result;
    }

    private TeacherResponse teacherToResponse(Teacher teacher) {
        if (teacher == null) {
            return null;
        }

        TeacherResponse response = new TeacherResponse();
        response.setId(teacher.getId());
        response.setNom(teacher.getNom());
        response.setPrenom(teacher.getPrenom());
        response.setEmail(teacher.getEmail());
        response.setCodeSmartex(teacher.getCodeSmartex());
        response.setGrade(teacher.getGradeCode());
        response.setParticipeSurveillance(teacher.getParticipeSurveillance());
        response.setQuota(teacher.getQuotaCredit());
        response.setCredit(teacher.getQuotaCredit());

        return response;
    }
}
