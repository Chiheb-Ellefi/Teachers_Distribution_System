package org.teacherdistributionsystem.distribution_system.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.ExamSessionDto;
import org.teacherdistributionsystem.distribution_system.entities.assignment.ExamSession;
import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;
import org.teacherdistributionsystem.distribution_system.mappers.assignment.ExamSessionMapper;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamService;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamSessionService;
import org.teacherdistributionsystem.distribution_system.services.teacher.*;
import org.teacherdistributionsystem.distribution_system.utils.TeacherMaps;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
@RequiredArgsConstructor
@Service
public class ExcelImportOrchestrator {

    private final TeacherService teacherService;
    private final TeacherQuotaService quotaService;
    private final GradeService gradeService;
    private final ExamSessionService examSessionService;
    private final ExamService examService;
    private final TeacherUnavailabilityService teacherUnavailabilityService;






    @Transactional
    public ExamSessionDto importData(
            String examDataFilePath,
            String teachersListFilePath,
            String teachersUnavailabilityFilePath) throws IOException {

        ExamSession examSession;
        try (FileInputStream examDataFile = new FileInputStream(examDataFilePath);
             Workbook examWorkbook = new XSSFWorkbook(examDataFile)) {
            examSession = examSessionService.addSession(examWorkbook);
        }

        TeacherMaps teacherMaps;
        try (FileInputStream teachersListFile = new FileInputStream(teachersListFilePath);
             Workbook teachersWorkbook = new XSSFWorkbook(teachersListFile)) {

            teacherMaps = teacherService.populateTeachersTable(teachersWorkbook);

            gradeService.addGrades(teachersWorkbook);
            quotaService.addTeachersQuota(
                    teachersWorkbook,
                    teacherMaps.getEmailToTeacherMap(),
                    examSession
            );
        }

        try (FileInputStream examDataFile = new FileInputStream(examDataFilePath);
             Workbook examWorkbook = new XSSFWorkbook(examDataFile)) {
            examService.addExams(examWorkbook, examSession);
        }

        try (FileInputStream teachersUnavailabilityFile = new FileInputStream(teachersUnavailabilityFilePath);
             Workbook unavailabilityWorkbook = new XSSFWorkbook(teachersUnavailabilityFile)) {

            teacherUnavailabilityService.addTeachersUnavailability(
                    unavailabilityWorkbook,
                    examSession,
                    teacherMaps.getAbrvToEmailMap()
            );
        }

        return ExamSessionMapper.toExamSessionDto(examSession);
    }
}