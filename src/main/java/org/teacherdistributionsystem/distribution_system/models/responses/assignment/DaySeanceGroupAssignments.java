package org.teacherdistributionsystem.distribution_system.models.responses.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.teacherdistributionsystem.distribution_system.models.responses.teacher.TeacherResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DaySeanceGroupAssignments {
    private Integer examDay;
    private Integer seance;
    private LocalDate examDate;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String numRooms;
    private List<TeacherResponse> supervisors;
}