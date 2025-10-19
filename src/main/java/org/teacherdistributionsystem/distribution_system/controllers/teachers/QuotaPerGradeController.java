package org.teacherdistributionsystem.distribution_system.controllers.teachers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.dtos.teacher.QuotaPerGradeDto;
import org.teacherdistributionsystem.distribution_system.exceptions.custom.BadRequestException;
import org.teacherdistributionsystem.distribution_system.mappers.teacher.QuotaPerGradeMapper;
import org.teacherdistributionsystem.distribution_system.services.teacher.QuotaPerGradeService;
import org.teacherdistributionsystem.distribution_system.services.teacher.TeacherQuotaService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/grade")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuotaPerGradeController {
    private final QuotaPerGradeService quotaPerGradeService;
    private final TeacherQuotaService teacherQuotaService;

    @PostMapping("/batch")
    public ResponseEntity<String> saveAll(@RequestBody List<QuotaPerGradeDto> quotasPerGradeDtos) {
        if(quotasPerGradeDtos == null || quotasPerGradeDtos.isEmpty()){
            throw new BadRequestException("Bad Request", "Quota per grades cannot be empty");
        }
        if(quotaPerGradeService.getCount()!=0){
            quotaPerGradeService.clearAllQuotasPerGrade();
        }

        quotaPerGradeService.saveAll(
                quotasPerGradeDtos.stream()
                        .map(QuotaPerGradeMapper::toEntity)
                        .toList()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body("Data saved successfully");
    }

    @PatchMapping("/quota")
    public ResponseEntity<String> updateQuotaPerGrade(@RequestBody Map<String,Integer> quotaPerGradeDto){
        for (Map.Entry<String,Integer> entry : quotaPerGradeDto.entrySet()) {
            quotaPerGradeService.updateQuotaPerGrade(entry.getKey(), entry.getValue());
            teacherQuotaService.updateQuotaPerGrade(entry.getKey(), entry.getValue());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("Data updated successfully");
    }

}