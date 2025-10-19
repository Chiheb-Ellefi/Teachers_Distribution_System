package org.teacherdistributionsystem.distribution_system.services.teacher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.teacherdistributionsystem.distribution_system.dtos.teacher.QuotaPerGradeDto;
import org.teacherdistributionsystem.distribution_system.entities.teacher.QuotaPerGrade;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;
import org.teacherdistributionsystem.distribution_system.mappers.teacher.QuotaPerGradeMapper;
import org.teacherdistributionsystem.distribution_system.repositories.teacher.QuotaPerGradeRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuotaPerGradeService {
    private final QuotaPerGradeRepository quotaPerGradeRepository;

    public void saveAll(List<QuotaPerGrade> quotaPerGrades) {
        quotaPerGradeRepository.saveAll(quotaPerGrades);
    }
    public QuotaPerGradeDto getQuotaByGrade(GradeType grade) throws IllegalArgumentException {
        Supplier<IllegalArgumentException> exceptionSupplier = () -> new IllegalArgumentException("Grade is not valid");
        return QuotaPerGradeMapper.toDto(quotaPerGradeRepository.findByGrade(grade).orElseThrow(exceptionSupplier));
    }
    public Map<GradeType, Integer> getPrioritiesByGrade() {
        return quotaPerGradeRepository.getPrioritiesByGrade().stream()
                .collect(Collectors.toMap(
                        row -> (GradeType) row[0],
                        row -> (Integer) row[1]
                ));
    }
    public Long getCount(){
        return quotaPerGradeRepository.count();
    }
    @Transactional
    public void clearAllQuotasPerGrade() {
        quotaPerGradeRepository.deleteAllInBatch();
    }
    public Map<GradeType, Integer> getDefaultQuotasByGrade() {
        List<QuotaPerGrade> quotaPerGrades = quotaPerGradeRepository.findAll();

        Map<GradeType, Integer> defaultQuotas = new HashMap<>();
        for (QuotaPerGrade qpg : quotaPerGrades) {
            defaultQuotas.put(qpg.getGrade(), qpg.getDefaultQuota());
        }

        return defaultQuotas;
    }

    public void updateQuotaPerGrade(String grade,Integer quota){
        quotaPerGradeRepository.updateQuotaPerGrade(grade,quota);
    }




}
