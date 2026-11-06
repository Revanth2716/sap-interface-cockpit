package com.revzz.sap.cockpit.domain;

/** The SLA picture for one interface over the monitoring window. */
public class SlaReport {

    private String interfaceName;
    private String description;
    private String type;
    private String direction;
    private String owner;
    private boolean critical;
    private int slaPercent;
    private String system;
    private String target;

    private int total;
    private int ok;
    private int errors;
    private int warnings;

    private double successRate;
    private long averageMs;
    private long p95Ms;
    private long maxMs;
    private int oldestFailureAgeHours;

    /** True when the success rate is below the agreed SLA. */
    public boolean breaching() {
        return successRate < slaPercent;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public int getSlaPercent() {
        return slaPercent;
    }

    public void setSlaPercent(int slaPercent) {
        this.slaPercent = slaPercent;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getOk() {
        return ok;
    }

    public void setOk(int ok) {
        this.ok = ok;
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

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public long getAverageMs() {
        return averageMs;
    }

    public void setAverageMs(long averageMs) {
        this.averageMs = averageMs;
    }

    public long getP95Ms() {
        return p95Ms;
    }

    public void setP95Ms(long p95Ms) {
        this.p95Ms = p95Ms;
    }

    public long getMaxMs() {
        return maxMs;
    }

    public void setMaxMs(long maxMs) {
        this.maxMs = maxMs;
    }

    public int getOldestFailureAgeHours() {
        return oldestFailureAgeHours;
    }

    public void setOldestFailureAgeHours(int oldestFailureAgeHours) {
        this.oldestFailureAgeHours = oldestFailureAgeHours;
    }
}
