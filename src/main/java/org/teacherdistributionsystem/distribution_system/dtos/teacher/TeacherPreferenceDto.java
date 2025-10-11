package org.teacherdistributionsystem.distribution_system.dtos.teacher;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.enums.PreferenceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherPreferenceDto {
    private Long id;
    private Long teacherId;
    private Long examSessionId;
    private PreferenceType preferenceType;
    private Integer priorityWeight;
}