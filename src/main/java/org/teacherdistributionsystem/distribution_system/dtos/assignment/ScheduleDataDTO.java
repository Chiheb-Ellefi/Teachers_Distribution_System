package org.teacherdistributionsystem.distribution_system.dtos.assignment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDataDTO {

    @JsonProperty("academicYear")
    private String academicYear;

    @JsonProperty("semester")
    private String semester;

    @JsonProperty("session")
    private String session;

    @JsonProperty("level")
    private String level;

    @JsonProperty("code")
    private String code;

    @JsonProperty("weeks")
    private List<Week> weeks;

    @JsonProperty("teachersSurveillance")
    private List<TeacherSurveillance> teachersSurveillance;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Week {
        @JsonProperty("weekNumber")
        private Integer weekNumber;

        @JsonProperty("startDate")
        private String startDate;

        @JsonProperty("endDate")
        private String endDate;

        @JsonProperty("days")
        private List<Day> days;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Day {
        @JsonProperty("date")
        private String date;

        @JsonProperty("dayName")
        private String dayName;

        @JsonProperty("sessions")
        private List<Session> sessions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Session {
        @JsonProperty("timeSlot")
        private String timeSlot;

        @JsonProperty("subject")
        private String subject;

        @JsonProperty("teacher")
        private String teacher;

        @JsonProperty("teacherCode")
        private String teacherCode;

        @JsonProperty("room")
        private String room;

        public boolean isEmpty() {
            return (subject == null || subject.trim().isEmpty()) &&
                    (teacher == null || teacher.trim().isEmpty());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherSurveillance {
        @JsonProperty("teacherName")
        private String teacherName;

        @JsonProperty("teacherCode")
        private String teacherCode;

        @JsonProperty("assignments")
        private List<Assignment> assignments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Assignment {
        @JsonProperty("date")
        private String date;

        @JsonProperty("time")
        private String time;

        @JsonProperty("duration")
        private String duration;

        @JsonProperty("type")
        private String type; // "Responsable" ou "Surveillance"

        @JsonProperty("subject")
        private String subject;

        @JsonProperty("room")
        private String room;

        public String getDisplaySubject() {
            if (subject == null || subject.trim().isEmpty()) {
                return "Surveillance";
            }
            return subject;
        }
    }
}