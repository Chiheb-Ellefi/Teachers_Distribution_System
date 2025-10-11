package org.teacherdistributionsystem.distribution_system.dtos.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherUnavailabilityDto {
    private Long id;
    private Long examSessionId;
    private Long teacherId;
    private Integer numeroJour;
    private String seance;
}
