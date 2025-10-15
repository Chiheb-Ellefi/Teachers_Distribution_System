package org.teacherdistributionsystem.distribution_system.services.teachers;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.teacherdistributionsystem.distribution_system.dtos.teacher.QuotaPerGradeDto;
import org.teacherdistributionsystem.distribution_system.entities.teacher.QuotaPerGrade;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;
import org.teacherdistributionsystem.distribution_system.mappers.teacher.QuotaPerGradeMapper;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.QuotaPerGradeRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuotaPerGradeService {
    private final QuotaPerGradeRepository quotaPerGradeRepository;

    public List<QuotaPerGradeDto> saveAll(List<QuotaPerGrade> quotaPerGrades) {
        return quotaPerGradeRepository.saveAll(quotaPerGrades).stream().map(QuotaPerGradeMapper::toDto).toList();
    }
    public QuotaPerGradeDto getQuotaByGrade(GradeType grade) throws BadRequestException {
        Supplier<BadRequestException> exceptionSupplier = () -> new BadRequestException("Grade is not valid");
        return QuotaPerGradeMapper.toDto(quotaPerGradeRepository.findByGrade(grade).orElseThrow(exceptionSupplier));
    }
    public Map<GradeType, Integer> getPrioritiesByGrade() {
        return quotaPerGradeRepository.getPrioritiesByGrade().stream()
                .collect(Collectors.toMap(
                        row -> (GradeType) row[0],
                        row -> (Integer) row[1]
                ));
    }
}
