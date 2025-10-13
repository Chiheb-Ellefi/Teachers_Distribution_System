package org.teacherdistributionsystem.distribution_system.models.keys;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExamKey(
        LocalDate examDate,
        LocalTime startTime,
        LocalTime endTime,
        String examType,
        Long teacherId,
        String numRooms
) {}