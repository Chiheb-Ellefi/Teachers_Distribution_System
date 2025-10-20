package org.teacherdistributionsystem.distribution_system.controllers.assignment;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.entities.assignment.Exam;
import org.teacherdistributionsystem.distribution_system.entities.assignment.TeacherExamAssignment;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.models.requests.SwapRequest;
import org.teacherdistributionsystem.distribution_system.models.requests.TeacherSwapRequest;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.SwapResponse;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.SwapResult;
import org.teacherdistributionsystem.distribution_system.models.responses.assignment.ValidationResponse;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.ExamRepository;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentPersistenceService;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentSwapService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherService;



import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AssignmentSwapController {

    private final AssignmentSwapService swapService;
    private final AssignmentPersistenceService assignmentPersistenceService;
    private final TeacherService teacherService;
    private final ExamRepository examRepository;
    private final JavaMailSender mailSender;

    @PostMapping("/swap")
    public ResponseEntity<SwapResponse> swapAssignments(@RequestBody SwapRequest request) {
        try {
            TeacherSwapRequest t1 = request.getTeacher1();
            TeacherSwapRequest t2 = request.getTeacher2();

            Long assignmentId1 = assignmentPersistenceService.getAssignmentIdByTeacherDayAndSeance(
                    t1.getTeacherId(), t1.getDay(), t1.getSeance()
            );
            Long assignmentId2 = assignmentPersistenceService.getAssignmentIdByTeacherDayAndSeance(
                    t2.getTeacherId(), t2.getDay(), t2.getSeance()
            );

            if (assignmentId1.equals(assignmentId2)) {
                return ResponseEntity.badRequest()
                        .body(SwapResponse.error("Cannot swap an assignment with itself"));
            }



            SwapResult result = swapService.swapAssignments(
                    assignmentId1,
                   assignmentId2
            );

            if (result.isSuccess()) {


                return ResponseEntity.ok(SwapResponse.success(result));
            } else {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(SwapResponse.failure(result));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SwapResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SwapResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }


    @GetMapping("/swap/validate")
    public ResponseEntity<ValidationResponse> validateSwap(
            @RequestParam Long assignmentId1,
            @RequestParam Long assignmentId2) {

        try {

            return ResponseEntity.ok(ValidationResponse.builder()
                    .message("Use POST /api/assignments/swap to attempt the swap")
                    .note("Validation is performed automatically during swap")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ValidationResponse.builder()
                            .message("Error: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/swap/test-email")
    public ResponseEntity<String> testSwapEmail(@RequestParam Long assignmentId1,
                                                @RequestParam Long assignmentId2) {
        try {
            // Récupérer les assignments
            TeacherExamAssignment assignment1 = assignmentPersistenceService.getAssignmentById(assignmentId1);
            TeacherExamAssignment assignment2 = assignmentPersistenceService.getAssignmentById(assignmentId2);

            // Récupérer les examens
            Exam exam1 = examRepository.findById(assignment1.getExamId())
                    .orElseThrow(() -> new IllegalArgumentException("Exam 1 not found"));
            Exam exam2 = examRepository.findById(assignment2.getExamId())
                    .orElseThrow(() -> new IllegalArgumentException("Exam 2 not found"));

            // Créer les enseignants de test avec les vrais noms et emails
            Teacher teacher1 = new Teacher();
            teacher1.setEmail("maher.ayachi@etudiant-isi.utm.tn");
            teacher1.setPrenom("Maher");
            teacher1.setNom("Ayachi");

            Teacher teacher2 = new Teacher();
            teacher2.setEmail("maherayachi0@gmail.com");
            teacher2.setPrenom("Mourad");
            teacher2.setNom("Mejri");

            // Envoyer les DEUX emails de test

            // Email 1: Maher Ayachi (qui avait exam1) reçoit maintenant exam2
            sendTestSwapEmail(teacher1, exam1, exam2, assignment2, "Maher Ayachi");

            // Email 2: Mourad Mejri (qui avait exam2) reçoit maintenant exam1
            sendTestSwapEmail(teacher2, exam2, exam1, assignment1, "Mourad Mejri");

            return ResponseEntity.ok("Deux emails de test envoyés avec succès à Maher Ayachi et Mourad Mejri");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'envoi des emails: " + e.getMessage());
        }
    }

    private void sendTestSwapEmail(Teacher teacher, Exam oldExam, Exam newExam,
                                   TeacherExamAssignment newAssignment, String role) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(teacher.getEmail());
        helper.setSubject("🔄 TEST - Échange d'affectation - " + role);

        String emailContent = buildTestSwapEmailContent(teacher, oldExam, newExam, newAssignment, role);
        helper.setText(emailContent, true);

        mailSender.send(message);
        System.out.println("📧 Email de TEST envoyé à {} ({})"+ teacher.getEmail()+ role);
    }

    private String buildTestSwapEmailContent(Teacher teacher, Exam oldExam, Exam newExam,
                                             TeacherExamAssignment newAssignment, String role) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String teacherName = teacher.getPrenom() + " " + teacher.getNom();

        // Informations ancien examen
        String oldExamDate = oldExam.getExamDate() != null ?
                oldExam.getExamDate().format(dateFormatter) : "N/A";
        String oldSeance = oldExam.getSeance() != null ? oldExam.getSeance().name() : "N/A";
        String oldStartTime = oldExam.getStartTime() != null ?
                oldExam.getStartTime().format(timeFormatter) : "N/A";
        String oldEndTime = oldExam.getEndTime() != null ?
                oldExam.getEndTime().format(timeFormatter) : "N/A";
        String oldRoom = oldExam.getNumRooms() != null ? oldExam.getNumRooms() : "N/A";

        // Informations nouvel examen
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
                .test-banner {
                    background: #ffeb3b; 
                    color: #333; 
                    padding: 15px; 
                    border: 2px dashed #ff9800; 
                    text-align: center;
                    font-weight: bold;
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
                <div class="test-banner">
                    🧪 EMAIL DE TEST - Rôle: %s<br>
                    Cet email simule une notification d'échange d'affectation
                </div>
                
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
                                <div class="exam-value">%s (Jour %%d)</div>
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
                                <div class="exam-value">%s (Jour %%d)</div>
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
                role, // %s pour le banner de test
                teacherName, // %s pour le greeting
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