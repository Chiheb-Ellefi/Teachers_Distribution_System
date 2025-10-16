package org.teacherdistributionsystem.distribution_system.controllers;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.ExamSessionDto;
import org.teacherdistributionsystem.distribution_system.exceptions.BadRequestException;
import org.teacherdistributionsystem.distribution_system.models.MainRequestBody;
import org.teacherdistributionsystem.distribution_system.models.projections.TeacherUnavailabilityProjection;
import org.teacherdistributionsystem.distribution_system.services.ExcelImportOrchestrator;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamService;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamSessionService;
import org.teacherdistributionsystem.distribution_system.services.teachers.TeacherUnavailabilityService;


import java.io.IOException;
import java.util.List;

@RequestMapping("/api/v1/session")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MainController {
    private final ExcelImportOrchestrator excelImportOrchestrator;
    private final ExamSessionService examSessionService;
    private final ExamService examService;


    @PostMapping("/upload")
    public ResponseEntity<ExamSessionDto> mainController(@RequestBody MainRequestBody request) {
       try {
           ExamSessionDto result= excelImportOrchestrator.importData(request.getExamDataFilePath(), request.getTeachersListFilePath(), request.getTeachersUnavailabilityFilePath());
           return ResponseEntity.ok().body(result);
       }catch (IOException e){
          throw new RuntimeException(e);
       }
    }
    @GetMapping("/exists")
    public ResponseEntity<Boolean> generatedPlanning(){
        return ResponseEntity.ok(!examService.dataExists());
    }

    @PatchMapping("/{sessionId}/teachers-per-exam")
    public ResponseEntity<ExamSessionDto> setNumberTeachersPerExam(@PathVariable Long sessionId, @RequestBody Integer teachersPerExam) throws org.apache.coyote.BadRequestException {
        if(sessionId == null || teachersPerExam == null) {
            throw new BadRequestException("Bad Request","Exam session id is null or teachersPerExam is null");
        }
       ExamSessionDto response= examSessionService.setTeachersPerExam(sessionId,teachersPerExam);
        return ResponseEntity.ok(response);
    }


}
