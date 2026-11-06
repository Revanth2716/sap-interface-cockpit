package com.revzz.sap.cockpit.domain;

/** One interface from the catalogue (a simplified model of SAP's IFDEF). */
public class InterfaceDef {

    public static final String TYPE_RFC = "RFC";
    public static final String TYPE_IDOC = "IDOC";
    public static final String TYPE_BAPI = "BAPI";

    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_WARNING = "WARNING";

    private String name;
    private String description;
    private String type;
    private String direction;
    private String owner;
    private String system;
    private String target;
    private int slaPercent;
    private boolean critical;

    public boolean isInbound() {
        return "IN".equals(direction);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getSlaPercent() {
        return slaPercent;
    }

    public void setSlaPercent(int slaPercent) {
        this.slaPercent = slaPercent;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }
}
