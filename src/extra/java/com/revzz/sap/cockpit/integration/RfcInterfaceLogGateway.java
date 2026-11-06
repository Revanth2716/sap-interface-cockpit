package com.revzz.sap.cockpit.integration;

import com.revzz.sap.cockpit.domain.MessageRecord;
import com.sap.jco.JCoDestination;
import com.sap.jco.JCoException;
import com.sap.jco.JCoFunction;
import com.sap.jco.JCoTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Live SAP gateway - RFC through the SAP Java Connector.
 *
 * <p><b>Only compiled when the {@code with-jco} Maven profile is active</b>,
 * because the SAP Java Connector is published on SAP's own Maven repository
 * rather than on Maven Central. The default build therefore compiles and
 * tests anywhere.</p>
 *
 * <pre>
 *   mvn -Pwith-jco clean package
 * </pre>
 *
 * <p>The connection uses an RFC destination maintained in SM59, so the same
 * build works against DEV, QA and PRD and the BASIS team keeps the
 * connection parameters in one place.</p>
 */
@Component
@Primary
public class RfcInterfaceLogGateway {

    private static final Logger LOG = LoggerFactory.getLogger(RfcInterfaceLogGateway.class);

    /** RFC-enabled ABAP function module that reads the interface message log. */
    private static final String FUNCTION_IF_LOG_READ = "Z_IF_LOG_READ";

    private final String destinationName;

    public RfcInterfaceLogGateway() {
        this("SAPDEV");
    }

    public RfcInterfaceLogGateway(String destinationName) {
        this.destinationName = destinationName;
    }

    /** Reads the interface message log from a live system. */
    public List<MessageRecord> fetchMessages(String interfaceName, int hours) throws JCoException {
        List<MessageRecord> messages = new ArrayList<MessageRecord>();

        JCoDestination destination = buildDestination();
        JCoFunction function = JCoFunction.getInstance(destination, FUNCTION_IF_LOG_READ);
        function.getParameter("IV_INTERFACE").setString(interfaceName == null ? "" : interfaceName);
        function.getParameter("IV_HOURS").setInt(hours);
        function.execute(destination);

        JCoTable table = function.getTable("IT_MESSAGES");
        if (table == null) {
            return messages;
        }
        table.first();
        while (!table.isEof()) {
            MessageRecord record = new MessageRecord();
            record.setMessageId(table.getField("MSG_ID").getString());
            record.setInterfaceName(table.getField("IFACE").getString());
            record.setMessageType(table.getField("MSG_TYPE").getString());
            record.setStatus(table.getField("STATUS").getString());
            record.setErrorCode(table.getField("ERR_CODE").getString());
            record.setDurationMs(table.getField("DURATION_MS").getInt());
            record.setHost(table.getField("HOST").getString());
            record.setCorrelationId(table.getField("CORR_ID").getString());
            record.setCreatedAt(toLocalDateTime(table.getField("CREATED_AT").getString()));
            messages.add(record);
            table.next();
        }
        LOG.info("Fetched {} interface messages over RFC", messages.size());
        return messages;
    }

    private LocalDateTime toLocalDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim().substring(0, 19).replace(' ', 'T'));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private JCoDestination buildDestination() throws JCoException {
        Properties properties = new Properties();
        try {
            java.io.InputStream stream = new java.io.FileInputStream("jco.properties");
            try {
                properties.load(stream);
            } finally {
                stream.close();
            }
        } catch (java.io.IOException exception) {
            throw new JCoException("jco.properties not found - copy jco.properties.example");
        }

        JCoDestination destination = JCoDestination.createDestination(destinationName);
        destination.setApplicationServer(properties.getProperty("sap.applicationServer"));
        destination.setSystemNumber(properties.getProperty("sap.systemNumber"));
        destination.setClient(properties.getProperty("sap.client"));
        destination.setUser(properties.getProperty("sap.user"));
        destination.setPassword(properties.getProperty("sap.password"));
        destination.setPoolSize(2);
        return destination;
    }
}
