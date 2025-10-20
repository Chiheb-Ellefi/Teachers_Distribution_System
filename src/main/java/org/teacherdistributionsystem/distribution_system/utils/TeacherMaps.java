package org.teacherdistributionsystem.distribution_system.utils;

import org.teacherdistributionsystem.distribution_system.entities.teacher.Teacher;

import java.util.Map;

public class TeacherMaps {
    private final Map<String, Teacher> emailToTeacherMap;
    private final Map<String, String> abrvToEmailMap;

    public TeacherMaps(Map<String, Teacher> emailToTeacherMap, Map<String, String> abrvToEmailMap) {
        this.emailToTeacherMap = emailToTeacherMap;
        this.abrvToEmailMap = abrvToEmailMap;
    }

    public Map<String, Teacher> getEmailToTeacherMap() {
        return emailToTeacherMap;
    }

    public Map<String, String> getAbrvToEmailMap() {
        return abrvToEmailMap;
    }
}