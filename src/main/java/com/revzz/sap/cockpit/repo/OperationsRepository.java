package com.revzz.sap.cockpit.repo;

import com.revzz.sap.cockpit.domain.Alert;
import com.revzz.sap.cockpit.domain.ReplayEntry;
import com.revzz.sap.cockpit.domain.Severity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/** Alerts and the replay queue. */
@Repository
public class OperationsRepository {

    private final JdbcTemplate jdbc;

    public OperationsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Alert> ALERT_MAPPER = new RowMapper<Alert>() {
        @Override
        public Alert mapRow(ResultSet rs, int rowNum) throws SQLException {
            Alert alert = new Alert();
            alert.setAlertId(rs.getString("ALERT_ID"));
            alert.setInterfaceName(rs.getString("IFACE"));
            alert.setRule(rs.getString("RULE"));
            alert.setSeverity(Severity.valueOf(rs.getString("SEVERITY")));
            alert.setMessage(rs.getString("MESSAGE"));
            Timestamp raised = rs.getTimestamp("RAISED_AT");
            alert.setRaisedAt(raised == null ? null : raised.toLocalDateTime());
            alert.setAcknowledged("Y".equalsIgnoreCase(rs.getString("ACK_FLAG")));
            alert.setAcknowledgedBy(rs.getString("ACK_BY"));
            Timestamp ack = rs.getTimestamp("ACK_AT");
            alert.setAcknowledgedAt(ack == null ? null : ack.toLocalDateTime());
            return alert;
        }
    };

    private static final RowMapper<ReplayEntry> QUEUE_MAPPER = new RowMapper<ReplayEntry>() {
        @Override
        public ReplayEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            ReplayEntry entry = new ReplayEntry();
            entry.setQueueId(rs.getString("QUEUE_ID"));
            entry.setMessageId(rs.getString("MSG_ID"));
            entry.setInterfaceName(rs.getString("IFACE"));
            entry.setState(rs.getString("STATE"));
            Timestamp queued = rs.getTimestamp("QUEUED_AT");
            entry.setQueuedAt(queued == null ? null : queued.toLocalDateTime());
            Timestamp played = rs.getTimestamp("PLAYED_AT");
            entry.setPlayedAt(played == null ? null : played.toLocalDateTime());
            entry.setPlayedBy(rs.getString("PLAYED_BY"));
            entry.setResult(rs.getString("RESULT"));
            return entry;
        }
    };

    // -- alerts ---------------------------------------------------------------

    public List<Alert> findAlerts() {
        return jdbc.query("SELECT ALERT_ID, IFACE, RULE, SEVERITY, MESSAGE, RAISED_AT, ACK_FLAG, "
                + "ACK_BY, ACK_AT FROM IFALERT ORDER BY ACK_FLAG, RAISED_AT DESC", ALERT_MAPPER);
    }

    public Alert findAlert(String alertId) {
        List<Alert> rows = jdbc.query("SELECT ALERT_ID, IFACE, RULE, SEVERITY, MESSAGE, RAISED_AT, "
                        + "ACK_FLAG, ACK_BY, ACK_AT FROM IFALERT WHERE ALERT_ID = ?",
                ALERT_MAPPER, alertId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Alert findAlertByRule(String interfaceName, String rule) {
        List<Alert> rows = jdbc.query("SELECT ALERT_ID, IFACE, RULE, SEVERITY, MESSAGE, RAISED_AT, "
                        + "ACK_FLAG, ACK_BY, ACK_AT FROM IFALERT WHERE IFACE = ? AND RULE = ?",
                ALERT_MAPPER, interfaceName, rule);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertAlert(Alert alert) {
        jdbc.update("INSERT INTO IFALERT (ALERT_ID, IFACE, RULE, SEVERITY, MESSAGE, RAISED_AT, "
                        + "ACK_FLAG, ACK_BY, ACK_AT) VALUES (?,?,?,?,?,?,?,?,?)",
                alert.getAlertId(), alert.getInterfaceName(), alert.getRule(),
                alert.getSeverity().name(), alert.getMessage(),
                Timestamp.valueOf(alert.getRaisedAt()), alert.isAcknowledged() ? "Y" : "N",
                alert.getAcknowledgedBy(),
                alert.getAcknowledgedAt() == null ? null : Timestamp.valueOf(alert.getAcknowledgedAt()));
    }

    public void updateAlert(Alert alert) {
        jdbc.update("UPDATE IFALERT SET SEVERITY = ?, MESSAGE = ?, RAISED_AT = ?, ACK_FLAG = ?, "
                        + "ACK_BY = ?, ACK_AT = ? WHERE ALERT_ID = ?",
                alert.getSeverity().name(), alert.getMessage(),
                Timestamp.valueOf(alert.getRaisedAt()), alert.isAcknowledged() ? "Y" : "N",
                alert.getAcknowledgedBy(),
                alert.getAcknowledgedAt() == null ? null : Timestamp.valueOf(alert.getAcknowledgedAt()),
                alert.getAlertId());
    }

    public void acknowledge(String alertId, String username, LocalDateTime at) {
        jdbc.update("UPDATE IFALERT SET ACK_FLAG = 'Y', ACK_BY = ?, ACK_AT = ? WHERE ALERT_ID = ?",
                username, Timestamp.valueOf(at), alertId);
    }

    // -- replay queue ---------------------------------------------------------

    public List<ReplayEntry> findQueue() {
        return jdbc.query("SELECT QUEUE_ID, MSG_ID, IFACE, STATE, QUEUED_AT, PLAYED_AT, PLAYED_BY, "
                + "RESULT FROM IFQUEUE ORDER BY QUEUED_AT DESC", QUEUE_MAPPER);
    }

    public ReplayEntry findQueueEntry(String messageId) {
        List<ReplayEntry> rows = jdbc.query("SELECT QUEUE_ID, MSG_ID, IFACE, STATE, QUEUED_AT, "
                        + "PLAYED_AT, PLAYED_BY, RESULT FROM IFQUEUE WHERE MSG_ID = ?",
                QUEUE_MAPPER, messageId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertQueueEntry(ReplayEntry entry) {
        jdbc.update("INSERT INTO IFQUEUE (QUEUE_ID, MSG_ID, IFACE, STATE, QUEUED_AT, PLAYED_AT, "
                        + "PLAYED_BY, RESULT) VALUES (?,?,?,?,?,?,?,?)",
                entry.getQueueId(), entry.getMessageId(), entry.getInterfaceName(),
                entry.getState(), Timestamp.valueOf(entry.getQueuedAt()),
                entry.getPlayedAt() == null ? null : Timestamp.valueOf(entry.getPlayedAt()),
                entry.getPlayedBy(), entry.getResult());
    }

    public void updateQueueEntry(ReplayEntry entry) {
        jdbc.update("UPDATE IFQUEUE SET STATE = ?, PLAYED_AT = ?, PLAYED_BY = ?, RESULT = ? "
                        + "WHERE QUEUE_ID = ?",
                entry.getState(),
                entry.getPlayedAt() == null ? null : Timestamp.valueOf(entry.getPlayedAt()),
                entry.getPlayedBy(), entry.getResult(), entry.getQueueId());
    }
}
