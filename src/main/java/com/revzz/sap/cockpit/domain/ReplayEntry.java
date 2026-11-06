package com.revzz.sap.cockpit.domain;

import java.time.LocalDateTime;

/** A failed message waiting to be re-processed. */
public class ReplayEntry {

    public static final String STATE_QUEUED = "QUEUED";
    public static final String STATE_PLAYED = "PLAYED";
    public static final String STATE_FAILED = "FAILED";

    private String queueId;
    private String messageId;
    private String interfaceName;
    private String state;
    private LocalDateTime queuedAt;
    private LocalDateTime playedAt;
    private String playedBy;
    private String result;

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(LocalDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(LocalDateTime playedAt) {
        this.playedAt = playedAt;
    }

    public String getPlayedBy() {
        return playedBy;
    }

    public void setPlayedBy(String playedBy) {
        this.playedBy = playedBy;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    /** Formatted for display - the raw timestamp carries nanoseconds. */
    public String getQueuedAtDisplay() {
        if (queuedAt == null) {
            return "";
        }
        return queuedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /** Formatted for display - the raw timestamp carries nanoseconds. */
    public String getPlayedAtDisplay() {
        if (playedAt == null) {
            return "";
        }
        return playedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
