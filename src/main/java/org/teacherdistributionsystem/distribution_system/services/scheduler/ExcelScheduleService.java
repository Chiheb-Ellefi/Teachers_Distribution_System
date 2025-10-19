package org.teacherdistributionsystem.distribution_system.services.scheduler;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.TeacherAssignmentsDTO;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelScheduleService.class);
    private final JsonDataLoaderService jsonDataLoaderService;

    public ExcelScheduleService(JsonDataLoaderService jsonDataLoaderService) {
        this.jsonDataLoaderService = jsonDataLoaderService;
    }

    /**
     * 1. Génère le planning par séance en Excel (tous les enseignants pour une séance)
     */
    public ByteArrayOutputStream generateSessionSchedule(Integer day, Integer seance) throws IOException {
        logger.info("Génération Excel planning séance - Jour: {}, Séance: {}", day, seance);

        List<TeacherAssignmentsDTO> allTeachers = jsonDataLoaderService.loadTeacherAssignments();

        // Filtrer les enseignants pour cette séance
        List<TeacherAssignmentsDTO> filteredTeachers = allTeachers.stream()
                .filter(teacher -> teacher.getAssignments() != null && teacher.getAssignments().stream()
                        .anyMatch(a -> a.getDay().equals(day) && a.getSeance().equals(seance)))
                .sorted((t1, t2) -> t1.getTeacherName().compareTo(t2.getTeacherName()))
                .collect(Collectors.toList());

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Séance J" + day + " S" + seance);

        // Styles
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createBlueHeaderStyle(workbook);
        CellStyle dataStyle = createBorderStyle(workbook);
        CellStyle infoStyle = createInfoStyle(workbook);

        int rowNum = 0;

        // Titre principal
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("LISTE D'AFFECTATION DES SURVEILLANTS - SÉANCE");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        titleRow.setHeightInPoints(30);

        // Informations de la séance avec date RÉELLE
        String date = jsonDataLoaderService.getSessionDate(day);
        Row infoRow = sheet.createRow(rowNum++);
        Cell infoCell = infoRow.createCell(0);
        infoCell.setCellValue("AU : 2024-2025 - Semestre : 2 - Session : Principale - Date : " + date + " - Séance : S" + seance);
        infoCell.setCellStyle(infoStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));

        rowNum++; // Ligne vide

        // En-têtes du tableau AVEC colonne signature
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Enseignant", "Salle", "Signature"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données des enseignants
        if (filteredTeachers.isEmpty()) {
            Row emptyRow = sheet.createRow(rowNum++);
            Cell emptyCell = emptyRow.createCell(0);
            emptyCell.setCellValue("Aucun enseignant pour cette séance");
            emptyCell.setCellStyle(dataStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 2));
        } else {
            for (TeacherAssignmentsDTO teacher : filteredTeachers) {
                TeacherAssignmentsDTO.TeacherAssignment assignment = teacher.getAssignments().stream()
                        .filter(a -> a.getDay().equals(day) && a.getSeance().equals(seance))
                        .findFirst()
                        .orElse(null);

                if (assignment != null) {
                    Row dataRow = sheet.createRow(rowNum++);
                    dataRow.setHeightInPoints(25);

                    // Enseignant
                    Cell nameCell = dataRow.createCell(0);
                    nameCell.setCellValue(teacher.getTeacherName());
                    nameCell.setCellStyle(dataStyle);

                    // Salle
                    Cell roomCell = dataRow.createCell(1);
                    roomCell.setCellValue(assignment.getRoom());
                    roomCell.setCellStyle(dataStyle);

                    // Signature (vide)
                    Cell signatureCell = dataRow.createCell(2);
                    signatureCell.setCellValue("");
                    signatureCell.setCellStyle(dataStyle);
                }
            }
        }

        // Ajuster les largeurs de colonnes
        sheet.setColumnWidth(0, 12000);  // Enseignant
        sheet.setColumnWidth(1, 6000);   // Salle
        sheet.setColumnWidth(2, 8000);   // Signature

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        logger.info("Excel planning séance généré avec succès");
        return outputStream;
    }

    /**
     * 2. Génère le planning surveillant en Excel (SANS colonne signature)
     */
    public ByteArrayOutputStream generateTeacherSchedule(String teacherName) throws IOException {
        logger.info("Génération Excel planning surveillant pour: {}", teacherName);

        TeacherAssignmentsDTO teacherData = jsonDataLoaderService.getTeacherDataByName(teacherName);

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Planning " + teacherName);

        // Styles
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle yellowHeaderStyle = createYellowHeaderStyle(workbook);
        CellStyle blueHeaderStyle = createBlueHeaderStyle(workbook);
        CellStyle dataStyle = createBorderStyle(workbook);
        CellStyle statsStyle = createStatsStyle(workbook);

        int rowNum = 0;

        // En-tête principal
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("PLANNING DE SURVEILLANCE DES EXAMENS");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3)); // 4 colonnes
        titleRow.setHeightInPoints(30);

        // Informations de l'enseignant
        Row teacherRow = sheet.createRow(rowNum++);
        Cell teacherCell = teacherRow.createCell(0);
        teacherCell.setCellValue("Enseignant : " + teacherData.getTeacherName() +
                (teacherData.getGrade() != null ? " | Grade : " + teacherData.getGrade() : "") +
                (teacherData.getEmail() != null ? " | Email : " + teacherData.getEmail() : ""));
        teacherCell.setCellStyle(yellowHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

        // Statistiques
        Row statsRow = sheet.createRow(rowNum++);
        Cell statsCell = statsRow.createCell(0);
        statsCell.setCellValue(String.format("Affectations: %d/%d (%.1f%%)",
                teacherData.getAssignedSupervisions(),
                teacherData.getQuotaSupervisions(),
                teacherData.getUtilizationPercentage()));
        statsCell.setCellStyle(statsStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 3));

        rowNum++; // Ligne vide

        // En-têtes du tableau SANS colonne signature
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Date", "Heure", "Durée", "Salle"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(blueHeaderStyle);
        }

        // Données des affectations avec dates RÉELLES
        if (teacherData.getAssignments() != null && !teacherData.getAssignments().isEmpty()) {
            // Trier les affectations par date puis par séance
            List<TeacherAssignmentsDTO.TeacherAssignment> sortedAssignments = teacherData.getAssignments().stream()
                    .sorted((a1, a2) -> {
                        int dateCompare = a1.getExamDate().compareTo(a2.getExamDate());
                        if (dateCompare != 0) return dateCompare;
                        return a1.getSeance().compareTo(a2.getSeance());
                    })
                    .collect(Collectors.toList());

            for (TeacherAssignmentsDTO.TeacherAssignment assignment : sortedAssignments) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.setHeightInPoints(25);

                // Date RÉELLE
                Cell dateCell = dataRow.createCell(0);
                String date = assignment.getExamDate() != null ? assignment.getExamDate() : "Jour " + assignment.getDay();
                dateCell.setCellValue(date);
                dateCell.setCellStyle(dataStyle);

                // Heure RÉELLE
                Cell timeCell = dataRow.createCell(1);
                String timeSlot = getRealTimeSlot(assignment);
                timeCell.setCellValue(timeSlot);
                timeCell.setCellStyle(dataStyle);

                // Durée calculée
                Cell durationCell = dataRow.createCell(2);
                durationCell.setCellValue(getDuration(assignment));
                durationCell.setCellStyle(dataStyle);

                // Salle
                Cell roomCell = dataRow.createCell(3);
                roomCell.setCellValue(assignment.getRoom());
                roomCell.setCellStyle(dataStyle);
            }
        } else {
            Row emptyRow = sheet.createRow(rowNum++);
            Cell emptyCell = emptyRow.createCell(0);
            emptyCell.setCellValue("Aucune affectation de surveillance");
            emptyCell.setCellStyle(dataStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 3));
        }

        // Footer
        rowNum += 2;
        Row footerRow = sheet.createRow(rowNum++);
        Cell footerCell = footerRow.createCell(0);
        footerCell.setCellValue("Cher(e) Collègue,\nVous êtes prié(e) d'assurer la surveillance des examens selon le calendrier ci-dessus.");
        footerCell.setCellStyle(createMessageStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 3));

        // Ajuster les largeurs de colonnes
        sheet.setColumnWidth(0, 4000);  // Date
        sheet.setColumnWidth(1, 5000);  // Heure
        sheet.setColumnWidth(2, 4000);  // Durée
        sheet.setColumnWidth(3, 4000);  // Salle

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        logger.info("Excel planning surveillant généré avec succès pour: {}", teacherName);
        return outputStream;
    }

    /**
     * 3. Génère le planning responsable en Excel (SANS colonne signature)
     */
    public ByteArrayOutputStream generateResponsibleTeacherSchedule(String teacherName) throws IOException {
        logger.info("Génération Excel planning responsable pour: {}", teacherName);

        TeacherAssignmentsDTO teacherData = jsonDataLoaderService.getResponsibleTeacherDataByName(teacherName);

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Planning Responsable " + teacherName);

        // Styles
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle responsibleHeaderStyle = createResponsibleHeaderStyle(workbook);
        CellStyle blueHeaderStyle = createBlueHeaderStyle(workbook);
        CellStyle dataStyle = createBorderStyle(workbook);
        CellStyle messageStyle = createMessageStyle(workbook);

        int rowNum = 0;

        // En-tête principal
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("PLANNING DE RESPONSABILITÉ DES EXAMENS");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5)); // 6 colonnes
        titleRow.setHeightInPoints(30);

        // Informations de l'enseignant responsable
        Row teacherRow = sheet.createRow(rowNum++);
        Cell teacherCell = teacherRow.createCell(0);
        teacherCell.setCellValue("ENSEIGNANT RESPONSABLE : " + teacherData.getTeacherName() +
                (teacherData.getGrade() != null ? " | Grade : " + teacherData.getGrade() : ""));
        teacherCell.setCellStyle(responsibleHeaderStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        rowNum++; // Ligne vide

        // Message spécifique pour responsable
        Row messageRow = sheet.createRow(rowNum++);
        Cell messageCell = messageRow.createCell(0);
        messageCell.setCellValue("Cher(e) Collègue,\nVous êtes prié(e) d'assurer la RESPONSABILITÉ des examens suivants selon le calendrier ci-dessous.");
        messageCell.setCellStyle(messageStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 5));

        rowNum++; // Ligne vide

        // En-têtes du tableau SANS colonne signature
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Date", "Heure", "Durée", "Matière", "Salle", "Surveillants"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(blueHeaderStyle);
        }

        // Données des affectations responsables avec dates RÉELLES
        if (teacherData.getAssignments() != null && !teacherData.getAssignments().isEmpty()) {
            // Trier les affectations par date puis par séance
            List<TeacherAssignmentsDTO.TeacherAssignment> sortedAssignments = teacherData.getAssignments().stream()
                    .sorted((a1, a2) -> {
                        int dateCompare = a1.getExamDate().compareTo(a2.getExamDate());
                        if (dateCompare != 0) return dateCompare;
                        return a1.getSeance().compareTo(a2.getSeance());
                    })
                    .collect(Collectors.toList());

            for (TeacherAssignmentsDTO.TeacherAssignment assignment : sortedAssignments) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.setHeightInPoints(25);

                // Date RÉELLE
                Cell dateCell = dataRow.createCell(0);
                String date = assignment.getExamDate() != null ? assignment.getExamDate() : "Jour " + assignment.getDay();
                dateCell.setCellValue(date);
                dateCell.setCellStyle(dataStyle);

                // Heure RÉELLE
                Cell timeCell = dataRow.createCell(1);
                String timeSlot = getRealTimeSlot(assignment);
                timeCell.setCellValue(timeSlot);
                timeCell.setCellStyle(dataStyle);

                // Durée calculée
                Cell durationCell = dataRow.createCell(2);
                durationCell.setCellValue(getDuration(assignment));
                durationCell.setCellStyle(dataStyle);

                // Matière
                Cell subjectCell = dataRow.createCell(3);
                subjectCell.setCellValue("Examen"); // À adapter selon vos données
                subjectCell.setCellStyle(dataStyle);

                // Salle
                Cell roomCell = dataRow.createCell(4);
                roomCell.setCellValue(assignment.getRoom());
                roomCell.setCellStyle(dataStyle);

                // Surveillants
                Cell supervisorsCell = dataRow.createCell(5);
                supervisorsCell.setCellValue("2"); // À adapter selon vos données
                supervisorsCell.setCellStyle(dataStyle);
            }
        } else {
            Row emptyRow = sheet.createRow(rowNum++);
            Cell emptyCell = emptyRow.createCell(0);
            emptyCell.setCellValue("Aucune responsabilité d'examen");
            emptyCell.setCellStyle(dataStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 5));
        }

        // Ajuster les largeurs de colonnes
        sheet.setColumnWidth(0, 4000);  // Date
        sheet.setColumnWidth(1, 5000);  // Heure
        sheet.setColumnWidth(2, 4000);  // Durée
        sheet.setColumnWidth(3, 6000);  // Matière
        sheet.setColumnWidth(4, 4000);  // Salle
        sheet.setColumnWidth(5, 4000);  // Surveillants

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        logger.info("Excel planning responsable généré avec succès pour: {}", teacherName);
        return outputStream;
    }

    /**
     * 4. Génère le planning administration en Excel (tous les enseignants)
     */
    public ByteArrayOutputStream generateAdminSchedule() throws IOException {
        logger.info("Génération du planning administration Excel");

        List<TeacherAssignmentsDTO> allTeachers = jsonDataLoaderService.getAllTeachersWithData();
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Planning Global");

        // Styles
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createBlueHeaderStyle(workbook);
        CellStyle dataStyle = createBorderStyle(workbook);
        CellStyle statsStyle = createStatsStyle(workbook);

        int rowNum = 0;

        // Titre principal
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("PLANNING GLOBAL DE SURVEILLANCE DES EXAMENS");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        titleRow.setHeightInPoints(30);

        // Statistiques globales
        Row statsRow = sheet.createRow(rowNum++);
        Cell statsCell = statsRow.createCell(0);
        long totalAssignments = allTeachers.stream()
                .mapToLong(teacher -> teacher.getAssignments() != null ? teacher.getAssignments().size() : 0)
                .sum();
        long teachersWithAssignments = allTeachers.stream()
                .filter(teacher -> teacher.getAssignments() != null && !teacher.getAssignments().isEmpty())
                .count();

        statsCell.setCellValue(String.format("Total: %d enseignants, %d avec affectations, %d surveillances totales",
                allTeachers.size(), teachersWithAssignments, totalAssignments));
        statsCell.setCellStyle(statsStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        rowNum++; // Ligne vide

        // En-têtes du tableau
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Enseignant", "Grade", "Email", "Affectations", "Quota", "Taux Utilisation", "Détail Affectations"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données des enseignants
        for (TeacherAssignmentsDTO teacher : allTeachers) {
            Row dataRow = sheet.createRow(rowNum++);
            dataRow.setHeightInPoints(25);

            // Enseignant
            Cell nameCell = dataRow.createCell(0);
            nameCell.setCellValue(teacher.getTeacherName() != null ? teacher.getTeacherName() : "N/A");
            nameCell.setCellStyle(dataStyle);

            // Grade ✅ CORRECTION
            Cell gradeCell = dataRow.createCell(1);
            gradeCell.setCellValue(teacher.getGrade() != null ? teacher.getGrade() : "N/A");
            gradeCell.setCellStyle(dataStyle);

            // Email ✅ CORRECTION
            Cell emailCell = dataRow.createCell(2);
            emailCell.setCellValue(teacher.getEmail() != null ? teacher.getEmail() : "N/A");
            emailCell.setCellStyle(dataStyle);

            // Affectations
            Cell assignedCell = dataRow.createCell(3);
            assignedCell.setCellValue(teacher.getAssignedSupervisions() != null ? teacher.getAssignedSupervisions() : 0);
            assignedCell.setCellStyle(dataStyle);

            // Quota ✅ CORRECTION - Utiliser le quota réel
            Cell quotaCell = dataRow.createCell(4);
            quotaCell.setCellValue(teacher.getQuotaSupervisions() != null ? teacher.getQuotaSupervisions() : 0);
            quotaCell.setCellStyle(dataStyle);

            // Taux utilisation
            Cell usageCell = dataRow.createCell(5);
            Double utilization = teacher.getUtilizationPercentage();
            String utilizationText = utilization != null ? String.format("%.1f%%", utilization) : "0%";
            usageCell.setCellValue(utilizationText);
            usageCell.setCellStyle(dataStyle);

            // Détail des affectations avec dates RÉELLES
            Cell detailCell = dataRow.createCell(6);
            StringBuilder details = new StringBuilder();
            if (teacher.getAssignments() != null && !teacher.getAssignments().isEmpty()) {
                for (TeacherAssignmentsDTO.TeacherAssignment assignment : teacher.getAssignments()) {
                    String date = assignment.getExamDate() != null ? assignment.getExamDate() : "Jour " + assignment.getDay();
                    String seanceLabel = assignment.getSeanceLabel() != null ? assignment.getSeanceLabel() : "S" + assignment.getSeance();
                    String room = assignment.getRoom() != null ? assignment.getRoom() : "N/A";

                    details.append(date)
                            .append(" - ")
                            .append(seanceLabel)
                            .append(" (")
                            .append(room)
                            .append("); ");
                }
            } else {
                details.append("Aucune affectation");
            }
            detailCell.setCellValue(details.toString());
            detailCell.setCellStyle(dataStyle);
        }

        // Ajuster les largeurs de colonnes
        sheet.setColumnWidth(0, 8000);  // Enseignant
        sheet.setColumnWidth(1, 4000);  // Grade
        sheet.setColumnWidth(2, 8000);  // Email
        sheet.setColumnWidth(3, 4000);  // Affectations
        sheet.setColumnWidth(4, 4000);  // Quota
        sheet.setColumnWidth(5, 4000);  // Taux
        sheet.setColumnWidth(6, 15000); // Détails

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        logger.info("Planning administration Excel généré avec succès");
        return outputStream;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

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

    // ==================== MÉTHODES DE CRÉATION DE STYLES ====================

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBlueHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createYellowHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createResponsibleHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBorderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createStatsStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createInfoStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createMessageStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }
}