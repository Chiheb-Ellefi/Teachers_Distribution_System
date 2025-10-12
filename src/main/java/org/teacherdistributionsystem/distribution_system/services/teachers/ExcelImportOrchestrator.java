package org.teacherdistributionsystem.distribution_system.services.teachers;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamService;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamSessionService;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class ExcelImportOrchestrator {

    private final TeacherService teacherService;
    private final  TeacherQuotaService quotaService;
    private final  GradeService gradeService;
    private final  TeacherPreferenceService preferenceService;
    private final ExamSessionService examSessionService;
    private final ExamService examService;
    private final TeacherUnavailabilityService teacherUnavailabilityService;
    @Value("${spring.application.teachersListFile}")
    private  String teachersList;
    @Value("${spring.application.teachersUnavailabilyFile}")
    private  String teachersUnavailability;
    @Value("${spring.application.examDataFile}")
    private  String examData;
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
    @PostConstruct
    public void importTeachersData() throws IOException {

        ExamSession examSession;
        try (FileInputStream examDataFile = new FileInputStream(examData)) {
            examSession = examSessionService.addSession(examDataFile);
        }

        Map<String, Teacher> teacherMap;
        try (FileInputStream teachersListFile = new FileInputStream(teachersList)) {
            teacherMap = teacherService.populateTeachersTable(teachersListFile);
        }

        try (FileInputStream teachersListFile = new FileInputStream(teachersList)) {
            gradeService.addGrades(teachersListFile);
        }

        try (FileInputStream teachersListFile = new FileInputStream(teachersList)) {
            quotaService.addTeachersQuota(teachersListFile, teacherMap, examSession);
        }

        try (FileInputStream teachersListFile = new FileInputStream(teachersList)) {
            preferenceService.addTeachersPreference(teachersListFile, teacherMap, examSession);
        }

        try (FileInputStream examDataFile = new FileInputStream(examData)) {
            examService.addExams(examDataFile, examSession);
        }

        try (FileInputStream teachersUnavailabilityFile = new FileInputStream(teachersUnavailability)) {
            teacherUnavailabilityService.addTeachersUnavailability(teachersUnavailabilityFile, examSession);
        }
    }
}