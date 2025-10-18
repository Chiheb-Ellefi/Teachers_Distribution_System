package org.teacherdistributionsystem.distribution_system.services.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherAssignmentsDTO;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JsonDataLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(JsonDataLoaderService.class);
    private final ObjectMapper objectMapper;
    private final TeacherService teacherService;

    public JsonDataLoaderService(ObjectMapper objectMapper, TeacherService teacherService) {
        this.objectMapper = objectMapper;
        this.teacherService = teacherService;
    }

    /**
     * Charge les données depuis le nouveau format JSON et convertit en ancien format
     */
    public List<TeacherAssignmentsDTO> loadTeacherAssignments() {
        try {
            File file = new File("data/assignments.json");

            if (!file.exists()) {
                logger.error("Fichier JSON non trouvé: data/assignments.json");
                logger.error("Chemin absolu recherché: {}", file.getAbsolutePath());
                throw new RuntimeException("Fichier de données non trouvé: data/assignments.json");
            }

            // Lire le JSON
            Map<String, Object> jsonMap = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> examAssignments = (List<Map<String, Object>>) jsonMap.get("examAssignments");

            if (examAssignments == null) {
                throw new RuntimeException("Format JSON invalide: champ examAssignments manquant");
            }

            // ✅ CHARGER TOUTES LES DONNÉES DES ENSEIGNANTS EN UNE FOIS
            Map<Long, String> allGrades = teacherService.getAllGrades();
            Map<Long, String> allEmails = teacherService.getAllEmails();
            Map<Long, String> allNames = teacherService.getAllNames();

            // Convertir en ancien format avec enrichissement
            List<TeacherAssignmentsDTO> teachers = convertToTeacherAssignments(
                    examAssignments,
                    allGrades,
                    allEmails,
                    allNames
            );

            logger.info("Données converties avec succès: {} enseignants, {} examens",
                    teachers.size(), examAssignments.size());
            return teachers;

        } catch (IOException e) {
            logger.error("Erreur lors du chargement des données JSON", e);
            throw new RuntimeException("Erreur lors du chargement des données JSON: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ MÉTHODE MODIFIÉE - Convertit avec enrichissement
     */
    private List<TeacherAssignmentsDTO> convertToTeacherAssignments(
            List<Map<String, Object>> examAssignments,
            Map<Long, String> allGrades,
            Map<Long, String> allEmails,
            Map<Long, String> allNames) {

        Map<Long, TeacherAssignmentsDTO> teacherMap = new HashMap<>();

        for (Map<String, Object> exam : examAssignments) {
            Long ownerTeacherId = getLongValue(exam.get("ownerTeacherId"));
            String ownerTeacherName = (String) exam.get("ownerTeacherName");

            // Pour l'enseignant PROPRIÉTAIRE (responsable)
            addExamToTeacher(teacherMap, exam, ownerTeacherId, ownerTeacherName, "owner",
                    allGrades, allEmails, allNames);

            // Pour les enseignants SURVEILLANTS
            List<Map<String, Object>> assignedTeachers = (List<Map<String, Object>>) exam.get("assignedTeachers");
            if (assignedTeachers != null) {
                for (Map<String, Object> assignedTeacher : assignedTeachers) {
                    Long teacherId = getLongValue(assignedTeacher.get("teacherId"));
                    String teacherName = (String) assignedTeacher.get("teacherName");
                    addExamToTeacher(teacherMap, exam, teacherId, teacherName, "supervisor",
                            allGrades, allEmails, allNames);
                }
            }
        }

        return new ArrayList<>(teacherMap.values());
    }

    /**
     * ✅ MÉTHODE MODIFIÉE - Enrichissement avec les maps
     */
    private void addExamToTeacher(Map<Long, TeacherAssignmentsDTO> teacherMap,
                                  Map<String, Object> exam,
                                  Long teacherId,
                                  String teacherName,
                                  String role,
                                  Map<Long, String> allGrades,
                                  Map<Long, String> allEmails,
                                  Map<Long, String> allNames) {

        if (teacherId == null) {
            logger.warn("TeacherId null pour: {}", teacherName);
            return;
        }

        TeacherAssignmentsDTO teacher = teacherMap.computeIfAbsent(teacherId, id -> {
            TeacherAssignmentsDTO newTeacher = new TeacherAssignmentsDTO();
            newTeacher.setTeacherId(teacherId);

            // ✅ Nom complet depuis la DB ou depuis le JSON
            String fullName = allNames.getOrDefault(teacherId, teacherName);
            newTeacher.setTeacherName(fullName);

            newTeacher.setAssignments(new ArrayList<>());
            newTeacher.setAssignedSupervisions(0);

            // ✅ GRADE depuis la DB
            String grade = allGrades.get(teacherId);
            newTeacher.setGrade(grade != null ? grade : "N/A");

            // ✅ EMAIL depuis la DB
            String email = allEmails.get(teacherId);
            newTeacher.setEmail(email != null ? email : "N/A");

            // ✅ QUOTA selon le grade (à adapter selon votre logique)
            newTeacher.setQuotaSupervisions(getQuotaByGrade(grade));

            newTeacher.setUtilizationPercentage(0.0);

            logger.debug("Enseignant créé: {} - Grade: {}, Email: {}, Quota: {}",
                    fullName, newTeacher.getGrade(), newTeacher.getEmail(), newTeacher.getQuotaSupervisions());

            return newTeacher;
        });

        // Créer l'affectation
        TeacherAssignmentsDTO.TeacherAssignment assignment = new TeacherAssignmentsDTO.TeacherAssignment();
        assignment.setExamId((String) exam.get("examId"));
        assignment.setDay(getIntegerValue(exam.get("day")));
        assignment.setDayLabel((String) exam.get("dayLabel"));
        assignment.setSeance(getIntegerValue(exam.get("seance")));
        assignment.setSeanceLabel((String) exam.get("seanceLabel"));
        assignment.setRoom((String) exam.get("room"));
        assignment.setExamDate((String) exam.get("examDate"));
        assignment.setStartTime((String) exam.get("startTime"));
        assignment.setEndTime((String) exam.get("endTime"));

        teacher.getAssignments().add(assignment);

        // Mettre à jour les statistiques
        if ("supervisor".equals(role)) {
            teacher.setAssignedSupervisions(teacher.getAssignedSupervisions() + 1);
            if (teacher.getQuotaSupervisions() > 0) {
                double utilization = (double) teacher.getAssignedSupervisions() / teacher.getQuotaSupervisions() * 100;
                teacher.setUtilizationPercentage(Math.round(utilization * 100.0) / 100.0);
            }
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE - Détermine le quota selon le grade
     */
    private Integer getQuotaByGrade(String gradeCode) {
        if (gradeCode == null) return 10;

        // Adapter selon vos règles métier
        switch (gradeCode.toUpperCase()) {
            case "PR":  return 15;  // Professeur
            case "MA":  return 12;  // Maître Assistant
            case "MC":  return 10;  // Maître de Conférences
            case "AC":  return 8;   // Assistant Contractuel
            case "AS":  return 6;   // Assistant
            case "V":   return 5;   // Vacataire
            default:    return 10;  // Par défaut
        }
    }

    /**
     * ✅ MÉTHODE MODIFIÉE - Responsable avec enrichissement
     */
    public TeacherAssignmentsDTO getResponsibleTeacherDataByName(String teacherName) {
        try {
            File file = new File("data/assignments.json");
            Map<String, Object> jsonMap = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> examAssignments = (List<Map<String, Object>>) jsonMap.get("examAssignments");

            if (examAssignments == null) {
                throw new RuntimeException("Format JSON invalide: champ examAssignments manquant");
            }

            // Filtrer les examens où l'enseignant est propriétaire
            List<Map<String, Object>> responsibleExams = examAssignments.stream()
                    .filter(exam -> {
                        String ownerName = (String) exam.get("ownerTeacherName");
                        return ownerName != null && ownerName.equalsIgnoreCase(teacherName.trim());
                    })
                    .collect(Collectors.toList());

            if (responsibleExams.isEmpty()) {
                throw new RuntimeException("Enseignant responsable non trouvé: " + teacherName);
            }

            // ✅ CHARGER LES DONNÉES
            Map<Long, String> allGrades = teacherService.getAllGrades();
            Map<Long, String> allEmails = teacherService.getAllEmails();
            Map<Long, String> allNames = teacherService.getAllNames();

            // Convertir
            Map<Long, TeacherAssignmentsDTO> teacherMap = new HashMap<>();
            for (Map<String, Object> exam : responsibleExams) {
                Long ownerTeacherId = getLongValue(exam.get("ownerTeacherId"));
                String ownerTeacherName = (String) exam.get("ownerTeacherName");
                addExamToTeacher(teacherMap, exam, ownerTeacherId, ownerTeacherName, "responsible",
                        allGrades, allEmails, allNames);
            }

            TeacherAssignmentsDTO responsibleTeacher = teacherMap.values().iterator().next();
            logger.info("Données responsable chargées: {} examens", responsibleTeacher.getAssignments().size());
            return responsibleTeacher;

        } catch (IOException e) {
            logger.error("Erreur lors du chargement des données responsable", e);
            throw new RuntimeException("Erreur: " + e.getMessage(), e);
        }
    }

    /**
     * Récupère la liste de tous les enseignants responsables
     */
    public List<String> getAllResponsibleTeacherNames() {
        try {
            File file = new File("data/assignments.json");
            Map<String, Object> jsonMap = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> examAssignments = (List<Map<String, Object>>) jsonMap.get("examAssignments");

            if (examAssignments == null) {
                return new ArrayList<>();
            }

            return examAssignments.stream()
                    .map(exam -> (String) exam.get("ownerTeacherName"))
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

        } catch (IOException e) {
            logger.error("Erreur lors du chargement des responsables", e);
            throw new RuntimeException("Erreur lors du chargement des responsables: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifie si un enseignant a des responsabilités
     */
    public boolean isResponsibleTeacher(String teacherName) {
        try {
            getResponsibleTeacherDataByName(teacherName);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Récupère la date pour une séance spécifique
     */
    public String getSessionDate(Integer day) {
        try {
            File file = new File("data/assignments.json");
            Map<String, Object> jsonMap = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> examAssignments = (List<Map<String, Object>>) jsonMap.get("examAssignments");

            if (examAssignments == null) return "Jour " + day;

            // Trouver le premier examen avec ce jour pour récupérer la date
            Optional<String> date = examAssignments.stream()
                    .filter(exam -> day.equals(getIntegerValue(exam.get("day"))))
                    .map(exam -> (String) exam.get("examDate"))
                    .filter(Objects::nonNull)
                    .findFirst();

            return date.orElse("Jour " + day);

        } catch (IOException e) {
            logger.error("Erreur lors de la récupération de la date de session", e);
            return "Jour " + day;
        }
    }

    // MÉTHODES EXISTANTES
    public TeacherAssignmentsDTO getTeacherDataByName(String teacherName) {
        List<TeacherAssignmentsDTO> teachers = loadTeacherAssignments();
        return teachers.stream()
                .filter(teacher -> teacher.getTeacherName() != null &&
                        teacher.getTeacherName().equalsIgnoreCase(teacherName.trim()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enseignant non trouvé: " + teacherName));
    }

    public TeacherAssignmentsDTO getTeacherDataById(Long teacherId) {
        List<TeacherAssignmentsDTO> teachers = loadTeacherAssignments();
        return teachers.stream()
                .filter(teacher -> teacher.getTeacherId() != null && teacher.getTeacherId().equals(teacherId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enseignant non trouvé avec l'ID: " + teacherId));
    }

    public List<String> getAllTeacherNames() {
        List<TeacherAssignmentsDTO> teachers = loadTeacherAssignments();
        return teachers.stream()
                .map(TeacherAssignmentsDTO::getTeacherName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public boolean teacherExists(String teacherName) {
        try {
            getTeacherDataByName(teacherName);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public List<TeacherAssignmentsDTO> getAllTeachersWithData() {
        return loadTeacherAssignments();
    }

    // Méthodes utilitaires
    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Long) return (Long) value;
        return null;
    }

    private Integer getIntegerValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        return null;
    }
}