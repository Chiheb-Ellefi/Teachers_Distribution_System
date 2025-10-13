package org.teacherdistributionsystem.distribution_system.dtos.teacher;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.enums.PreferenceType;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherPreferenceDto {
    private Long id;
    private Long teacherId;
    private Long examSessionId;
    private List<PreferenceType> preferenceTypes;
    private Integer priorityWeight;
}