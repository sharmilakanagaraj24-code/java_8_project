
package com.elderlycare;

import java.sql.Timestamp;

public class DispensingEvent {

    private int dispensingId;
    private int doseId;
    private int slotId;
    private Timestamp dispensedAt;
    private String dispenseStatus;
    private int tabletsDispensed;
    private String deviceMessage;

    public DispensingEvent() {
    }

    public int getDispensingId() {
        return dispensingId;
    }

    public void setDispensingId(int dispensingId) {
        this.dispensingId = dispensingId;
    }

    public int getDoseId() {
        return doseId;
    }

    public void setDoseId(int doseId) {
        this.doseId = doseId;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public Timestamp getDispensedAt() {
        return dispensedAt;
    }

    public void setDispensedAt(Timestamp dispensedAt) {
        this.dispensedAt = dispensedAt;
    }

    public String getDispenseStatus() {
        return dispenseStatus;
    }

    public void setDispenseStatus(String dispenseStatus) {
        this.dispenseStatus = dispenseStatus;
    }

    public int getTabletsDispensed() {
        return tabletsDispensed;
    }

    public void setTabletsDispensed(int tabletsDispensed) {
        this.tabletsDispensed = tabletsDispensed;
    }

    public String getDeviceMessage() {
        return deviceMessage;
    }

    public void setDeviceMessage(String deviceMessage) {
        this.deviceMessage = deviceMessage;
    }
}
