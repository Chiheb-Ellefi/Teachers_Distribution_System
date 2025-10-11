package org.teacherdistributionsystem.distribution_system.dtos.teacher;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.enums.QuotaType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherQuotaDto {
    private Long id;
    private Long examSessionId;
    private Long teacherId;
    private Integer assignedQuota;
    private QuotaType quotaType;
    private String reason;
}