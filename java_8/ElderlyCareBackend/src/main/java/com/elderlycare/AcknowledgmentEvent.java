
package com.elderlycare;

import java.sql.Timestamp;

public class AcknowledgmentEvent {

    private int acknowledgmentId;
    private int doseId;
    private int dispensingId;
    private Timestamp acknowledgedAt;
    private String acknowledgmentMethod;
    private String acknowledgmentStatus;

    public AcknowledgmentEvent() {
    }

    public int getAcknowledgmentId() {
        return acknowledgmentId;
    }

    public void setAcknowledgmentId(int acknowledgmentId) {
        this.acknowledgmentId = acknowledgmentId;
    }

    public int getDoseId() {
        return doseId;
    }

    public void setDoseId(int doseId) {
        this.doseId = doseId;
    }

    public int getDispensingId() {
        return dispensingId;
    }

    public void setDispensingId(int dispensingId) {
        this.dispensingId = dispensingId;
    }

    public Timestamp getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Timestamp acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getAcknowledgmentMethod() {
        return acknowledgmentMethod;
    }

    public void setAcknowledgmentMethod(String acknowledgmentMethod) {
        this.acknowledgmentMethod = acknowledgmentMethod;
    }

    public String getAcknowledgmentStatus() {
        return acknowledgmentStatus;
    }

    public void setAcknowledgmentStatus(String acknowledgmentStatus) {
        this.acknowledgmentStatus = acknowledgmentStatus;
    }
}
