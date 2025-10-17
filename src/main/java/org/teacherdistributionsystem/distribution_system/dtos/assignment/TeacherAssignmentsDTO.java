package org.teacherdistributionsystem.distribution_system.dtos.assignment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAssignmentsDTO {

    @JsonProperty("teacherId")
    private Long teacherId;

    @JsonProperty("teacherName")
    private String teacherName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("grade")
    private String grade;

    @JsonProperty("assignedSupervisions")
    private Integer assignedSupervisions;

    @JsonProperty("quotaSupervisions")
    private Integer quotaSupervisions;

    @JsonProperty("utilizationPercentage")
    private Double utilizationPercentage;

    @JsonProperty("assignments")
    private List<TeacherAssignment> assignments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherAssignment {
        @JsonProperty("examId")
        private String examId;

        @JsonProperty("day")
        private Integer day;

        @JsonProperty("dayLabel")
        private String dayLabel;

        @JsonProperty("seance")
        private Integer seance;

        @JsonProperty("seanceLabel")
        private String seanceLabel;

        @JsonProperty("room")
        private String room;

        // NOUVEAUX CHAMPS - dates réelles depuis le JSON
        @JsonProperty("examDate")
        private String examDate;

        @JsonProperty("startTime")
        private String startTime;

        @JsonProperty("endTime")
        private String endTime;
    }
}