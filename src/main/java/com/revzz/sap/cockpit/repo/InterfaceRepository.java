package com.revzz.sap.cockpit.repo;

import com.revzz.sap.cockpit.domain.InterfaceDef;
import com.revzz.sap.cockpit.domain.MessageRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The interface catalogue and the message log.
 *
 * <p>Every statement is portable ANSI SQL, so the same repository runs
 * against the local H2 database and against a live SAP HANA instance. Note
 * that no statement uses {@code LIMIT} - that syntax is not portable - so
 * result sets are trimmed in Java instead.</p>
 */
@Repository
public class InterfaceRepository {

    private final JdbcTemplate jdbc;

    public InterfaceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<InterfaceDef> DEF_MAPPER = new RowMapper<InterfaceDef>() {
        @Override
        public InterfaceDef mapRow(ResultSet rs, int rowNum) throws SQLException {
            InterfaceDef definition = new InterfaceDef();
            definition.setName(rs.getString("IFACE"));
            definition.setDescription(rs.getString("DESCRIPTION"));
            definition.setType(rs.getString("IF_TYPE"));
            definition.setDirection(rs.getString("DIRECTION"));
            definition.setOwner(rs.getString("OWNER"));
            definition.setSystem(rs.getString("SYSTEM"));
            definition.setTarget(rs.getString("TARGET"));
            definition.setSlaPercent(rs.getInt("SLA_PCT"));
            definition.setCritical("Y".equalsIgnoreCase(rs.getString("CRITICAL")));
            return definition;
        }
    };

    private static final RowMapper<MessageRecord> MSG_MAPPER = new RowMapper<MessageRecord>() {
        @Override
        public MessageRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            MessageRecord record = new MessageRecord();
            record.setMessageId(rs.getString("MSG_ID"));
            record.setInterfaceName(rs.getString("IFACE"));
            record.setMessageType(rs.getString("MSG_TYPE"));
            record.setDirection(rs.getString("DIRECTION"));
            record.setStatus(rs.getString("STATUS"));
            record.setErrorCode(rs.getString("ERR_CODE"));
            record.setErrorText(rs.getString("ERR_TEXT"));
            record.setDurationMs(rs.getInt("DURATION_MS"));
            record.setHost(rs.getString("HOST"));
            record.setCorrelationId(rs.getString("CORR_ID"));
            Timestamp created = rs.getTimestamp("CREATED_AT");
            record.setCreatedAt(created == null ? null : created.toLocalDateTime());
            return record;
        }
    };

    // -- catalogue ------------------------------------------------------------

    public List<InterfaceDef> findAllInterfaces() {
        return jdbc.query("SELECT IFACE, DESCRIPTION, IF_TYPE, DIRECTION, OWNER, SYSTEM, TARGET, "
                + "SLA_PCT, CRITICAL FROM IFDEF ORDER BY IFACE", DEF_MAPPER);
    }

    public InterfaceDef findInterface(String name) {
        List<InterfaceDef> rows = jdbc.query("SELECT IFACE, DESCRIPTION, IF_TYPE, DIRECTION, OWNER, "
                        + "SYSTEM, TARGET, SLA_PCT, CRITICAL FROM IFDEF WHERE IFACE = ?",
                DEF_MAPPER, name);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // -- message log ----------------------------------------------------------

    /** Every message inside the window, oldest first. */
    public List<MessageRecord> findMessagesSince(LocalDateTime since) {
        return jdbc.query("SELECT MSG_ID, IFACE, MSG_TYPE, DIRECTION, STATUS, ERR_CODE, ERR_TEXT, "
                        + "DURATION_MS, HOST, CORR_ID, CREATED_AT FROM IFLOG "
                        + "WHERE CREATED_AT >= ? ORDER BY CREATED_AT",
                MSG_MAPPER, Timestamp.valueOf(since));
    }

    /** The messages of one interface inside the window. */
    public List<MessageRecord> findMessagesSince(String interfaceName, LocalDateTime since) {
        return jdbc.query("SELECT MSG_ID, IFACE, MSG_TYPE, DIRECTION, STATUS, ERR_CODE, ERR_TEXT, "
                        + "DURATION_MS, HOST, CORR_ID, CREATED_AT FROM IFLOG "
                        + "WHERE IFACE = ? AND CREATED_AT >= ? ORDER BY CREATED_AT",
                MSG_MAPPER, interfaceName, Timestamp.valueOf(since));
    }

    public MessageRecord findMessage(String messageId) {
        List<MessageRecord> rows = jdbc.query("SELECT MSG_ID, IFACE, MSG_TYPE, DIRECTION, STATUS, "
                        + "ERR_CODE, ERR_TEXT, DURATION_MS, HOST, CORR_ID, CREATED_AT FROM IFLOG "
                        + "WHERE MSG_ID = ?",
                MSG_MAPPER, messageId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertMessage(MessageRecord record) {
        jdbc.update("INSERT INTO IFLOG (MSG_ID, IFACE, MSG_TYPE, DIRECTION, STATUS, ERR_CODE, "
                        + "ERR_TEXT, DURATION_MS, HOST, CORR_ID, CREATED_AT) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                record.getMessageId(), record.getInterfaceName(), record.getMessageType(),
                record.getDirection(), record.getStatus(), record.getErrorCode(),
                record.getErrorText(), record.getDurationMs(), record.getHost(),
                record.getCorrelationId(), Timestamp.valueOf(record.getCreatedAt()));
    }
}
