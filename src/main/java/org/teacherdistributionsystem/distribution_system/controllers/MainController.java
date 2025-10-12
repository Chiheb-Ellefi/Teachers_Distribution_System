package org.teacherdistributionsystem.distribution_system.controllers;

import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.teacherdistributionsystem.distribution_system.models.MainRequestBody;
import org.teacherdistributionsystem.distribution_system.services.ExcelImportOrchestrator;
import org.teacherdistributionsystem.distribution_system.services.assignment.AssignmentAlgorithmService;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamService;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamSessionService;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherQuotaService;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherService;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherUnavailabilityService;

import java.io.IOException;

@RestController
public class MainController {
    private final ExcelImportOrchestrator excelImportOrchestrator;
    private final TeacherQuotaService teacherQuotaService;
    private final TeacherUnavailabilityService teacherUnavailabilityService;
    private final ExamSessionService examSessionService;
    private final ExamService examService;
    private final TeacherService teacherService;

    public MainController(ExcelImportOrchestrator excelImportOrchestrator,TeacherQuotaService teacherQuotaService,
                          TeacherUnavailabilityService teacherUnavailabilityService, ExamSessionService examSessionService,
                          ExamService examService,TeacherService teacherService ) {
        this.excelImportOrchestrator = excelImportOrchestrator;
        this.teacherQuotaService = teacherQuotaService;
        this.teacherUnavailabilityService = teacherUnavailabilityService;
        this.examSessionService = examSessionService;
        this.examService = examService;
        this.teacherService = teacherService;
    }
    @PostMapping
    public ResponseEntity<String> mainController(@RequestBody MainRequestBody request) {
       try {
           excelImportOrchestrator.importData(request.getExamDataFilePath(), request.getTeachersListFilePath(), request.getTeachersUnavailabilityFilePath());
           return ResponseEntity.ok().body("Data imported successfully");
       }catch (IOException e){
          throw new RuntimeException(e);
       }
    }


}
