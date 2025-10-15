package org.teacherdistributionsystem.distribution_system.controllers.teachers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.teacherdistributionsystem.distribution_system.dtos.teacher.QuotaPerGradeDto;
import org.teacherdistributionsystem.distribution_system.exceptions.BadRequestException;
import org.teacherdistributionsystem.distribution_system.mappers.teacher.QuotaPerGradeMapper;
import org.teacherdistributionsystem.distribution_system.services.teachers.QuotaPerGradeService;

import java.util.List;

@RestController("/api/v1/grade")
@RequiredArgsConstructor
public class QuotaPerGradeController {
    private final QuotaPerGradeService quotaPerGradeService;

    public ResponseEntity<String> saveAll(@RequestBody  List<QuotaPerGradeDto> quotasPerGradeDtos) {
       if(quotasPerGradeDtos.isEmpty()){
           throw new BadRequestException("Bad Request", "Quota per grades cannot be empty");
       }
        quotaPerGradeService.saveAll( quotasPerGradeDtos.stream().map(QuotaPerGradeMapper::toEntity).toList());
        return ResponseEntity.ok().body("Data saved successfully");
    }
}
