package org.teacherdistributionsystem.distribution_system.enums;

import lombok.Getter;

import java.time.LocalTime;

@Getter
public enum SeanceType {
    S1(LocalTime.of(8,30,0),LocalTime.of(10,0,0)),
    S2(LocalTime.of(10,30,0),LocalTime.of(12,0,0)),
    S3(LocalTime.of(12,30,0),LocalTime.of(14,0,0)),
    S4(LocalTime.of(14,30,0),LocalTime.of(16,0,0));

    private final LocalTime startTime;
    private final LocalTime endTime;
    SeanceType(LocalTime startTime,LocalTime endTime){
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public static SeanceType fromTime(LocalTime time) {
        for (SeanceType session : values()) {
            if (session.startTime.equals(time)) {
                return session;
            }
        }
        throw new IllegalArgumentException("No session found for start time: " + time);
    }

}
