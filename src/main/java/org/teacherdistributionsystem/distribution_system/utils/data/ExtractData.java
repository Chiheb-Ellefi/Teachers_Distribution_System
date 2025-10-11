package org.teacherdistributionsystem.distribution_system.utils.data;


import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.teacherdistributionsystem.distribution_system.entities.assignment.*;
import org.teacherdistributionsystem.distribution_system.entities.teacher.*;
import org.teacherdistributionsystem.distribution_system.enums.*;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.ExamRepository;
import org.teacherdistributionsystem.distribution_system.repositories.assignement.ExamSessionRepository;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.*;


import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import java.util.function.Function;

@Component
public class ExtractData {

    private final TeacherRepository teacherRepository;
    private final GradeTypeRepository gradeTypeRepository;
    private final TeacherUnavailabilitRepository teacherUnavailabilitRepository;
    private final ExamSessionRepository examSessionRepository;
    private final TeacherQuotaRepository teacherQuotaRepository;
    private final TeacherPreferenceRepository teacherPreferenceRepository;
    private final ExamRepository examRepository;
    @Value("${spring.application.teachersListFile}")
    private  String teachersListFile;
    @Value("${spring.application.teachersUnavailabilyFile}")
    private  String teachersUnavailabilyFile;
    @Value("${spring.application.examDataFile}")
    private  String examDataFile;

    public ExtractData(TeacherRepository teacherRepository, GradeTypeRepository gradeTypeRepository, TeacherUnavailabilitRepository teacherUnavailabilitRepository, ExamSessionRepository examSessionRepository, TeacherQuotaRepository teacherQuotaRepository, TeacherPreferenceRepository teacherPreferenceRepository, ExamRepository examRepository) {
        this.teacherRepository = teacherRepository;
        this.gradeTypeRepository = gradeTypeRepository;
        this.teacherUnavailabilitRepository = teacherUnavailabilitRepository;
        this.examSessionRepository = examSessionRepository;
        this.teacherQuotaRepository = teacherQuotaRepository;
        this.teacherPreferenceRepository = teacherPreferenceRepository;
        this.examRepository = examRepository;
    }
    @PostConstruct
    @Order(0)
    private void populateExamSession() throws IOException {

        FileInputStream file = new FileInputStream(examDataFile);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);

        LocalDate startDate = null;
        LocalDate endDate = null;
        String academicYear ;
        String sessionLibelle = null;
        String semesterLibelle = null;

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;

            Function<Integer, String> getString = i -> {
                Cell cell = row.getCell(i);
                if (cell == null) return "";
                return switch (cell.getCellType()) {
                    case STRING -> cell.getStringCellValue().trim();
                    case NUMERIC -> {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                        } else {
                            yield String.valueOf((int) cell.getNumericCellValue());
                        }
                    }
                    case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                    default -> "";
                };
            };

            LocalDate examDate = getLocalDate(getString.apply(0));

            if (startDate == null || examDate.isBefore(startDate)) {
                startDate = examDate;
            }
            if (endDate == null || examDate.isAfter(endDate)) {
                endDate = examDate;
            }

            sessionLibelle = SessionType.valueOf(getString.apply(3)).getLabel();
            semesterLibelle = getString.apply(5);
        }

        if (endDate == null) {
            throw new IllegalStateException("No valid exam dates found in Excel file.");
        }

        academicYear = String.valueOf(endDate.getYear());
        int jourNumero = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;

        ExamSession session = ExamSession.builder()
                .startDate(startDate)
                .endDate(endDate)
                .sessionLibelle(sessionLibelle)
                .academicYear(academicYear)
                .semesterCode(SemesterType.fromLabel(semesterLibelle).getLabel())
                .numExamDays(jourNumero)
                .seancesPerDay(SeanceType.values().length)
                .semesterLibelle(semesterLibelle)
                .build();
        examSessionRepository.save(session);
    }


    @PostConstruct
    @Order(1)
 void populateTeachersTable() throws IOException {
        List<Teacher> teachers=new ArrayList<>();
        List<GradeType> grades=new ArrayList<>();
        List<TeacherQuota> teacherQuotas=new ArrayList<>();
        List<TeacherPreference> teacherPreferences=new ArrayList<>();
     FileInputStream file = new FileInputStream(teachersListFile);
     Workbook workbook = new XSSFWorkbook(file);
     Sheet sheet = workbook.getSheetAt(0);
     sheet.forEach(row -> {
         if (row.getRowNum() != 0) {

             Function<Integer, String> getString =  i -> {
                 Cell cell = row.getCell(i);
                 if (cell == null) return "";
                 return switch (cell.getCellType()) {
                     case STRING -> cell.getStringCellValue().trim();
                     case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
                     case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                     default -> "";
                 };
             };
             Function<Integer, Integer> getInteger = i -> {
                 Cell cell = row.getCell(i);
                 if (cell == null) return 0;
                 return cell.getCellType() == CellType.NUMERIC ? (int) cell.getNumericCellValue() : 0;
             };
             Function<Integer, Boolean> getBoolean = i -> {
                 Cell cell = row.getCell(i);
                 if (cell == null) return false;
                 return cell.getCellType() == CellType.BOOLEAN && cell.getBooleanCellValue();
             };
             //--------------find session -----------------
             ExamSession session=examSessionRepository.findById(1L).orElse(null);
             //--------Build teacher instance-----
             Teacher teacher = Teacher.builder()
                     .nom(getString.apply(0))
                     .prenom(getString.apply(1))
                     .email(getString.apply(2))
                     .gradeCode(getString.apply(3))
                     .codeSmartex(getInteger.apply(4))
                     .participeSurveillance(getBoolean.apply(5))
                     .build();
             teachers.add(teacher);
             //-----Build grade type instance------
             GradeType gradeType=GradeType.builder()
                     .gradeCode(getString.apply(3))
                     .gradeLibelle(GradeMap.fromCode(getString.apply(3)).getLabel())
                     .defaultQuotaPerSession(GradeMap.fromCode(getString.apply(3)).getDefaultQuota())
                     .priorityLevel(GradeMap.fromCode(getString.apply(3)).getPriority())
                     .build();
             grades.add(gradeType);
             //-------Build teacher quota instance -------

             TeacherQuota quota=TeacherQuota.builder()
                     .assignedQuota(GradeMap.fromCode(getString.apply(3)).getDefaultQuota())
                     .teacher(teacher)
                     .examSession(session)
                     .quotaType(QuotaType.STANDARD)
                     .reason("Standard pour le grade : " + getString.apply(3))
                     .build();
             teacherQuotas.add(quota);
             //--------Build Teacher preference instance ----------
             TeacherPreference teacherPreference=TeacherPreference.builder()
                     .examSession(session)
                     .teacher(teacher)
                     .preferenceType(PreferenceType.NOTHING)
                     .priorityWeight(0)
                     .build();
             teacherPreferences.add(teacherPreference);
         }

         teacherRepository.saveAll(teachers);
         gradeTypeRepository.saveAll(grades);
         teacherQuotaRepository.saveAll(teacherQuotas);
         teacherPreferenceRepository.saveAll(teacherPreferences);
     });

 }
