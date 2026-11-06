package com.revzz.sap.cockpit.domain;

/** One hour of traffic - the unit the trend chart is drawn from. */
public class HourlyBucket {

    private String label;
    private int hour;
    private int total;
    private int errors;
    private int warnings;
    private long averageMs;

    public int successRatePercent() {
        if (total == 0) {
            return 100;
        }
        return Math.round(100.0f * (total - errors) / total);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getWarnings() {
        return warnings;
    }

    public void setWarnings(int warnings) {
        this.warnings = warnings;
    }

    public long getAverageMs() {
        return averageMs;
    }

    public void setAverageMs(long averageMs) {
        this.averageMs = averageMs;
    }
}
