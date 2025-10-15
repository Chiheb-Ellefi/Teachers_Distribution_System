package org.teacherdistributionsystem.distribution_system.services.assignment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.teacherdistributionsystem.distribution_system.entities.assignment.AssignmentSession;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;
import org.teacherdistributionsystem.distribution_system.enums.AssignmentStatus;
import org.teacherdistributionsystem.distribution_system.models.responses.*;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.AssignmentSessionRepository;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.TeacherExamAssignmentRepository;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentPersistenceService {

    private final TeacherExamAssignmentRepository assignmentRepository;
    private final AssignmentSessionRepository sessionRepository;
    private final TeacherRepository teacherRepository;


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
        AssignmentSession existing = sessionRepository.findByExamSessionId(metadata.getSessionId());
        if (existing != null) {
            sessionRepository.delete(existing);
        }

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


    @Transactional(readOnly = true)
    public List<TeacherExamAssignment> getAssignmentsForSession(Long sessionId) {
        return assignmentRepository.findBySessionIdAndIsActiveTrue(sessionId);
    }


    @Transactional(readOnly = true)
    public List<TeacherExamAssignment> getTeacherAssignments(Long teacherId, Long sessionId) {
        return assignmentRepository.findByTeacherIdAndSessionIdAndIsActiveTrue(teacherId, sessionId);
    }

    @Transactional(readOnly = true)
    public List<TeacherExamAssignment> getExamAssignments(String examId, Long sessionId) {
        return assignmentRepository.findByExamIdAndSessionIdAndIsActiveTrue(examId, sessionId);
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
}
