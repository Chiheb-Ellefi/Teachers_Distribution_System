package org.teacherdistributionsystem.distribution_system.services;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamService;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamSessionService;
import org.teacherdistributionsystem.distribution_system.services.teachers.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class ExcelImportOrchestrator {

    private final TeacherService teacherService;
    private final TeacherQuotaService quotaService;
    private final GradeService gradeService;
    private final TeacherPreferenceService preferenceService;
    private final ExamSessionService examSessionService;
    private final ExamService examService;
    private final TeacherUnavailabilityService teacherUnavailabilityService;

    ExcelImportOrchestrator(TeacherService teacherService, TeacherQuotaService quotaService, GradeService gradeService,
                            TeacherPreferenceService preferenceService, ExamSessionService examSessionService, ExamService examService, TeacherUnavailabilityService teacherUnavailabilityService) {
        this.teacherService = teacherService;
        this.quotaService = quotaService;
        this.gradeService = gradeService;
        this.preferenceService = preferenceService;
        this.examSessionService = examSessionService;
        this.examService = examService;
        this.teacherUnavailabilityService = teacherUnavailabilityService;
    }
    @Transactional
    public void importData(String examDataFilePath, String teachersListFilePath, String teachersUnavailabilityFilePath) throws IOException {
        ExamSession examSession;
        try (FileInputStream examDataFile = new FileInputStream(examDataFilePath);
             Workbook examWorkbook = new XSSFWorkbook(examDataFile)) {

            examSession = examSessionService.addSession(examWorkbook);
        }
        Map<String, Teacher> teacherMap;
        try (FileInputStream teachersListFile = new FileInputStream(teachersListFilePath);
             Workbook teachersWorkbook = new XSSFWorkbook(teachersListFile)) {

            teacherMap = teacherService.populateTeachersTable(teachersWorkbook);
            gradeService.addGrades(teachersWorkbook);
            quotaService.addTeachersQuota(teachersWorkbook, teacherMap, examSession);
            preferenceService.addTeachersPreference(teachersWorkbook, teacherMap, examSession);
        }
        try (FileInputStream examDataFile = new FileInputStream(examDataFilePath);
             Workbook examWorkbook = new XSSFWorkbook(examDataFile)) {
            examService.addExams(examWorkbook, examSession);
        }

        try (FileInputStream teachersUnavailabilityFile = new FileInputStream(teachersUnavailabilityFilePath);
             Workbook unavailabilityWorkbook = new XSSFWorkbook(teachersUnavailabilityFile)) {

            teacherUnavailabilityService.addTeachersUnavailability(unavailabilityWorkbook, examSession);
        }
    }
}