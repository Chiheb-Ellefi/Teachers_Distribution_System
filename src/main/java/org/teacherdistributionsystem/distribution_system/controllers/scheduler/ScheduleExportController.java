package org.teacherdistributionsystem.distribution_system.controllers.scheduler;

import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherAssignmentsDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.services.scheduler.JsonDataLoaderService;
import org.teacherdistributionsystem.distribution_system.services.scheduler.PDFScheduleService;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "*")
public class ScheduleExportController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleExportController.class);

    private final PDFScheduleService pdfService;
    private final JsonDataLoaderService jsonDataLoaderService;

    public ScheduleExportController(PDFScheduleService pdfService,
                                    JsonDataLoaderService jsonDataLoaderService) {
        this.pdfService = pdfService;
        this.jsonDataLoaderService = jsonDataLoaderService;
    }

    /**
     * GET - Liste de tous les enseignants
     */
    @GetMapping("/teachers")
    public ResponseEntity<List<String>> getAllTeachers() {
        try {
            List<String> teachers = jsonDataLoaderService.getAllTeacherNames();
            return ResponseEntity.ok(teachers);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des enseignants", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Données d'un enseignant PAR NOM
     */
    @GetMapping("/teachers/{teacherName}")
    public ResponseEntity<TeacherAssignmentsDTO> getTeacherData(@PathVariable String teacherName) {
        try {
            TeacherAssignmentsDTO teacher = jsonDataLoaderService.getTeacherDataByName(teacherName);
            return ResponseEntity.ok(teacher);
        } catch (Exception e) {
            logger.error("Enseignant non trouvé: {}", teacherName, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET - Données d'un enseignant PAR EMAIL (NOUVEAU)
     * Exemple: /api/schedule/teachers/email/john.doe@example.com
     */
    @GetMapping("/teachers/email/{teacherEmail}")
    public ResponseEntity<TeacherAssignmentsDTO> getTeacherDataByEmail(@PathVariable String teacherEmail) {
        try {
            TeacherAssignmentsDTO teacher = jsonDataLoaderService.getTeacherDataByEmail(teacherEmail);
            return ResponseEntity.ok(teacher);
        } catch (Exception e) {
            logger.error("Enseignant non trouvé avec l'email: {}", teacherEmail, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET - Export PDF planning enseignant (surveillances) PAR EMAIL (MODIFIÉ)
     * Exemple: /api/schedule/export/teacher/email/john.doe@example.com/pdf
     */
    @GetMapping("/export/teacher/email/{teacherEmail}/pdf")
    public ResponseEntity<ByteArrayResource> exportTeacherSchedulePDF(@PathVariable String teacherEmail) {
        try {
            logger.info("Export PDF planning enseignant par email: {}", teacherEmail);

            ByteArrayOutputStream outputStream = pdfService.generateTeacherSchedulePDF(teacherEmail);
            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            // Récupérer le nom pour le filename
            TeacherAssignmentsDTO teacher = jsonDataLoaderService.getTeacherDataByEmail(teacherEmail);
            String teacherName = teacher.getTeacherName().replace(" ", "_");
            String filename = "Planning_Surveillance_" + teacherName + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Erreur export PDF enseignant", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Export PDF planning séance (UN SEUL jour + UNE SEULE séance)
     * Exemple: /api/schedule/export/session/pdf?day=1&seance=1
     */
    @GetMapping("/export/session/pdf")
    public ResponseEntity<ByteArrayResource> exportSessionSchedulePDF(
            @RequestParam Integer day,
            @RequestParam Integer seance) {
        try {
            logger.info("Export PDF planning séance - Jour: {}, Séance: {}", day, seance);

            ByteArrayOutputStream outputStream = pdfService.generateSessionSchedulePDF();
            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            String filename = "Planning_Jour" + day + "_Seance" + seance + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Erreur export PDF séance", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Export PDF de TOUTES les séances de TOUS les jours (NOUVEAU)
     * Exemple: /api/schedule/export/all-sessions/pdf
     */
    @GetMapping("/export/all-sessions/pdf")
    public ResponseEntity<ByteArrayResource> exportAllSessionsSchedulePDF() {
        try {
            logger.info("Export PDF planning complet - Toutes les séances de tous les jours");

            ByteArrayOutputStream outputStream = pdfService.generateAllSessionsSchedulePDF();
            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            String filename = "Planning_Complet_Toutes_Seances.pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Erreur export PDF toutes séances", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST - Génération complète de tous les plannings
     */
    @PostMapping("/generate-all")
    public ResponseEntity<Map<String, Object>> generateAllSchedules() {
        try {
            logger.info("Génération de tous les plannings");

            List<TeacherAssignmentsDTO> teachers = jsonDataLoaderService.loadTeacherAssignments();

            Map<String, Object> result = new HashMap<>();
            result.put("totalTeachers", teachers.size());
            result.put("totalAssignments", teachers.stream()
                    .mapToInt(t -> t.getAssignments().size())
                    .sum());
            result.put("message", "Tous les plannings peuvent être générés");
            result.put("teachers", teachers.stream()
                    .map(TeacherAssignmentsDTO::getTeacherName)
                    .toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Erreur génération complète", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Informations générales sur les affectations
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getScheduleInfo() {
        try {
            List<TeacherAssignmentsDTO> teachers = jsonDataLoaderService.loadTeacherAssignments();

            Map<String, Object> info = new HashMap<>();
            info.put("totalTeachers", teachers.size());
            info.put("totalAssignments", teachers.stream()
                    .mapToInt(t -> t.getAssignments().size())
                    .sum());
            info.put("averageUtilization", teachers.stream()
                    .mapToDouble(TeacherAssignmentsDTO::getUtilizationPercentage)
                    .average()
                    .orElse(0.0));

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            logger.error("Erreur récupération info", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Récupère tous les enseignants avec leurs affectations pour un jour
     */
    @GetMapping("/day/{day}")
    public ResponseEntity<List<TeacherAssignmentsDTO>> getTeachersByDay(@PathVariable Integer day) {
        try {
            List<TeacherAssignmentsDTO> allTeachers = jsonDataLoaderService.loadTeacherAssignments();

            List<TeacherAssignmentsDTO> filteredTeachers = allTeachers.stream()
                    .filter(teacher -> teacher.getAssignments().stream()
                            .anyMatch(a -> a.getDay().equals(day)))
                    .toList();

            return ResponseEntity.ok(filteredTeachers);

        } catch (Exception e) {
            logger.error("Erreur récupération enseignants pour le jour {}", day, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Récupère tous les enseignants pour une séance spécifique
     */
    @GetMapping("/session")
    public ResponseEntity<List<Map<String, Object>>> getTeachersBySession(
            @RequestParam Integer day,
            @RequestParam Integer seance) {
        try {
            List<TeacherAssignmentsDTO> allTeachers = jsonDataLoaderService.loadTeacherAssignments();

            List<Map<String, Object>> result = allTeachers.stream()
                    .filter(teacher -> teacher.getAssignments().stream()
                            .anyMatch(a -> a.getDay().equals(day) && a.getSeance().equals(seance)))
                    .map(teacher -> {
                        Map<String, Object> teacherInfo = new HashMap<>();
                        teacherInfo.put("teacherName", teacher.getTeacherName());
                        teacherInfo.put("grade", teacher.getGrade());

                        teacher.getAssignments().stream()
                                .filter(a -> a.getDay().equals(day) && a.getSeance().equals(seance))
                                .findFirst()
                                .ifPresent(assignment -> {
                                    teacherInfo.put("room", assignment.getRoom());
                                    teacherInfo.put("examId", assignment.getExamId());
                                });

                        return teacherInfo;
                    })
                    .toList();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Erreur récupération séance jour:{} séance:{}", day, seance, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Liste de tous les enseignants RESPONSABLES
     */
    @GetMapping("/responsible-teachers")
    public ResponseEntity<List<String>> getAllResponsibleTeachers() {
        try {
            List<String> responsibleTeachers = jsonDataLoaderService.getAllResponsibleTeacherNames();
            return ResponseEntity.ok(responsibleTeachers);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des enseignants responsables", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Export PDF planning ENSEIGNANT RESPONSABLE PAR EMAIL (MODIFIÉ)
     * Exemple: /api/schedule/export/responsible/email/john.doe@example.com/pdf
     */
    @GetMapping("/export/responsible/email/{teacherEmail}/pdf")
    public ResponseEntity<ByteArrayResource> exportResponsibleTeacherPDF(@PathVariable String teacherEmail) {
        try {
            logger.info("Export PDF planning enseignant RESPONSABLE par email: {}", teacherEmail);

            ByteArrayOutputStream outputStream = pdfService.generateResponsibleTeacherSchedulePDF(teacherEmail);
            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            // Récupérer le nom pour le filename
            TeacherAssignmentsDTO teacher = jsonDataLoaderService.getResponsibleTeacherDataByEmail(teacherEmail);
            String teacherName = teacher.getTeacherName().replace(" ", "_");
            String filename = "Planning_Responsable_" + teacherName + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Erreur export PDF enseignant responsable par email: {}", teacherEmail, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Vérifie si un enseignant a des responsabilités PAR NOM
     */
    @GetMapping("/teachers/{teacherName}/is-responsible")
    public ResponseEntity<Map<String, Object>> isTeacherResponsible(@PathVariable String teacherName) {
        try {
            boolean isResponsible = jsonDataLoaderService.isResponsibleTeacher(teacherName);
            Map<String, Object> response = new HashMap<>();
            response.put("teacherName", teacherName);
            response.put("isResponsible", isResponsible);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur vérification responsable: {}", teacherName, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Vérifie si un enseignant a des responsabilités PAR EMAIL (NOUVEAU)
     * Exemple: /api/schedule/teachers/email/john.doe@example.com/is-responsible
     */
    @GetMapping("/teachers/email/{teacherEmail}/is-responsible")
    public ResponseEntity<Map<String, Object>> isTeacherResponsibleByEmail(@PathVariable String teacherEmail) {
        try {
            // Vérifier si l'enseignant existe d'abord
            TeacherAssignmentsDTO teacher = jsonDataLoaderService.getTeacherDataByEmail(teacherEmail);

            // Vérifier s'il a des responsabilités
            boolean isResponsible = jsonDataLoaderService.isResponsibleTeacher(teacher.getTeacherName());

            Map<String, Object> response = new HashMap<>();
            response.put("teacherEmail", teacherEmail);
            response.put("teacherName", teacher.getTeacherName());
            response.put("isResponsible", isResponsible);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur vérification responsable par email: {}", teacherEmail, e);
            return ResponseEntity.badRequest().build();
        }
    }
}