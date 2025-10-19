package org.teacherdistributionsystem.distribution_system.services.assignment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.teacherdistributionsystem.distribution_system.entities.assignment.Exam;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;

import org.teacherdistributionsystem.distribution_system.models.responses.assignment.SwapResult;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.ExamRepository;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.TeacherExamAssignmentRepository;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherUnavailabilityRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssignmentSwapService {

    private final TeacherExamAssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final TeacherUnavailabilityRepository unavailabilityRepository;

    public AssignmentSwapService(TeacherExamAssignmentRepository assignmentRepository,
                                 ExamRepository examRepository,
                                 TeacherUnavailabilityRepository unavailabilityRepository) {
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.unavailabilityRepository = unavailabilityRepository;
    }

    /**
     * Swap assignments between two teachers
     *
     * @param assignmentId1 First teacher's assignment ID
     * @param assignmentId2 Second teacher's assignment ID
     * @return SwapResult with success status and details
     */
    @Transactional
    public SwapResult swapAssignments(Long assignmentId1, Long assignmentId2) {


        TeacherExamAssignment assignment1 = assignmentRepository.findById(assignmentId1)
                .orElseThrow(() -> new IllegalArgumentException("Assignment 1 not found: " + assignmentId1));

        TeacherExamAssignment assignment2 = assignmentRepository.findById(assignmentId2)
                .orElseThrow(() -> new IllegalArgumentException("Assignment 2 not found: " + assignmentId2));



        if (!assignment1.getSessionId().equals(assignment2.getSessionId())) {
            return SwapResult.failure("Assignments must be from the same session");
        }


        if (assignment1.getTeacherId().equals(assignment2.getTeacherId())) {
            return SwapResult.failure("Cannot swap assignments for the same teacher");
        }

        Exam exam1 = examRepository.findById(assignment1.getExamId())
                .orElseThrow(() -> new IllegalArgumentException("Exam 1 not found"));

        Exam exam2 = examRepository.findById(assignment2.getExamId())
                .orElseThrow(() -> new IllegalArgumentException("Exam 2 not found"));


        List<String> violations = new ArrayList<>();


        String teacher1ToExam2 = validateSwap(
                assignment1.getTeacherId(),
                exam2,
                "Teacher 1 → Exam 2"
        );
        if (teacher1ToExam2 != null) {
            violations.add(teacher1ToExam2);
        }

        String teacher2ToExam1 = validateSwap(
                assignment2.getTeacherId(),
                exam1,
                "Teacher 2 → Exam 1"
        );
        if (teacher2ToExam1 != null) {
            violations.add(teacher2ToExam1);
        }


        if (!violations.isEmpty()) {
            System.out.println("❌ SWAP REJECTED\n");
            for (String violation : violations) {
                System.out.println("  ✗ " + violation);
            }
            System.out.println();
            return SwapResult.failure("Swap constraints violated", violations);
        }
        Long assignment1Id = assignment1.getTeacherId();
        Long assignment2Id = assignment2.getTeacherId();

        assignment1.setTeacherId(assignment2Id);

        assignment2.setTeacherId(assignment1Id);


        assignmentRepository.save(assignment1);
        assignmentRepository.save(assignment2);


        return SwapResult.success(assignment1, assignment2);
    }


    private String validateSwap(Long teacherId, Exam exam, String context) {
        // Check 1: Teacher is not the exam owner
        if (exam.getResponsable().getId().equals(teacherId)) {
            return context + ": Teacher cannot supervise their own exam (Exam " +
                    exam.getId() + ")";
        }

        // Check 2: Teacher is available at this time slot
        boolean isUnavailable = unavailabilityRepository.existsByTeacherAndSessionAndExamDayAndSeance(
                teacherId,
                exam.getExamSession().getId(),
                exam.getJourNumero(),
                exam.getSeance().name()
        );

        if (isUnavailable) {
            return context + ": Teacher is unavailable at this time (Day " +
                    exam.getJourNumero() + ", " + exam.getSeance() + ")";
        }

        // Check 3: Teacher doesn't have another exam at the same time
        boolean hasConflict = examRepository.existsExamForTeacherInSessionAndDayAndSeance(
                teacherId,
                exam.getExamSession().getId(),
                exam.getJourNumero(),
                exam.getSeance().name()
        );

        if (hasConflict) {
            return context + ": Teacher already has an assignment at this time slot";
        }

        return null; // No violations
    }






}