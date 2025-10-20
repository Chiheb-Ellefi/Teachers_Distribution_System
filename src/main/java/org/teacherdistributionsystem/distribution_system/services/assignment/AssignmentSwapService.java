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

import java.time.format.DateTimeFormatter;
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
    public void sendSwapNotifications(Long teacher1Id, Long teacher2Id,
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
            helper.setText(buildSwapEmailContent(teacher, oldExam, newExam, newAssignment), true);

            mailSender.send(message);
            log.info("📧 Email envoyé à: {}", teacher.getEmail());

        } catch (Exception e) {
            log.error("❌ Échec de l'envoi de l'email à {}: {}", teacher.getEmail(), e.getMessage());
            throw e;
        }
    }

    private String buildSwapEmailContent(Teacher teacher, Exam oldExam, Exam newExam,
                                         TeacherExamAssignment newAssignment) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String teacherName = teacher.getPrenom() + " " + teacher.getNom();

        // Informations ancien examen (depuis l'entité Exam)
        String oldExamDate = oldExam.getExamDate() != null ?
                oldExam.getExamDate().format(dateFormatter) : "N/A";
        String oldSeance = oldExam.getSeance() != null ? oldExam.getSeance().name() : "N/A";
        String oldStartTime = oldExam.getStartTime() != null ?
                oldExam.getStartTime().format(timeFormatter) : "N/A";
        String oldEndTime = oldExam.getEndTime() != null ?
                oldExam.getEndTime().format(timeFormatter) : "N/A";
        String oldRoom = oldExam.getNumRooms() != null ? oldExam.getNumRooms() : "N/A";

        // Informations nouvel examen (depuis l'entité Exam)
        String newExamDate = newExam.getExamDate() != null ?
                newExam.getExamDate().format(dateFormatter) : "N/A";
        String newSeance = newExam.getSeance() != null ? newExam.getSeance().name() : "N/A";
        String newStartTime = newExam.getStartTime() != null ?
                newExam.getStartTime().format(timeFormatter) : "N/A";
        String newEndTime = newExam.getEndTime() != null ?
                newExam.getEndTime().format(timeFormatter) : "N/A";
        String newRoom = newExam.getNumRooms() != null ? newExam.getNumRooms() : "N/A";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                        line-height: 1.6; 
                        color: #333; 
                        background-color: #f4f4f4; 
                        margin: 0;
                        padding: 20px;
                    }
                    .container { 
                        max-width: 600px; 
                        margin: 0 auto; 
                        background-color: white;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .header { 
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white; 
                        padding: 30px 20px; 
                        text-align: center; 
                    }
                    .header h2 { margin: 0; font-size: 24px; }
                    .content { padding: 30px; }
                    .greeting { font-size: 16px; margin-bottom: 20px; }
                    .alert-box { 
                        background-color: #fff3cd; 
                        border-left: 4px solid #ffc107;
                        padding: 15px; 
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                    .exam-section { margin: 25px 0; }
                    .exam-section h3 { 
                        margin-bottom: 15px; 
                        font-size: 18px;
                    }
                    .exam-box { 
                        background-color: #f8f9fa; 
                        padding: 20px; 
                        border-radius: 6px;
                        border-left: 4px solid #ddd;
                    }
                    .exam-box.old { 
                        border-left-color: #dc3545; 
                        background-color: #fff5f5;
                    }
                    .exam-box.new { 
                        border-left-color: #28a745; 
                        background-color: #f0fff4;
                    }
                    .exam-row { 
                        display: flex; 
                        padding: 8px 0; 
                        border-bottom: 1px solid #e9ecef;
                    }
                    .exam-row:last-child { border-bottom: none; }
                    .exam-label { 
                        font-weight: 600; 
                        color: #495057; 
                        min-width: 120px;
                    }
                    .exam-value { color: #212529; }
                    .footer { 
                        background-color: #f8f9fa; 
                        padding: 20px; 
                        text-align: center; 
                        font-size: 12px; 
                        color: #6c757d; 
                        border-top: 1px solid #dee2e6;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🔄 Échange d'Affectation</h2>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">
                            <strong>Bonjour %s,</strong>
                        </div>
                        
                        <p>Votre affectation de surveillance d'examen a été modifiée suite à un échange entre enseignants.</p>
                        
                        <div class="alert-box">
                            <strong>⚠️ Action requise :</strong> Veuillez mettre à jour votre emploi du temps personnel.
                        </div>

                        <div class="exam-section">
                            <h3 style="color: #dc3545;">❌ Ancienne Affectation</h3>
                            <div class="exam-box old">
                                <div class="exam-row">
                                    <div class="exam-label">Date :</div>
                                    <div class="exam-value">%s (Jour %d)</div>
                                </div>
                                <div class="exam-row">
                                    <div class="exam-label">Séance :</div>
                                    <div class="exam-value">%s</div>
                                </div>
                                <div class="exam-row">
                                    <div class="exam-label">Horaire :</div>
                                    <div class="exam-value">%s - %s</div>
                                </div>
                                <div class="exam-row">
                                    <div class="exam-label">Salle :</div>
                                    <div class="exam-value">%s</div>
                                </div>
                            </div>
                        </div>

                        <div class="exam-section">
                            <h3 style="color: #28a745;">✅ Nouvelle Affectation</h3>
                            <div class="exam-box new">
                                <div class="exam-row">
                                    <div class="exam-label">Date :</div>
                                    <div class="exam-value">%s (Jour %d)</div>
                                </div>
                                <div class="exam-row">
                                    <div class="exam-label">Séance :</div>
                                    <div class="exam-value">%s</div>
                                </div>
                                <div class="exam-row">
                                    <div class="exam-label">Horaire :</div>
                                    <div class="exam-value">%s - %s</div>
                                </div>
                                <div class="exam-row">
                                    <div class="exam-label">Salle :</div>
                                    <div class="exam-value">%s</div>
                                </div>
                            </div>
                        </div>
                        
                        <p style="margin-top: 25px; color: #6c757d;">
                            Pour toute question, contactez l'administration académique.
                        </p>
                    </div>
                    
                    <div class="footer">
                        <p><strong>Système de Distribution des Enseignants</strong></p>
                        <p>Email automatique - Ne pas répondre</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                teacherName,
                // Ancien examen
                oldExamDate, oldExam.getJourNumero(),
                oldSeance,
                oldStartTime, oldEndTime,
                oldRoom,
                // Nouvel examen
                newExamDate, newExam.getJourNumero(),
                newSeance,
                newStartTime, newEndTime,
                newRoom
        );
    }





}