package org.teacherdistributionsystem.distribution_system.dtos.teacher;

import jakarta.persistence.*;
import lombok.*;
import org.teacherdistributionsystem.distribution_system.enums.GradeType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaPerGradeDto {
    private Long id;
    private GradeType grade;
    private Integer defaultQuota;
    private Integer priority;
}