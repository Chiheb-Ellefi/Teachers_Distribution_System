package org.teacherdistributionsystem.distribution_system.mappers.teacher;

import org.teacherdistributionsystem.distribution_system.dtos.teacher.QuotaPerGradeDto;
import org.teacherdistributionsystem.distribution_system.entities.teacher.QuotaPerGrade;

public class QuotaPerGradeMapper {

    public static QuotaPerGradeDto toDto(QuotaPerGrade quotaPerGrade) {
        return QuotaPerGradeDto.builder()
                .id(quotaPerGrade.getId())
                .grade(quotaPerGrade.getGrade())
                .defaultQuota(quotaPerGrade.getDefaultQuota())
                .priority(quotaPerGrade.getPriority())
                .build();

    }
    public static QuotaPerGrade toEntity(QuotaPerGradeDto quotaPerGradeDto) {
        return QuotaPerGrade.builder()
                .id(quotaPerGradeDto.getId())
                .grade(quotaPerGradeDto.getGrade())
                .defaultQuota(quotaPerGradeDto.getDefaultQuota())
                .priority(quotaPerGradeDto.getPriority())
                .build();
    }
}
