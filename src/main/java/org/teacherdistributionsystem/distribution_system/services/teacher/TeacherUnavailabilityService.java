package org.teacherdistributionsystem.distribution_system.services.teacher;


import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.entities.teacher.TeacherUnavailability;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilitiesProjection;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilityProjection;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherRepository;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.TeacherUnavailabilityRepository;
import org.teacherdistributionsystem.distribution_system.models.keys.TeacherKey;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.teacherdistributionsystem.distribution_system.utils.ExcelCellUtils.getCellAsString;

@Service
@RequiredArgsConstructor
public class TeacherUnavailabilityService {
    private final TeacherUnavailabilityRepository teacherUnavailabilitRepository;
    private final TeacherRepository teacherRepository;

    private static final Map<String, DayOfWeek> FRENCH_DAYS = Map.of(
            "lundi", DayOfWeek.MONDAY,
            "mardi", DayOfWeek.TUESDAY,
            "mercredi", DayOfWeek.WEDNESDAY,
            "jeudi", DayOfWeek.THURSDAY,
            "vendredi", DayOfWeek.FRIDAY,
            "samedi", DayOfWeek.SATURDAY,
            "dimanche", DayOfWeek.SUNDAY
    );
    public void addTeachersUnavailability(
            Workbook workbook,
            ExamSession session,
            Map<String, String> abrvToEmailMap) {

        // Build email-to-teacher map
        Map<String, Teacher> emailToTeacherMap = teacherRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Teacher::getEmail,
                        t -> t,
                        (existing, duplicate) -> existing
                ));

        List<TeacherUnavailability> teacherUnavailabilityList =
                getTeacherUnavailabilities(workbook, session, abrvToEmailMap, emailToTeacherMap);

        teacherUnavailabilitRepository.saveAll(teacherUnavailabilityList);
    }

    private List<TeacherUnavailability> getTeacherUnavailabilities(
            Workbook workbook,
            ExamSession session,
            Map<String, String> abrvToEmailMap,
            Map<String, Teacher> emailToTeacherMap) {

        List<TeacherUnavailability> teacherUnavailabilityList = new ArrayList<>();
        LocalDate examStartDate = session.getStartDate();

        workbook.forEach(sheet -> {
            sheet.forEach(row -> {
                if (row.getRowNum() == 0) return;

                // Column 0: Enseignant (abbreviation format like "N.BEN HARIZ")
                String teacherAbrv = getCellAsString(row, 0);
                if (teacherAbrv == null || teacherAbrv.trim().isEmpty()) return;

                teacherAbrv = teacherAbrv.trim();

                // Look up teacher email using abbreviation
                String email = abrvToEmailMap.get(teacherAbrv);
                if (email == null) {
                    System.err.println("No email mapping found for abbreviation: " + teacherAbrv);
                    return;
                }

                // Get teacher using email
                Teacher teacher = emailToTeacherMap.get(email);
                if (teacher == null) {
                    System.err.println("Teacher not found for email: " + email + " (abbreviation: " + teacherAbrv + ")");
                    return;
                }

                // Column 3: Jour (day name like "Mardi")
                String jourName = getCellAsString(row, 3);
                if (jourName == null || jourName.trim().isEmpty()) return;

                Integer numeroJour = calculateDayNumber(jourName.toLowerCase().trim(), examStartDate);
                if (numeroJour == null) {
                    System.err.println("Unable to calculate day number for: " + jourName);
                    return;
                }

                // Column 4: Séances (comma-separated like "S1,S2,S3,S4")
                String seancesStr = getCellAsString(row, 4);
                if (seancesStr == null || seancesStr.trim().isEmpty()) return;

                List<String> seances = parseSeances(seancesStr);

                // Create unavailability entry for each seance
                for (String seance : seances) {
                    TeacherUnavailability unavailability = TeacherUnavailability.builder()
                            .teacher(teacher)
                            .examSession(session)
                            .numeroJour(numeroJour)
                            .seance(seance)
                            .build();

                    teacherUnavailabilityList.add(unavailability);
                }
            });
        });

        return teacherUnavailabilityList;
    }

    /**
     * Calculate the day number in the exam week based on the French day name
     * and the exam start date
     */
    private Integer calculateDayNumber(String jourName, LocalDate examStartDate) {
        DayOfWeek targetDay = FRENCH_DAYS.get(jourName);
        if (targetDay == null) {
            // Try to handle variations
            for (Map.Entry<String, DayOfWeek> entry : FRENCH_DAYS.entrySet()) {
                if (jourName.startsWith(entry.getKey())) {
                    targetDay = entry.getValue();
                    break;
                }
            }
            if (targetDay == null) return null;
        }

        // Find the first occurrence of targetDay on or after examStartDate
        LocalDate examDay = examStartDate;
        while (examDay.getDayOfWeek() != targetDay) {
            examDay = examDay.plusDays(1);
        }

        // Calculate day number (1-based) from start date
        long daysDifference = ChronoUnit.DAYS.between(examStartDate, examDay);
        return (int) daysDifference + 1;
    }

    /**
     * Parse séances string into individual seance codes
     * Example: "S1,S2,S3,S4" -> ["S1", "S2", "S3", "S4"]
     */
    private List<String> parseSeances(String seancesStr) {
        List<String> seances = new ArrayList<>();
        String[] parts = seancesStr.split(",");

        for (String part : parts) {
            String seance = part.trim();
            if (!seance.isEmpty()) {
                seances.add(seance);
            }
        }

        return seances;
    }
    public List<TeacherUnavailabilityProjection> getTeacherUnavailabilitiesBySessionId(Long sessionId) {
       return teacherUnavailabilitRepository.getTeacherUnavailableBySessionId(sessionId);
    }
    public List<TeacherUnavailabilitiesProjection> getTeacherUnavailabilityListBySessionId(Long sessionId) {
        return teacherUnavailabilitRepository.findAllByExamSessionId(sessionId);
    }
    public void cleanUp(){
        teacherUnavailabilitRepository.deleteAll();
    }
}
