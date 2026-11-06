package com.revzz.sap.cockpit.domain;

import java.time.LocalDateTime;

/** One row of the message log - a single RFC, IDoc or BAPI call. */
public class MessageRecord {

    private String messageId;
    private String interfaceName;
    private String messageType;
    private String direction;
    private String status;
    private String errorCode;
    private String errorText;
    private int durationMs;
    private String host;
    private String correlationId;
    private LocalDateTime createdAt;

    public boolean isError() {
        return InterfaceDef.STATUS_ERROR.equals(status);
    }

    public boolean isWarning() {
        return InterfaceDef.STATUS_WARNING.equals(status);
    }

    public boolean isOk() {
        return InterfaceDef.STATUS_OK.equals(status);
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Formatted for display - the raw timestamp carries nanoseconds. */
    public String getCreatedAtDisplay() {
        if (createdAt == null) {
            return "";
        }
        return createdAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
