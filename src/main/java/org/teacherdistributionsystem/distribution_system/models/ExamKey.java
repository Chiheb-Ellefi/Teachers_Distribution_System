package org.teacherdistributionsystem.distribution_system.models;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExamKey(LocalDate examDate, LocalTime startTime, LocalTime endTime, String examType, Long teacherId) {
    public  ExamKey(LocalDate examDate, LocalTime startTime, LocalTime endTime, String examType, Long teacherId) {
        this.endTime=endTime;
        this.startTime=startTime;
        this.examType=examType;
        this.teacherId=teacherId;
        this.examDate=examDate;
    }
}
