package com.revzz.sap.cockpit.domain;

/** The portal's own roles - not SAP authorisation roles. */
public enum PortalRole {

    /** Signs in and sees the dashboard. */
    VIEWER,

    /** Everything a viewer sees, plus the message log and the triage. */
    ANALYST,

    /** Everything, including the alert handling and the replay queue. */
    OPS_ADMIN;

    public boolean canSeeOperations() {
        return this != VIEWER;
    }

    public boolean canOperate() {
        return this == OPS_ADMIN;
    }

    public String cssClass() {
        return name().toLowerCase();
    }
}
