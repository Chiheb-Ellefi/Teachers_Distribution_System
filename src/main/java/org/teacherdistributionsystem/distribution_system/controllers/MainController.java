package org.teacherdistributionsystem.distribution_system.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.teacherdistributionsystem.distribution_system.models.MainRequestBody;
import org.teacherdistributionsystem.distribution_system.services.ExcelImportOrchestrator;

import java.io.IOException;

@RestController
public class MainController {
    private final ExcelImportOrchestrator excelImportOrchestrator;

    public MainController(ExcelImportOrchestrator excelImportOrchestrator) {
        this.excelImportOrchestrator = excelImportOrchestrator;
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
