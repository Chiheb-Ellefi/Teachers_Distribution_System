package org.teacherdistributionsystem.distribution_system.services.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherAssignmentsDTO;
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

    public JsonDataLoaderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

            // Lire le JSON comme Map pour extraire examAssignments
            Map<String, Object> jsonMap = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});

            // Extraire la liste des examAssignments
            List<Map<String, Object>> examAssignments = (List<Map<String, Object>>) jsonMap.get("examAssignments");

            if (examAssignments == null) {
                throw new RuntimeException("Format JSON invalide: champ examAssignments manquant");
            }

            // Convertir en ancien format
            List<TeacherAssignmentsDTO> teachers = convertToTeacherAssignments(examAssignments);

            logger.info("Données converties avec succès: {} enseignants, {} examens",
                    teachers.size(), examAssignments.size());
            return teachers;

        } catch (IOException e) {
            logger.error("Erreur lors du chargement des données JSON", e);
            throw new RuntimeException("Erreur lors du chargement des données JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Convertit le nouveau format en ancien format TeacherAssignmentsDTO
     */
    private List<TeacherAssignmentsDTO> convertToTeacherAssignments(List<Map<String, Object>> examAssignments) {
        Map<Long, TeacherAssignmentsDTO> teacherMap = new HashMap<>();

        for (Map<String, Object> exam : examAssignments) {
            // Extraire les données de l'examen
            Long ownerTeacherId = getLongValue(exam.get("ownerTeacherId"));
            String ownerTeacherName = (String) exam.get("ownerTeacherName");

            // Pour l'enseignant PROPRIÉTAIRE (responsable)
            addExamToTeacher(teacherMap, exam, ownerTeacherId, ownerTeacherName, "owner");

            // Pour les enseignants SURVEILLANTS
            List<Map<String, Object>> assignedTeachers = (List<Map<String, Object>>) exam.get("assignedTeachers");
            if (assignedTeachers != null) {
                for (Map<String, Object> assignedTeacher : assignedTeachers) {
                    Long teacherId = getLongValue(assignedTeacher.get("teacherId"));
                    String teacherName = (String) assignedTeacher.get("teacherName");
                    addExamToTeacher(teacherMap, exam, teacherId, teacherName, "supervisor");
                }
            }
        }

        return new ArrayList<>(teacherMap.values());
    }

    private void addExamToTeacher(Map<Long, TeacherAssignmentsDTO> teacherMap,
                                  Map<String, Object> exam,
                                  Long teacherId,
                                  String teacherName,
                                  String role) {

        // Vérifier que teacherId n'est pas null
        if (teacherId == null) {
            logger.warn("TeacherId null pour: {}", teacherName);
            return;
        }

        TeacherAssignmentsDTO teacher = teacherMap.computeIfAbsent(teacherId, id -> {
            TeacherAssignmentsDTO newTeacher = new TeacherAssignmentsDTO();
            newTeacher.setTeacherId(teacherId);
            newTeacher.setTeacherName(teacherName);
            newTeacher.setAssignments(new ArrayList<>());
            newTeacher.setAssignedSupervisions(0);
            newTeacher.setQuotaSupervisions(10); // Valeur par défaut
            newTeacher.setUtilizationPercentage(0.0);
            return newTeacher;
        });

        // Créer l'affectation avec TOUTES les données
        TeacherAssignmentsDTO.TeacherAssignment assignment = new TeacherAssignmentsDTO.TeacherAssignment();
        assignment.setExamId((String) exam.get("examId"));
        assignment.setDay(getIntegerValue(exam.get("day")));
        assignment.setDayLabel((String) exam.get("dayLabel"));
        assignment.setSeance(getIntegerValue(exam.get("seance")));
        assignment.setSeanceLabel((String) exam.get("seanceLabel"));
        assignment.setRoom((String) exam.get("room"));

        // AJOUT DES NOUVELLES DONNÉES - dates réelles
        assignment.setExamDate((String) exam.get("examDate"));
        assignment.setStartTime((String) exam.get("startTime"));
        assignment.setEndTime((String) exam.get("endTime"));

        teacher.getAssignments().add(assignment);

        // Mettre à jour les statistiques (seulement pour les surveillants)
        if ("supervisor".equals(role)) {
            teacher.setAssignedSupervisions(teacher.getAssignedSupervisions() + 1);
            if (teacher.getQuotaSupervisions() > 0) {
                double utilization = (double) teacher.getAssignedSupervisions() / teacher.getQuotaSupervisions() * 100;
                teacher.setUtilizationPercentage(Math.round(utilization * 100.0) / 100.0);
            }
        }
    }

    /**
     * Récupère les données d'un enseignant RESPONSABLE (owner) par nom
     */
    public TeacherAssignmentsDTO getResponsibleTeacherDataByName(String teacherName) {
        try {
            File file = new File("data/assignments.json");
            Map<String, Object> jsonMap = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> examAssignments = (List<Map<String, Object>>) jsonMap.get("examAssignments");

            if (examAssignments == null) {
                throw new RuntimeException("Format JSON invalide: champ examAssignments manquant");
            }

            // Filtrer uniquement les examens où l'enseignant est propriétaire
            List<Map<String, Object>> responsibleExams = examAssignments.stream()
                    .filter(exam -> {
                        String ownerName = (String) exam.get("ownerTeacherName");
                        return ownerName != null && ownerName.equalsIgnoreCase(teacherName.trim());
                    })
                    .collect(Collectors.toList());

            if (responsibleExams.isEmpty()) {
                throw new RuntimeException("Enseignant responsable non trouvé ou aucun examen en responsabilité: " + teacherName);
            }

            // Convertir en TeacherAssignmentsDTO
            Map<Long, TeacherAssignmentsDTO> teacherMap = new HashMap<>();

            for (Map<String, Object> exam : responsibleExams) {
                Long ownerTeacherId = getLongValue(exam.get("ownerTeacherId"));
                String ownerTeacherName = (String) exam.get("ownerTeacherName");
                addExamToTeacher(teacherMap, exam, ownerTeacherId, ownerTeacherName, "responsible");
            }

            TeacherAssignmentsDTO responsibleTeacher = teacherMap.values().iterator().next();
            logger.info("Données responsable chargées: {} examens en responsabilité", responsibleTeacher.getAssignments().size());
            return responsibleTeacher;

        } catch (IOException e) {
            logger.error("Erreur lors du chargement des données responsable", e);
            throw new RuntimeException("Erreur lors du chargement des données responsable: " + e.getMessage(), e);
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

    // MÉTHODES EXISTANTES (gardez-les telles quelles)
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