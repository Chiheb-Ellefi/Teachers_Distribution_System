package org.teacherdistributionsystem.distribution_system.services.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import org.teacherdistributionsystem.distribution_system.dtos.assignment.EmailRequestDTO;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherAssignmentsDTO;

import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.*;
@RequiredArgsConstructor
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);


    private final JavaMailSender mailSender;


    private final  PDFScheduleService pdfScheduleService;

    private final  JsonDataLoaderService jsonDataLoaderService;

    /**
     * Traite l'envoi de mails selon le type demandé
     */
    public Map<String, Object> envoyerMailsAvecPlanning(EmailRequestDTO request) {
        try {
            switch (request.getType()) {
                case "PERSONNALISE":
                    return traiterPersonnalise(request);

                case "RESPONSABLES":
                    return traiterResponsables(request);

                case "SURVEILLANTS":
                    return traiterSurveillants(request);

                default:
                    return Map.of(
                            "success", false,
                            "message", "Type de destinataire invalide"
                    );
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi des mails", e);
            return Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            );
        }
    }

    /**
     * Type 1: PERSONNALISÉ - Adresses séparées par des virgules
     * Chaque personne reçoit le planning complet (tous les jours, toutes les séances)
     */
    private Map<String, Object> traiterPersonnalise(EmailRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        int mailsEnvoyes = 0;
        List<String> erreurs = new ArrayList<>();

        try {
            // Extraire les adresses (format: "email1@gmail.com, email2@gmail.com")
            List<String> emails = Arrays.stream(request.getDestinataires().split(","))
                    .map(String::trim)
                    .filter(email -> email.contains("@"))
                    .toList();

            if (emails.isEmpty()) {
                response.put("success", false);
                response.put("message", "Aucune adresse email valide trouvée");
                return response;
            }

            logger.info("Envoi mail personnalisé à {} destinataires", emails.size());

            // Générer le PDF du planning complet (toutes les séances de tous les jours)
            ByteArrayOutputStream pdfData = pdfScheduleService.generateAllSessionsSchedulePDF();

            // Envoyer à chaque destinataire
            for (String email : emails) {
                try {
                    envoyerMailAvecPDF(
                            email,
                            request.getObjet(),
                            request.getCorps(),
                            pdfData.toByteArray(),
                            "planning_complet_surveillances.pdf"
                    );
                    mailsEnvoyes++;
                } catch (Exception e) {
                    logger.error("Erreur envoi à {}: {}", email, e.getMessage());
                    erreurs.add(email + " - " + e.getMessage());
                }
            }

            response.put("success", true);
            response.put("message", mailsEnvoyes + "/" + emails.size() + " mails envoyés");
            response.put("mailsEnvoyes", mailsEnvoyes);
            response.put("totalDestinataires", emails.size());
            if (!erreurs.isEmpty()) {
                response.put("erreurs", erreurs);
            }

        } catch (Exception e) {
            logger.error("Erreur traitement personnalisé", e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
        }

        return response;
    }

    /**
     * Type 2: RESPONSABLES - Chaque responsable reçoit SON planning de responsabilité
     */
    private Map<String, Object> traiterResponsables(EmailRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        int mailsEnvoyes = 0;
        List<String> erreurs = new ArrayList<>();

        try {
            // Récupérer tous les enseignants responsables
            List<String> responsables = jsonDataLoaderService.getAllResponsibleTeacherNames();

            if (responsables.isEmpty()) {
                response.put("success", false);
                response.put("message", "Aucun responsable trouvé");
                return response;
            }

            logger.info("Envoi mail aux {} responsables", responsables.size());

            // Envoyer à chaque responsable SON planning
            for (String responsableName : responsables) {
                try {
                    // Récupérer les données complètes du responsable
                    TeacherAssignmentsDTO responsableData =
                            jsonDataLoaderService.getResponsibleTeacherDataByName(responsableName);

                    // Récupérer l'email du responsable (à partir de teacherId ou autre source)
                    String email = getEmailFromTeacherData(responsableData);

                    if (email == null || email.isEmpty()) {
                        logger.warn("Email non trouvé pour responsable: {}", responsableName);
                        erreurs.add(responsableName + " - Email non trouvé");
                        continue;
                    }

                    // Générer le PDF du planning pour ce responsable
                    ByteArrayOutputStream pdfData = pdfScheduleService
                            .generateResponsibleTeacherSchedulePDF(responsableName);

                    // Corps personnalisé
                    String corpsPersonnalise = request.getCorps()
                            .replace("{nom}", responsableName);

                    envoyerMailAvecPDF(
                            email,
                            request.getObjet(),
                            corpsPersonnalise,
                            pdfData.toByteArray(),
                            "planning_responsable_" + sanitizeFilename(responsableName) + ".pdf"
                    );
                    mailsEnvoyes++;

                } catch (Exception e) {
                    logger.error("Erreur envoi à responsable {}: {}", responsableName, e.getMessage());
                    erreurs.add(responsableName + " - " + e.getMessage());
                }
            }

            response.put("success", true);
            response.put("message", mailsEnvoyes + "/" + responsables.size() + " mails envoyés aux responsables");
            response.put("mailsEnvoyes", mailsEnvoyes);
            response.put("totalDestinataires", responsables.size());
            if (!erreurs.isEmpty()) {
                response.put("erreurs", erreurs);
            }

        } catch (Exception e) {
            logger.error("Erreur traitement responsables", e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
        }

        return response;
    }

    /**
     * Type 3: SURVEILLANTS - Chaque surveillant reçoit SON planning de surveillance
     */
    private Map<String, Object> traiterSurveillants(EmailRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        int mailsEnvoyes = 0;
        List<String> erreurs = new ArrayList<>();

        try {
            // Récupérer tous les enseignants (surveillants)
            List<String> surveillants = jsonDataLoaderService.getAllTeacherNames();

            if (surveillants.isEmpty()) {
                response.put("success", false);
                response.put("message", "Aucun surveillant trouvé");
                return response;
            }

            logger.info("Envoi mail aux {} surveillants", surveillants.size());

            // Envoyer à chaque surveillant SON planning
            for (String surveillantName : surveillants) {
                try {
                    // Récupérer les données complètes du surveillant
                    TeacherAssignmentsDTO surveillantData =
                            jsonDataLoaderService.getTeacherDataByName(surveillantName);

                    // Récupérer l'email du surveillant
                    String email = getEmailFromTeacherData(surveillantData);

                    if (email == null || email.isEmpty()) {
                        logger.warn("Email non trouvé pour surveillant: {}", surveillantName);
                        erreurs.add(surveillantName + " - Email non trouvé");
                        continue;
                    }

                    // Générer le PDF du planning pour ce surveillant
                    ByteArrayOutputStream pdfData = pdfScheduleService
                            .generateTeacherSchedulePDF(surveillantName);

                    // Corps personnalisé
                    String corpsPersonnalise = request.getCorps()
                            .replace("{nom}", surveillantName);

                    envoyerMailAvecPDF(
                            email,
                            request.getObjet(),
                            corpsPersonnalise,
                            pdfData.toByteArray(),
                            "planning_surveillance_" + sanitizeFilename(surveillantName) + ".pdf"
                    );
                    mailsEnvoyes++;

                } catch (Exception e) {
                    logger.error("Erreur envoi à surveillant {}: {}", surveillantName, e.getMessage());
                    erreurs.add(surveillantName + " - " + e.getMessage());
                }
            }

            response.put("success", true);
            response.put("message", mailsEnvoyes + "/" + surveillants.size() + " mails envoyés aux surveillants");
            response.put("mailsEnvoyes", mailsEnvoyes);
            response.put("totalDestinataires", surveillants.size());
            if (!erreurs.isEmpty()) {
                response.put("erreurs", erreurs);
            }

        } catch (Exception e) {
            logger.error("Erreur traitement surveillants", e);
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());
        }

        return response;
    }

    /**
     * Envoie un mail avec un PDF en pièce jointe
     */
    private void envoyerMailAvecPDF(String destinataire, String sujet, String corps,
                                    byte[] pdfData, String nomFichier) throws Exception {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(destinataire);
        helper.setSubject(sujet);
        helper.setText(corps, true); // true = HTML

        // Ajouter la pièce jointe PDF
        ByteArrayResource resource = new ByteArrayResource(pdfData);
        helper.addAttachment(nomFichier, resource, "application/pdf");

        mailSender.send(message);
        logger.info("Mail envoyé avec succès à: {}", destinataire);
    }

    /**
     * Récupère l'email depuis TeacherAssignmentsDTO
     * TODO: Adapter selon votre structure de données réelle
     */
    private String getEmailFromTeacherData(TeacherAssignmentsDTO teacher) {
        // Option 1: Si l'email est dans le DTO
        if (teacher.getEmail() != null && !teacher.getEmail().isEmpty()) {
            return teacher.getEmail();
        }

        // Option 2: Construire l'email à partir du nom (temporaire)
        // Format: prenom.nom@institution.tn
        String name = teacher.getTeacherName().toLowerCase()
                .replace(" ", ".")
                .replaceAll("[^a-z.]", "");
        return name + "@isi.utm.tn"; // À adapter selon votre domaine

        // Option 3: Chercher dans une autre source de données
        // return emailRepository.findByTeacherId(teacher.getTeacherId());
    }

    /**
     * Nettoie un nom de fichier pour éviter les caractères invalides
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}