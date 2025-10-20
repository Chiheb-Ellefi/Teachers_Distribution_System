package org.teacherdistributionsystem.distribution_system.services.scheduler;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherAssignmentsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PDFScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(PDFScheduleService.class);
    private final JsonDataLoaderService jsonDataLoaderService;

    public PDFScheduleService(JsonDataLoaderService jsonDataLoaderService) {
        this.jsonDataLoaderService = jsonDataLoaderService;
    }

    /**
     * Génère le PDF du planning pour un enseignant (par EMAIL)
     */
    public ByteArrayOutputStream generateTeacherSchedulePDF(String teacherEmail) {
        logger.info("Génération PDF planning enseignant par email: {}", teacherEmail);

        TeacherAssignmentsDTO teacherData = jsonDataLoaderService.getTeacherDataByEmail(teacherEmail);
        String html = generateTeacherScheduleHTML(teacherData);
        return convertHTMLToPDF(html);
    }

    /**
     * Génère le PDF du planning complet (tous les jours, toutes les séances dans l'ordre)
     */
    public ByteArrayOutputStream generateSessionSchedulePDF() {
        logger.info("Génération PDF planning complet - Tous les jours, toutes les séances");

        List<TeacherAssignmentsDTO> allTeachers = jsonDataLoaderService.loadTeacherAssignments();
        String html = generateCompleteScheduleHTML(allTeachers);
        return convertHTMLToPDF(html);
    }

    /**
     * Génère le HTML pour le planning complet (tous les jours, toutes les séances)
     */
    private String generateCompleteScheduleHTML(List<TeacherAssignmentsDTO> allTeachers) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'/>");
        html.append("<style>");
        html.append(getCommonStyles());
        html.append("""
        .day-section {
            margin-bottom: 30px;
            page-break-inside: avoid;
        }
        .day-title {
            background-color: #2c5aa0;
            color: white;
            padding: 10px;
            font-size: 14pt;
            font-weight: bold;
            margin-bottom: 15px;
            border-radius: 4px;
        }
        .session-section {
            margin-bottom: 20px;
            page-break-inside: avoid;
        }
        .session-title {
            background-color: #4a7bc8;
            color: white;
            padding: 8px 12px;
            font-size: 12pt;
            font-weight: bold;
            margin-bottom: 10px;
            border-radius: 4px;
        }
    """);
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // En-tête
        html.append(generateHeader("EXD-FR-08-01", "0504-24", "Page 1/1"));

        // Titre principal
        html.append("<div class='session-info'>");
        html.append("<div style='font-size: 16pt; font-weight: bold; text-align: center; margin-bottom: 10px;'>");
        html.append("LISTE COMPLÈTE DES SURVEILLANTS");
        html.append("</div>");
        html.append("<div style='text-align: center; font-size: 12pt;'>");
        html.append("AU : 2024-2025 - Semestre : 2 - Session : Principale");
        html.append("</div>");
        html.append("</div>");

        // Récupérer tous les jours et séances uniques
        Map<Integer, Map<Integer, List<TeacherAssignmentsDTO>>> assignmentsByDayAndSession =
                organizeAssignmentsByDayAndSession(allTeachers);

        // Trier les jours
        List<Integer> sortedDays = assignmentsByDayAndSession.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        // Parcourir tous les jours
        for (Integer day : sortedDays) {
            String date = jsonDataLoaderService.getSessionDate(day);

            html.append("<div class='day-section'>");
            html.append("<div class='day-title'>");
            html.append("JOUR ").append(day).append(" - ").append(date);
            html.append("</div>");

            Map<Integer, List<TeacherAssignmentsDTO>> sessionsForDay = assignmentsByDayAndSession.get(day);

            // Trier les séances dans l'ordre S1, S2, S3, S4
            List<Integer> sortedSessions = Arrays.asList(1, 2, 3, 4);

            // Parcourir toutes les séances du jour dans l'ordre
            for (Integer session : sortedSessions) {
                html.append("<div class='session-section'>");
                html.append("<div class='session-title'>");
                html.append("SÉANCE S").append(session).append(" - ").append(getTimeSlot(session));
                html.append("</div>");

                // Tableau pour cette séance
                html.append("<table class='schedule-table'>");
                html.append("<thead>");
                html.append("<tr>");
                html.append("<th>Enseignant</th>");
                html.append("<th class='center'>Salle</th>");
                html.append("<th class='signature-col center'>Signature</th>");
                html.append("</tr>");
                html.append("</thead>");
                html.append("<tbody>");

                List<TeacherAssignmentsDTO> teachersForSession = sessionsForDay.get(session);

                if (teachersForSession == null || teachersForSession.isEmpty()) {
                    html.append("<tr><td colspan='3' class='center'>Aucun enseignant pour cette séance</td></tr>");
                } else {
                    // Trier les enseignants par nom
                    List<TeacherAssignmentsDTO> sortedTeachers = teachersForSession.stream()
                            .sorted((t1, t2) -> t1.getTeacherName().compareTo(t2.getTeacherName()))
                            .collect(Collectors.toList());

                    for (TeacherAssignmentsDTO teacher : sortedTeachers) {
                        TeacherAssignmentsDTO.TeacherAssignment assignment = teacher.getAssignments().stream()
                                .filter(a -> a.getDay().equals(day) && a.getSeance().equals(session))
                                .findFirst()
                                .orElse(null);

                        if (assignment != null) {
                            html.append("<tr>");
                            html.append("<td>").append(escapeHtml(teacher.getTeacherName())).append("</td>");
                            html.append("<td class='center'>").append(escapeHtml(assignment.getRoom())).append("</td>");
                            html.append("<td></td>");
                            html.append("</tr>");
                        }
                    }
                }

                html.append("</tbody>");
                html.append("</table>");
                html.append("</div>"); // fin session-section
            }

            html.append("</div>"); // fin day-section
        }

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Génère le PDF du planning pour TOUTES les séances de TOUS les jours
     */
    public ByteArrayOutputStream generateAllSessionsSchedulePDF() {
        logger.info("Génération PDF planning complet - Toutes les séances de tous les jours");

        List<TeacherAssignmentsDTO> allTeachers = jsonDataLoaderService.loadTeacherAssignments();
        String html = generateAllSessionsScheduleHTML(allTeachers);
        return convertHTMLToPDF(html);
    }

    /**
     * Génère le HTML pour toutes les séances de tous les jours
     */
    private String generateAllSessionsScheduleHTML(List<TeacherAssignmentsDTO> allTeachers) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'/>");
        html.append("<style>");
        html.append(getCommonStyles());
        html.append("""
        .session-separator {
            page-break-after: always;
            margin-bottom: 30px;
        }
        .session-separator:last-child {
            page-break-after: avoid;
        }
        """);
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // Déterminer le nombre de jours disponibles
        int maxDay = allTeachers.stream()
                .flatMap(t -> t.getAssignments() != null ? t.getAssignments().stream() : java.util.stream.Stream.empty())
                .map(TeacherAssignmentsDTO.TeacherAssignment::getDay)
                .max(Integer::compareTo)
                .orElse(0);

        // Boucler sur tous les jours
        for (int day = 1; day <= maxDay; day++) {
            // Boucler sur toutes les séances (S1 à S4)
            for (int seance = 1; seance <= 4; seance++) {
                // Vérifier s'il y a des enseignants pour cette séance
                final int currentDay = day;
                final int currentSeance = seance;

                List<TeacherAssignmentsDTO> filteredTeachers = allTeachers.stream()
                        .filter(teacher -> teacher.getAssignments() != null && teacher.getAssignments().stream()
                                .anyMatch(a -> a.getDay().equals(currentDay) && a.getSeance().equals(currentSeance)))
                        .sorted((t1, t2) -> t1.getTeacherName().compareTo(t2.getTeacherName()))
                        .collect(Collectors.toList());

                // Ne générer une section que s'il y a des enseignants
                if (!filteredTeachers.isEmpty()) {
                    html.append("<div class='session-separator'>");

                    // En-tête (une seule fois par séance)
                    html.append(generateHeader("EXD-FR-08-01", "0504-24", "Page 1/1"));

                    // Info session avec date RÉELLE
                    String date = jsonDataLoaderService.getSessionDate(currentDay);

                    html.append("<div class='session-info'>");
                    html.append("<div>AU : 2024-2025 - Semestre : 2 - Session : Principale</div>");
                    html.append("<div>Date : ").append(escapeHtml(date));
                    html.append(" - Séance : S").append(currentSeance).append("</div>");
                    html.append("</div>");

                    // Tableau des surveillants
                    html.append("<table class='schedule-table'>");
                    html.append("<thead>");
                    html.append("<tr>");
                    html.append("<th>Enseignant</th>");
                    html.append("<th class='center'>Salle</th>");
                    html.append("<th class='signature-col center'>Signature</th>");
                    html.append("</tr>");
                    html.append("</thead>");
                    html.append("<tbody>");

                    for (TeacherAssignmentsDTO teacher : filteredTeachers) {
                        TeacherAssignmentsDTO.TeacherAssignment assignment = teacher.getAssignments().stream()
                                .filter(a -> a.getDay().equals(currentDay) && a.getSeance().equals(currentSeance))
                                .findFirst()
                                .orElse(null);

                        if (assignment != null) {
                            html.append("<tr>");
                            html.append("<td>").append(escapeHtml(teacher.getTeacherName())).append("</td>");
                            html.append("<td class='center'>").append(escapeHtml(assignment.getRoom())).append("</td>");
                            html.append("<td></td>");
                            html.append("</tr>");
                        }
                    }

                    html.append("</tbody>");
                    html.append("</table>");

                    html.append("</div>"); // Fin session-separator
                }
            }
        }

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Organise les affectations par jour puis par séance
     * Retourne une Map: Jour -> Séance -> Liste d'enseignants
     */
    private Map<Integer, Map<Integer, List<TeacherAssignmentsDTO>>> organizeAssignmentsByDayAndSession(
            List<TeacherAssignmentsDTO> allTeachers) {

        Map<Integer, Map<Integer, List<TeacherAssignmentsDTO>>> organized = new HashMap<>();

        for (TeacherAssignmentsDTO teacher : allTeachers) {
            if (teacher.getAssignments() == null) continue;

            for (TeacherAssignmentsDTO.TeacherAssignment assignment : teacher.getAssignments()) {
                Integer day = assignment.getDay();
                Integer seance = assignment.getSeance();

                // Créer la structure si elle n'existe pas
                organized.putIfAbsent(day, new HashMap<>());
                organized.get(day).putIfAbsent(seance, new ArrayList<>());

                // Ajouter l'enseignant à cette séance
                organized.get(day).get(seance).add(teacher);
            }
        }

        return organized;
    }

    /**
     * Génère le PDF du planning pour un enseignant RESPONSABLE (par EMAIL)
     */
    public ByteArrayOutputStream generateResponsibleTeacherSchedulePDF(String teacherEmail) {
        logger.info("Génération PDF planning enseignant RESPONSABLE par email: {}", teacherEmail);

        TeacherAssignmentsDTO teacherData = jsonDataLoaderService.getResponsibleTeacherDataByEmail(teacherEmail);
        String html = generateResponsibleTeacherScheduleHTML(teacherData);
        return convertHTMLToPDF(html);
    }

    /**
     * Génère le HTML pour le planning enseignant (avec dates réelles)
     */
    private String generateTeacherScheduleHTML(TeacherAssignmentsDTO teacher) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'/>");
        html.append("<style>");
        html.append(getCommonStyles());
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // En-tête
        html.append(generateHeader("EXD-FR-08-01", "0504-24", "Page 1/1"));

        // Section Notes
        html.append("<div class='notes-section'>");
        html.append("<div class='notes-label'>Notes à</div>");
        html.append("<div class='teacher-name'>");
        html.append(teacher.getGrade() != null ? teacher.getGrade() + " " : "Mr/Mme ");
        html.append(escapeHtml(teacher.getTeacherName()));
        html.append("</div>");
        html.append("</div>");

        // Message CORRIGÉ
        html.append("<div class='message'>");
        html.append("Cher (e) Collègue,<br/>");
        html.append("Vous êtes prié (e) d'assurer la surveillance des examens selon le calendrier ci joint.");
        html.append("</div>");

        // Tableau SANS colonne signature
        html.append("<table class='schedule-table'>");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th class='date-col'>Date</th>");
        html.append("<th class='heure-col center'>Heure</th>");
        html.append("<th class='duree-col center'>Durée</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        // Vérifier si la liste d'assignments n'est pas null
        if (teacher.getAssignments() != null && !teacher.getAssignments().isEmpty()) {
            // Utiliser un Set pour éliminer les doublons basés sur date, heure et durée
            Set<String> uniqueAssignments = new HashSet<>();
            List<TeacherAssignmentsDTO.TeacherAssignment> uniqueSortedAssignments = new ArrayList<>();

            for (TeacherAssignmentsDTO.TeacherAssignment assignment : teacher.getAssignments()) {
                String date = assignment.getExamDate() != null ? assignment.getExamDate() : "Jour " + assignment.getDay();
                String timeSlot = getRealTimeSlot(assignment);
                String duration = getDuration(assignment);

                String assignmentKey = date + "|" + timeSlot + "|" + duration;

                if (!uniqueAssignments.contains(assignmentKey)) {
                    uniqueAssignments.add(assignmentKey);
                    uniqueSortedAssignments.add(assignment);
                }
            }

            // Trier les affectations uniques par date puis par séance
            List<TeacherAssignmentsDTO.TeacherAssignment> sortedAssignments = uniqueSortedAssignments.stream()
                    .sorted((a1, a2) -> {
                        int dateCompare = a1.getExamDate().compareTo(a2.getExamDate());
                        if (dateCompare != 0) return dateCompare;
                        return a1.getSeance().compareTo(a2.getSeance());
                    })
                    .collect(Collectors.toList());

            for (TeacherAssignmentsDTO.TeacherAssignment assignment : sortedAssignments) {
                html.append("<tr>");
                // Date RÉELLE depuis examDate
                String date = assignment.getExamDate() != null ? assignment.getExamDate() : "Jour " + assignment.getDay();
                html.append("<td>").append(escapeHtml(date)).append("</td>");
                // Heure RÉELLE depuis startTime/endTime
                String timeSlot = getRealTimeSlot(assignment);
                html.append("<td class='center'>").append(timeSlot).append("</td>");
                // Durée calculée
                html.append("<td class='center'>").append(getDuration(assignment)).append("</td>");
                html.append("</tr>");
            }
        } else {
            html.append("<tr><td colspan='3' class='center'>Aucune affectation</td></tr>");
        }

        html.append("</tbody>");
        html.append("</table>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Génère le HTML pour le planning d'une séance (avec dates réelles)
     */
    private String generateSessionScheduleHTML(Integer day, Integer seance, List<TeacherAssignmentsDTO> allTeachers) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'/>");
        html.append("<style>");
        html.append(getCommonStyles());
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // En-tête
        html.append(generateHeader("EXD-FR-08-01", "0504-24", "Page 1/1"));

        // Info session avec date RÉELLE
        String date = jsonDataLoaderService.getSessionDate(day);

        html.append("<div class='session-info'>");
        html.append("<div>AU : 2024-2025 - Semestre : 2 - Session : Principale</div>");
        html.append("<div>Date : ").append(date);
        html.append(" - Séance : S").append(seance).append("</div>");
        html.append("</div>");

        // Tableau AVEC colonne signature
        html.append("<table class='schedule-table'>");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th>Enseignant</th>");
        html.append("<th class='center'>Salle</th>");
        html.append("<th class='signature-col center'>Signature</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        // Filtrer les enseignants pour cette séance
        List<TeacherAssignmentsDTO> filteredTeachers = allTeachers.stream()
                .filter(teacher -> teacher.getAssignments() != null && teacher.getAssignments().stream()
                        .anyMatch(a -> a.getDay().equals(day) && a.getSeance().equals(seance)))
                .sorted((t1, t2) -> t1.getTeacherName().compareTo(t2.getTeacherName()))
                .collect(Collectors.toList());

        if (filteredTeachers.isEmpty()) {
            html.append("<tr><td colspan='3' class='center'>Aucun enseignant pour cette séance</td></tr>");
        } else {
            for (TeacherAssignmentsDTO teacher : filteredTeachers) {
                TeacherAssignmentsDTO.TeacherAssignment assignment = teacher.getAssignments().stream()
                        .filter(a -> a.getDay().equals(day) && a.getSeance().equals(seance))
                        .findFirst()
                        .orElse(null);

                if (assignment != null) {
                    html.append("<tr>");
                    html.append("<td>").append(escapeHtml(teacher.getTeacherName())).append("</td>");
                    html.append("<td class='center'>").append(escapeHtml(assignment.getRoom())).append("</td>");
                    html.append("<td></td>");
                    html.append("</tr>");
                }
            }
        }

        html.append("</tbody>");
        html.append("</table>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Génère le HTML pour le planning enseignant RESPONSABLE (avec dates réelles)
     */
    private String generateResponsibleTeacherScheduleHTML(TeacherAssignmentsDTO teacher) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'/>");
        html.append("<style>");
        html.append(getCommonStyles());
        html.append("""
        .responsible-badge {
            background-color: #d4edda;
            color: #155724;
            padding: 8px 12px;
            border-radius: 4px;
            font-weight: bold;
            margin: 10px 0;
            text-align: center;
            border: 1px solid #c3e6cb;
        }
        .subject-col {
            width: 200px;
        }
        .students-col {
            width: 100px;
        }
    """);
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // En-tête
        html.append(generateHeader("EXD-FR-08-01", "0504-24", "Page 1/1"));

        // Section Notes avec badge RESPONSABLE
        html.append("<div class='notes-section'>");
        html.append("<div class='notes-label'>Notes à</div>");
        html.append("<div class='teacher-name'>");
        html.append(teacher.getGrade() != null ? teacher.getGrade() + " " : "Mr/Mme ");
        html.append(escapeHtml(teacher.getTeacherName()));
        html.append("</div>");
        html.append("<div class='responsible-badge'>📋 ENSEIGNANT RESPONSABLE</div>");
        html.append("</div>");

        // Message spécifique pour responsable
        html.append("<div class='message'>");
        html.append("Cher (e) Collègue,<br/>");
        html.append("Vous êtes prié (e) d'assurer la <strong>RESPONSABILITÉ</strong> des examens suivants selon le calendrier ci-joint.");
        html.append("</div>");

        // Tableau avec colonnes supplémentaires pour responsable SANS signature
        html.append("<table class='schedule-table'>");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th class='date-col'>Date</th>");
        html.append("<th class='heure-col center'>Heure</th>");
        html.append("<th class='duree-col center'>Durée</th>");
        html.append("<th class='subject-col center'>Matière</th>");
        html.append("<th class='room-col center'>Salle</th>");
        html.append("<th class='students-col center'>Surveillants</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        // Vérifier si la liste d'assignments n'est pas null
        if (teacher.getAssignments() != null && !teacher.getAssignments().isEmpty()) {
            // Utiliser un Set pour éliminer les doublons basés sur date, heure et durée
            Set<String> uniqueAssignments = new HashSet<>();
            List<TeacherAssignmentsDTO.TeacherAssignment> uniqueSortedAssignments = new ArrayList<>();

            for (TeacherAssignmentsDTO.TeacherAssignment assignment : teacher.getAssignments()) {
                String date = assignment.getExamDate() != null ? assignment.getExamDate() : "Jour " + assignment.getDay();
                String timeSlot = getRealTimeSlot(assignment);
                String duration = getDuration(assignment);

                String assignmentKey = date + "|" + timeSlot + "|" + duration;

                if (!uniqueAssignments.contains(assignmentKey)) {
                    uniqueAssignments.add(assignmentKey);
                    uniqueSortedAssignments.add(assignment);
                }
            }

            // Trier les affectations uniques par date puis par séance
            List<TeacherAssignmentsDTO.TeacherAssignment> sortedAssignments = uniqueSortedAssignments.stream()
                    .sorted((a1, a2) -> {
                        int dateCompare = a1.getExamDate().compareTo(a2.getExamDate());
                        if (dateCompare != 0) return dateCompare;
                        return a1.getSeance().compareTo(a2.getSeance());
                    })
                    .collect(Collectors.toList());

            for (TeacherAssignmentsDTO.TeacherAssignment assignment : sortedAssignments) {
                html.append("<tr>");
                // Date RÉELLE depuis examDate
                String date = assignment.getExamDate() != null ? assignment.getExamDate() : "Jour " + assignment.getDay();
                html.append("<td>").append(escapeHtml(date)).append("</td>");
                // Heure RÉELLE depuis startTime/endTime
                String timeSlot = getRealTimeSlot(assignment);
                html.append("<td class='center'>").append(timeSlot).append("</td>");
                // Durée calculée
                html.append("<td class='center'>").append(getDuration(assignment)).append("</td>");
                // Matière (à adapter selon vos données)
                html.append("<td class='center'>Examen</td>");
                // Salle
                html.append("<td class='center'>").append(escapeHtml(assignment.getRoom())).append("</td>");
                // Nombre de surveillants (à adapter)
                html.append("<td class='center'>2</td>");
                html.append("</tr>");
            }
        } else {
            html.append("<tr><td colspan='6' class='center'>Aucune responsabilité d'examen</td></tr>");
        }

        html.append("</tbody>");
        html.append("</table>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Retourne le créneau horaire réel depuis startTime/endTime
     */
    private String getRealTimeSlot(TeacherAssignmentsDTO.TeacherAssignment assignment) {
        if (assignment.getStartTime() != null && assignment.getEndTime() != null) {
            try {
                // Format "08:30:00" - on prend les 5 premiers caractères "08:30"
                String start = assignment.getStartTime().length() >= 5 ?
                        assignment.getStartTime().substring(0, 5) : assignment.getStartTime();
                String end = assignment.getEndTime().length() >= 5 ?
                        assignment.getEndTime().substring(0, 5) : assignment.getEndTime();
                return start + "-" + end;
            } catch (Exception e) {
                logger.warn("Erreur format heure, utilisation séance par défaut: {}", e.getMessage());
            }
        }
        // Fallback sur les séances si pas de time réel
        return getTimeSlot(assignment.getSeance());
    }

    /**
     * Calcule la durée depuis startTime/endTime
     */
    private String getDuration(TeacherAssignmentsDTO.TeacherAssignment assignment) {
        if (assignment.getStartTime() != null && assignment.getEndTime() != null) {
            try {
                // Format "08:30:00" - on prend les 5 premiers caractères "08:30"
                String start = assignment.getStartTime().length() >= 5 ?
                        assignment.getStartTime().substring(0, 5) : assignment.getStartTime();
                String end = assignment.getEndTime().length() >= 5 ?
                        assignment.getEndTime().substring(0, 5) : assignment.getEndTime();

                // Extraire heures et minutes
                int startHour = Integer.parseInt(start.substring(0, 2));
                int startMin = Integer.parseInt(start.substring(3, 5));
                int endHour = Integer.parseInt(end.substring(0, 2));
                int endMin = Integer.parseInt(end.substring(3, 5));

                int totalMinutes = (endHour * 60 + endMin) - (startHour * 60 + startMin);
                int hours = totalMinutes / 60;
                int minutes = totalMinutes % 60;

                if (minutes == 0) {
                    return hours + " H";
                } else {
                    return hours + " H " + minutes + " min";
                }
            } catch (Exception e) {
                logger.warn("Erreur calcul durée, utilisation valeur par défaut: {}", e.getMessage());
            }
        }
        return "1.5 H"; // Valeur par défaut
    }

    private String getTimeSlot(Integer seance) {
        switch (seance) {
            case 1: return "08:30-10:00";
            case 2: return "10:15-11:45";
            case 3: return "12:00-13:30";
            case 4: return "13:45-15:15";
            default: return "N/A";
        }
    }

    private String generateHeader(String code, String approvalDate, String pageNumber) {
        StringBuilder html = new StringBuilder();

        html.append("<table class='header-table'>");
        html.append("<tr>");
        html.append("<td class='logo-cell'>");
        html.append("<img src='data:image/png;base64,").append(getLogoBase64()).append("' class='logo-image' alt='Logo ISI'/>");
        html.append("</td>");
        html.append("<td class='header-center'>");
        html.append("<div class='header-title'>GESTION DES EXAMENS ET DÉLIBÉRATIONS</div>");
        html.append("<div class='header-subtitle'>Procédure d'exécution des épreuves</div>");
        html.append("<div class='header-list-title'>Liste d'affectation des surveillants</div>");
        html.append("</td>");
        html.append("<td class='header-right'>");
        html.append("<div class='code-text'>").append(code).append("</div>");
        html.append("<div style='margin-top: 15px;'>Date d'approbation<br/>").append(approvalDate).append("</div>");
        html.append("<div class='page-number' style='margin-top: 15px;'>").append(pageNumber).append("</div>");
        html.append("</td>");
        html.append("</tr>");
        html.append("</table>");

        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String getCommonStyles() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: 'Calibri', 'Arial', sans-serif;
                font-size: 11pt;
                padding: 15px;
            }
            
            .header-table {
                width: 100%;
                border-collapse: collapse;
                border: 2px solid #000;
                margin-bottom: 25px;
            }
            
            .header-table td {
                border: 1px solid #000;
                padding: 12px;
                vertical-align: top;
            }
            
            .logo-cell {
                width: 140px;
                text-align: center;
                vertical-align: middle;
            }
            
            .logo-image {
                width: 110px;
                height: 75px;
                object-fit: contain;
                display: block;
                margin: 0 auto;
            }
            
            .header-center {
                text-align: center;
            }
            
            .header-title {
                font-size: 15pt;
                font-weight: bold;
                color: #003d7a;
                margin-bottom: 8px;
                text-transform: uppercase;
            }
            
            .header-subtitle {
                font-size: 12pt;
                color: #000;
                margin: 4px 0;
            }
            
            .header-list-title {
                font-size: 13pt;
                font-weight: bold;
                color: #003d7a;
                margin-top: 8px;
            }
            
            .header-right {
                width: 140px;
                text-align: right;
                font-size: 10pt;
            }
            
            .code-text {
                font-weight: bold;
                color: #000;
            }
            
            .page-number {
                font-weight: bold;
                color: #003d7a;
                font-size: 13pt;
            }
            
            .notes-section {
                text-align: center;
                margin: 35px 0 25px 0;
            }
            
            .notes-label {
                font-size: 15pt;
                color: #003d7a;
                margin-bottom: 18px;
            }
            
            .teacher-name {
                font-size: 18pt;
                font-weight: bold;
                color: #003d7a;
                margin-bottom: 25px;
            }
            
            .message {
                text-align: left;
                font-size: 11pt;
                line-height: 1.5;
                color: #000;
                margin-bottom: 25px;
            }
            
            .session-info {
                text-align: center;
                margin: 25px 0;
                font-size: 13pt;
                font-weight: bold;
                color: #003d7a;
                line-height: 1.7;
            }
            
            .schedule-table {
                width: 100%;
                border-collapse: collapse;
                margin: 18px 0;
            }  
            
            .schedule-table thead {
                background-color: #0047AB;
                color: white;
            }
            
            .schedule-table th {
                padding: 10px 8px;
                text-align: left;
                font-weight: bold;
                border: 1px solid #003380;
                font-size: 11pt;
                background-color: #0047AB;
                color: white;
            }
            
            .schedule-table th.center {
                text-align: center;
            }
            
            .schedule-table td {
                padding: 8px;
                border: 1px solid #0047AB;
                font-size: 10pt;
                text-align: left;
            }
            
            .schedule-table td.center {
                text-align: center;
            }
            
            .schedule-table tbody tr:nth-child(even) {
                background-color: #f8f9fa;
            }
            
            .signature-col {
                width: 140px;
            }
            
            .date-col {
                width: 110px;
            }
            
            .heure-col {
                width: 110px;
            }
            
            .duree-col {
                width: 90px;
            }
        """;
    }

    private ByteArrayOutputStream convertHTMLToPDF(String html) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            ConverterProperties converterProperties = new ConverterProperties();
            HtmlConverter.convertToPdf(html, outputStream, converterProperties);

            logger.info("PDF généré avec succès");
            return outputStream;

        } catch (Exception e) {
            logger.error("Erreur lors de la génération du PDF", e);
            throw new RuntimeException("Erreur lors de la génération du PDF: " + e.getMessage(), e);
        }
    }

    private String getLogoBase64() {
        try {
            File logoFile = new File("data/logo-isi.png");
            if (logoFile.exists()) {
                byte[] logoBytes = Files.readAllBytes(logoFile.toPath());
                return java.util.Base64.getEncoder().encodeToString(logoBytes);
            } else {
                logger.warn("Logo non trouvé à: {}", logoFile.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("Erreur lors du chargement du logo: {}", e.getMessage());
        }

        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    }
}