package org.teacherdistributionsystem.distribution_system.controllers;


import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.models.MainRequestBody;
import org.teacherdistributionsystem.distribution_system.services.ExcelImportOrchestrator;


import java.io.IOException;
@RequestMapping("/api/v1/assignments")
@RestController
public class MainController {
    private final ExcelImportOrchestrator excelImportOrchestrator;

    public MainController(ExcelImportOrchestrator excelImportOrchestrator ) {
        this.excelImportOrchestrator = excelImportOrchestrator;

    }

    @PostMapping("/upload")
    public ResponseEntity<String> mainController(@RequestBody MainRequestBody request) {
       try {
           excelImportOrchestrator.importData(request.getExamDataFilePath(), request.getTeachersListFilePath(), request.getTeachersUnavailabilityFilePath());
           return ResponseEntity.ok().body("Data imported successfully");
       }catch (IOException e){
          throw new RuntimeException(e);
       }
    }


}
