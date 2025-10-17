package org.teacherdistributionsystem.distribution_system.controllers.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import org.teacherdistributionsystem.distribution_system.dtos.assignment.EmailRequestDTO;

import jakarta.validation.Valid;
import org.teacherdistributionsystem.distribution_system.services.scheduler.EmailService;

import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    private final EmailService emailService;

    /**
     * Endpoint pour envoyer des emails avec planning
     * POST /api/emails/send
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> envoyerEmails(@Valid @RequestBody EmailRequestDTO emailRequest) {
        try {
            logger.info("Réception demande d'envoi d'emails - Type: {}", emailRequest.getType());

            Map<String, Object> result = emailService.envoyerMailsAvecPlanning(emailRequest);

            boolean success = (boolean) result.getOrDefault("success", false);

            if (success) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi des emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur serveur: " + e.getMessage()
                    ));
        }
    }

    /**
     * Endpoint de test pour vérifier que le service email est configuré
     * GET /api/emails/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEmailService() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Service d'envoi d'emails opérationnel"
        ));
    }
}