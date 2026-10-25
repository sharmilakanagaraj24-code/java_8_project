
package com.elderlycare;

import java.sql.Timestamp;

public class ScheduledDose {

    private int doseId;
    private int scheduleId;
    private Timestamp dueTime;
    private String doseStatus;
    private Timestamp createdAt;

    public ScheduledDose() {
    }

    public int getDoseId() {
        return doseId;
    }

    public void setDoseId(int doseId) {
        this.doseId = doseId;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Timestamp getDueTime() {
        return dueTime;
    }

    public void setDueTime(Timestamp dueTime) {
        this.dueTime = dueTime;
    }

    public String getDoseStatus() {
        return doseStatus;
    }

    public void setDoseStatus(String doseStatus) {
        this.doseStatus = doseStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
