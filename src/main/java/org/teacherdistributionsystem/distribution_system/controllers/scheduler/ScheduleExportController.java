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
     * GET - Données d'un enseignant
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
     * GET - Export PDF planning enseignant (surveillances)
     * Exemple: /api/schedule/export/teacher/Nihel%20Ben%20youssef/pdf
     */
    @GetMapping("/export/teacher/{teacherName}/pdf")
    public ResponseEntity<ByteArrayResource> exportTeacherSchedulePDF(@PathVariable String teacherName) {
        try {
            logger.info("Export PDF planning enseignant: {}", teacherName);

            ByteArrayOutputStream outputStream = pdfService.generateTeacherSchedulePDF(teacherName);
            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            String filename = "Planning_Surveillance_" + teacherName.replace(" ", "_") + ".pdf";

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
     * GET - Export PDF planning ENSEIGNANT RESPONSABLE
     * Exemple: /api/schedule/export/responsible/Nihel%20Ben%20youssef/pdf
     */
    @GetMapping("/export/responsible/{teacherName}/pdf")
    public ResponseEntity<ByteArrayResource> exportResponsibleTeacherPDF(@PathVariable String teacherName) {
        try {
            logger.info("Export PDF planning enseignant RESPONSABLE: {}", teacherName);

            ByteArrayOutputStream outputStream = pdfService.generateResponsibleTeacherSchedulePDF(teacherName);
            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            String filename = "Planning_Responsable_" + teacherName.replace(" ", "_") + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Erreur export PDF enseignant responsable: {}", teacherName, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET - Vérifie si un enseignant a des responsabilités
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
}