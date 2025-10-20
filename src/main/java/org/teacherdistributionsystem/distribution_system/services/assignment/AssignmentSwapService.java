package org.teacherdistributionsystem.distribution_system.services.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.teacherdistributionsystem.distribution_system.entities.assignment.Exam;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;



import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.SwapResult;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.ExamRepository;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.TeacherExamAssignmentRepository;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherRepository;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherUnavailabilityRepository;

import java.util.ArrayList;
import java.util.List;

import static org.teacherdistributionsystem.distribution_system.models.responses.assignment.SwapResult.success;

@Service
@RequiredArgsConstructor
@Log4j2
public class AssignmentSwapService {

    private final TeacherExamAssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final TeacherUnavailabilityRepository unavailabilityRepository;
    private final JavaMailSender mailSender;
    private final TeacherRepository teacherRepository;



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
                exam2,exam1,
                "Teacher 1 → Exam 2"
        );
        if (teacher1ToExam2 != null) {
            violations.add(teacher1ToExam2);
        }

        String teacher2ToExam1 = validateSwap(
                assignment2.getTeacherId(),
                exam1,exam2,
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


        return success(assignment1, assignment2);
    }


    private String validateSwap(Long teacherId, Exam exam,Exam ownExam, String context) {
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
                ownExam.getExamSession().getId(),
                ownExam.getJourNumero(),
                ownExam.getSeance().name()
        );

        if (hasConflict) {
            return context + ": Teacher already has an assignment at this time slot";
        }

        return null; // No violations
    }

    /**
     * Envoie les notifications de swap aux deux enseignants concernés
     */
    private void sendSwapNotifications(Long teacher1Id, Long teacher2Id,
                                       Exam exam1, Exam exam2,
                                       TeacherExamAssignment assignment1,
                                       TeacherExamAssignment assignment2) {
        try {
            // Récupérer les enseignants
            Teacher teacher1 = teacherRepository.findById(teacher1Id)
                    .orElseThrow(() -> new IllegalArgumentException("Teacher 1 not found"));
            Teacher teacher2 = teacherRepository.findById(teacher2Id)
                    .orElseThrow(() -> new IllegalArgumentException("Teacher 2 not found"));

            // Récupérer les nouveaux examens après swap
            Exam newExamForTeacher1 = exam2; // Teacher1 prend l'exam de Teacher2
            Exam newExamForTeacher2 = exam1; // Teacher2 prend l'exam de Teacher1

            // Envoyer les emails
            sendSwapEmail(teacher1, exam1, newExamForTeacher1, assignment2);
            sendSwapEmail(teacher2, exam2, newExamForTeacher2, assignment1);

            log.info("📧 Notifications envoyées à {} et {}",
                    teacher1.getEmail(), teacher2.getEmail());

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send swap notifications", e);
        }
    }

    /**
     * Envoie un email de notification de swap à un enseignant
     */
    private void sendSwapEmail(Teacher teacher, Exam oldExam, Exam newExam,
                               TeacherExamAssignment newAssignment) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(teacher.getEmail());
            helper.setSubject("🔄 Modification de votre affectation de surveillance d'examen");
            //helper.setText(buildSwapEmailContent(teacher, oldExam, newExam, newAssignment), true);

            mailSender.send(message);
            log.info("📧 Email envoyé à: {}", teacher.getEmail());

        } catch (Exception e) {
            log.error("❌ Échec de l'envoi de l'email à {}: {}", teacher.getEmail(), e.getMessage());
            throw e;
        }
    }






}