//-----------------Session & TeacherUnavailibilty--------------------------
@PostConstruct
@Order(2)
 public void populateTeacherUnavailabilityTable() throws IOException {

        ExamSession session=examSessionRepository.findById(1L).orElse(null);
     List<TeacherUnavailability> teacherUnavailabilityList=new ArrayList<>();
     FileInputStream file = new FileInputStream(teachersUnavailabilyFile);
     Workbook workbook = new XSSFWorkbook(file);
     Sheet sheet = workbook.getSheetAt(0);
     sheet.forEach(row -> {
         if (row.getRowNum() != 0) {
             Function<Integer, String> getString = i -> {
                 Cell cell = row.getCell(i);
                 if (cell == null) return "";
                 return switch (cell.getCellType()) {
                     case STRING -> cell.getStringCellValue().trim();
                     case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
                     case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                     default -> "";
                 };
             };
             Function<Integer, Integer> getInteger = i -> {
                 Cell cell = row.getCell(i);
                 if (cell == null) return 0;
                 return cell.getCellType() == CellType.NUMERIC ? (int) cell.getNumericCellValue() : 0;
             };

             // -------------------------------------find teacher ------------------------
            Teacher teacher = teacherRepository.findByNomAndPrenom(getString.apply(2),getString.apply(3));
             TeacherUnavailability teacherUnavailability=TeacherUnavailability.builder()
                     .teacher(teacher)
                     .examSession(session)
                     .numeroJour(getInteger.apply(4))
                     .seance(getString.apply(5))
                     .build();
             teacherUnavailabilityList.add(teacherUnavailability);
         }

     });
    teacherUnavailabilitRepository.saveAll(teacherUnavailabilityList);
 }

 @Order(4)
    @PostConstruct
    public void populateExam() throws IOException {
        List<Exam> examList = new ArrayList<>();
        FileInputStream file = new FileInputStream(examDataFile);
             Workbook workbook = new XSSFWorkbook(file);

            Sheet sheet = workbook.getSheetAt(0);
            sheet.forEach(row -> {
                if (row.getRowNum() == 0) return; // skip header

                Function<Integer, String> getString = i -> {
                    Cell cell = row.getCell(i);
                    if (cell == null) return "";
                    return switch (cell.getCellType()) {
                        case STRING -> cell.getStringCellValue().trim();
                        case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
                        case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                        default -> "";
                    };
                };


                String codeStr = getString.apply(6);
                if (codeStr.isEmpty()) return;

                int codeSmartex;
                try {
                    codeSmartex = Integer.parseInt(codeStr);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid code in row " + row.getRowNum() + ": " + codeStr);
                    return;
                }

                ExamSession examSession = examSessionRepository.findById(1L).orElse(null);
                Teacher teacher = teacherRepository.findByCodeSmartex(codeSmartex);
                if (teacher == null) {
                    System.err.println("No teacher found for code: " + codeSmartex);
                    return;
                }

                LocalDate examDate=getLocalDate(getString.apply(0));
                LocalTime startTime=getLocalTime(getString.apply(1));
                LocalTime endTime=getLocalTime(getString.apply(2));
                assert examSession != null;
                int jourNumero = (int)ChronoUnit.DAYS.between(LocalDate.of(2025,5,13),examDate)+1;
                Exam exam = Exam.builder()
                        .examDate(examDate)
                        .startTime(startTime)
                        .endTime(endTime)
                        .examSession(examSession)
                        .examType(getString.apply(4))
                        .responsable(teacher)
                        .numRooms(getString.apply(7))
                        .requiredSupervisors(2)
                        .seance(SeanceType.fromTime(startTime))
                        .jourNumero(jourNumero)
                        .build();

                examList.add(exam);
            });

            examRepository.saveAll(examList);

    }

    private LocalTime getLocalTime(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);
        return dateTime.toLocalTime();

    }
    private LocalDate getLocalDate(String dateTimeStr) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return LocalDate.parse(dateTimeStr.trim(), formatter);

    }




}
