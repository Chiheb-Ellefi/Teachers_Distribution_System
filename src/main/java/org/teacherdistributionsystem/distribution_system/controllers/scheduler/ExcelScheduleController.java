package org.teacherdistributionsystem.distribution_system.controllers.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.services.scheduler.ExcelScheduleService;
import org.teacherdistributionsystem.distribution_system.services.scheduler.JsonDataLoaderService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "*")
public class ExcelScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(ExcelScheduleController.class);

    private final ExcelScheduleService excelScheduleService;
    private final JsonDataLoaderService jsonDataLoaderService;

    public ExcelScheduleController(ExcelScheduleService excelScheduleService,
                                   JsonDataLoaderService jsonDataLoaderService) {
        this.excelScheduleService = excelScheduleService;
        this.jsonDataLoaderService = jsonDataLoaderService;
    }

    /**
     * 1. Télécharger le planning par séance en Excel (AVEC signature)
     * URL: GET http://localhost:8080/api/schedules/session/excel?day=1&seance=1
     */
    @GetMapping("/session/excel")
    public ResponseEntity<byte[]> downloadSessionSchedule(@RequestParam Integer day,
                                                          @RequestParam Integer seance) {
        try {
            logger.info("Demande de téléchargement Excel planning séance - Jour: {}, Séance: {}", day, seance);

            ByteArrayOutputStream outputStream = excelScheduleService.generateSessionSchedule(day, seance);
            byte[] excelBytes = outputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "Planning_Seance_J" + day + "_S" + seance + ".xlsx");
            headers.setContentLength(excelBytes.length);

            logger.info("Planning séance Excel téléchargé avec succès - Jour: {}, Séance: {}", day, seance);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Erreur lors de la génération du planning séance Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 2. Télécharger le planning d'un enseignant surveillant en Excel (SANS signature)
     * URL: GET http://localhost:8080/api/schedules/teacher/excel?name=NomEnseignant
     */
    @GetMapping("/teacher/excel")
    public ResponseEntity<byte[]> downloadTeacherSchedule(@RequestParam String name) {
        try {
            logger.info("Demande de téléchargement Excel planning surveillant: {}", name);

            if (!jsonDataLoaderService.teacherExists(name)) {
                logger.warn("Enseignant non trouvé: {}", name);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ByteArrayOutputStream outputStream = excelScheduleService.generateTeacherSchedule(name);
            byte[] excelBytes = outputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "Planning_Surveillance_" + name.replace(" ", "_") + ".xlsx");
            headers.setContentLength(excelBytes.length);

            logger.info("Planning surveillant Excel téléchargé avec succès pour: {}", name);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Erreur lors de la génération du planning surveillant Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 3. Télécharger le planning d'un enseignant responsable en Excel (SANS signature)
     * URL: GET http://localhost:8080/api/schedules/responsible/excel?name=NomEnseignant
     */
    @GetMapping("/responsible/excel")
    public ResponseEntity<byte[]> downloadResponsibleTeacherSchedule(@RequestParam String name) {
        try {
            logger.info("Demande de téléchargement Excel planning responsable: {}", name);

            if (!jsonDataLoaderService.isResponsibleTeacher(name)) {
                logger.warn("Enseignant responsable non trouvé ou sans responsabilités: {}", name);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ByteArrayOutputStream outputStream = excelScheduleService.generateResponsibleTeacherSchedule(name);
            byte[] excelBytes = outputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "Planning_Responsable_" + name.replace(" ", "_") + ".xlsx");
            headers.setContentLength(excelBytes.length);

            logger.info("Planning responsable Excel téléchargé avec succès pour: {}", name);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Erreur lors de la génération du planning responsable Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 4. Télécharger le planning administration en Excel
     * URL: GET http://localhost:8080/api/schedules/admin/excel
     */
    @GetMapping("/admin/excel")
    public ResponseEntity<byte[]> downloadAdminSchedule() {
        try {
            logger.info("Demande de téléchargement du planning administration Excel");

            ByteArrayOutputStream outputStream = excelScheduleService.generateAdminSchedule();
            byte[] excelBytes = outputStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Planning_Administration.xlsx");
            headers.setContentLength(excelBytes.length);

            logger.info("Planning administration Excel téléchargé avec succès");
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Erreur lors de la génération du planning administration Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer la liste de tous les enseignants
     * URL: GET http://localhost:8080/api/schedules/teachers
     */
    @GetMapping("/teachers")
    public ResponseEntity<List<String>> getAllTeachers() {
        try {
            logger.info("Demande de la liste des enseignants");
            List<String> teachers = jsonDataLoaderService.getAllTeacherNames();
            logger.info("Liste des enseignants récupérée: {} enseignants", teachers.size());
            return ResponseEntity.ok(teachers);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de la liste des enseignants", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupérer la liste de tous les enseignants responsables
     * URL: GET http://localhost:8080/api/schedules/responsible-teachers
     */
    @GetMapping("/responsible-teachers")
    public ResponseEntity<List<String>> getAllResponsibleTeachers() {
        try {
            logger.info("Demande de la liste des enseignants responsables");
            List<String> responsibleTeachers = jsonDataLoaderService.getAllResponsibleTeacherNames();
            logger.info("Liste des enseignants responsables récupérée: {} enseignants", responsibleTeachers.size());
            return ResponseEntity.ok(responsibleTeachers);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de la liste des enseignants responsables", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Vérifier si un enseignant existe
     * URL: GET http://localhost:8080/api/schedules/teacher/exists?name=NomEnseignant
     */
    @GetMapping("/teacher/exists")
    public ResponseEntity<Boolean> checkTeacherExists(@RequestParam String name) {
        try {
            boolean exists = jsonDataLoaderService.teacherExists(name);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification de l'existence de l'enseignant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Vérifier si un enseignant a des responsabilités
     * URL: GET http://localhost:8080/api/schedules/responsible/exists?name=NomEnseignant
     */
    @GetMapping("/responsible/exists")
    public ResponseEntity<Boolean> checkResponsibleTeacherExists(@RequestParam String name) {
        try {
            boolean exists = jsonDataLoaderService.isResponsibleTeacher(name);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification des responsabilités de l'enseignant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}