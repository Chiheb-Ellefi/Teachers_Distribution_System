package org.teacherdistributionsystem.distribution_system.controllers;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.dtos.assignment.ExamSessionDto;
import org.teacherdistributionsystem.distribution_system.exceptions.custom.BadRequestException;
import org.teacherdistributionsystem.distribution_system.models.requests.MainRequestBody;
import org.teacherdistributionsystem.distribution_system.models.responses.teacher.GradeCount;
import org.teacherdistributionsystem.distribution_system.services.ExcelImportOrchestrator;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamService;
import org.teacherdistributionsystem.distribution_system.services.assignment.ExamSessionService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherService;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping("/api/v1/session")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MainController {
    private final ExcelImportOrchestrator excelImportOrchestrator;
    private final ExamSessionService examSessionService;
    private final ExamService examService;
    private final TeacherService teacherService;


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

    @GetMapping("/dashboard")
    public ResponseEntity<Object> dashboard() {
        Map<String, Object> response = new HashMap<>();

        Long teachersCount = teacherService.getTeachersCount();
        Long examsCount = examService.getExamCount();
        List<GradeCount> teachersPerGrade = teacherService.teachersPerGrade();

        teachersPerGrade.forEach(g ->
                g.setPercentage((g.getNbr() * 100) / teachersCount)
        );

        response.put("teachersCount", teachersCount);
        response.put("examsCount", examsCount);
        response.put("teachersPerGrade", teachersPerGrade);

        return ResponseEntity.ok(response);
    }



}
