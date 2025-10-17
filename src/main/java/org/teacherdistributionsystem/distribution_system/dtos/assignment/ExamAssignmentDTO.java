package org.teacherdistributionsystem.distribution_system.dtos.assignment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class ExamAssignmentDTO {
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

    @JsonProperty("requiredSupervisors")
    private Integer requiredSupervisors;

    @JsonProperty("ownerTeacherId")
    private Long ownerTeacherId;

    @JsonProperty("ownerTeacherName")
    private String ownerTeacherName;

    @JsonProperty("assignedTeachers")
    private List<TeacherAssignmentsDTO> assignedTeachers;

    @JsonProperty("examDate")
    private String examDate;

    @JsonProperty("startTime")
    private String startTime;

    @JsonProperty("endTime")
    private String endTime;
}