package org.teacherdistributionsystem.distribution_system.models.responses.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionAssignmentsResponse {
    private Map<Integer, Map<Integer, List<DaySeanceGroupAssignments>>> assignments;
}