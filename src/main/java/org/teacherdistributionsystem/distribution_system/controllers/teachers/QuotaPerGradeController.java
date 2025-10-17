package org.teacherdistributionsystem.distribution_system.controllers.teachers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.teacherdistributionsystem.distribution_system.dtos.teacher.QuotaPerGradeDto;
import org.teacherdistributionsystem.distribution_system.exceptions.custom.BadRequestException;
import org.teacherdistributionsystem.distribution_system.mappers.teacher.QuotaPerGradeMapper;
import org.teacherdistributionsystem.distribution_system.services.teacher.QuotaPerGradeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grade")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuotaPerGradeController {
    private final QuotaPerGradeService quotaPerGradeService;

    @PostMapping("/batch")
    public ResponseEntity<String> saveAll(@RequestBody List<QuotaPerGradeDto> quotasPerGradeDtos) {
        if(quotasPerGradeDtos == null || quotasPerGradeDtos.isEmpty()){
            throw new BadRequestException("Bad Request", "Quota per grades cannot be empty");
        }
        quotaPerGradeService.saveAll(
                quotasPerGradeDtos.stream()
                        .map(QuotaPerGradeMapper::toEntity)
                        .toList()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body("Data saved successfully");
    }

  /*  @GetMapping
    public ResponseEntity<List<QuotaPerGradeDto>> getAll() {
        List<QuotaPerGradeDto> quotas = quotaPerGradeService.findAll()
                .stream()
                .map(QuotaPerGradeMapper::toDto)
                .toList();
        return ResponseEntity.ok(quotas);
    }*/
}