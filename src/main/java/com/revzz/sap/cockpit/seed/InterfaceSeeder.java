package com.revzz.sap.cockpit.seed;

import com.revzz.sap.cockpit.config.AppProperties;
import com.revzz.sap.cockpit.domain.InterfaceDef;
import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.repo.InterfaceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds the interface landscape and seven days of message traffic.
 *
 * <p>The traffic is generated, not random noise: every interface has a
 * profile, and the profiles tell a story an integration team would recognise.
 * One interface started failing hard at 02:00 yesterday with the same
 * authorisation error, one is slow but healthy, one fails intermittently,
 * and the rest are quiet. That is what makes the cockpit worth looking at -
 * a dashboard over random failures proves nothing.</p>
 *
 * <p>The random seed is fixed, so the dataset is identical on every start
 * and the tests can assert on it.</p>
 */
@Component
@Order(2)
public class InterfaceSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final InterfaceRepository repository;
    private final AppProperties properties;

    public InterfaceSeeder(JdbcTemplate jdbc, InterfaceRepository repository,
                           AppProperties properties) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedInterfaces();
        seedErrorCatalogue();
        seedTraffic();
    }

    private void seedUsers() {
        Object[][] users = {
                {"PRIYA.FI", "Priya Finance", "A", "FI"},
                {"ANIL.HR", "Anil HR", "A", "HR"},
                {"SARA.INV", "Sara Inventory", "A", "Inventory"},
                {"DEEP.INV", "Deep Inventory", "A", "Inventory"},
                {"RAJU.SALES", "Raju Sales", "A", "Sales"},
                {"DEVADMIN", "Dev Administrator", "A", "IT-Basis"},
        };
        for (Object[] user : users) {
            jdbc.update("INSERT INTO SAPUSER (USERID, FULLNAME, UFLAG, DEPT) VALUES (?,?,?,?)",
                    user[0], user[1], user[2], user[3]);
        }
    }

    private void seedInterfaces() {
        Object[][] definitions = {
                {"ZFI_VENDOR_RFC", "Vendor master posting (RFC)", "RFC", "OUT", "PRIYA.FI",
                        "PRD", "FI_VENDOR", 99, "Y"},
                {"ZHR_PAYROLL_RFC", "Payroll result transfer (RFC)", "RFC", "OUT", "ANIL.HR",
                        "PRD", "HR_PAYROLL", 99, "Y"},
                {"ZMDM_CUSTOMER_IDOC", "Customer master inbound (IDoc)", "IDOC", "IN", "SARA.INV",
                        "MDM", "PRD", 99, "N"},
                {"ZBANK_FILE_IDOC", "Bank statement file (IDoc)", "IDOC", "IN", "PRIYA.FI",
                        "BANKSYS", "PRD", 99, "Y"},
                {"ZINVENTORY_IDOC", "Goods movement (IDoc)", "IDOC", "IN", "DEEP.INV",
                        "PRD", "INV_ERP", 99, "N"},
                {"ZSALES_ORDER_BAPI", "Sales order creation (BAPI)", "BAPI", "OUT", "RAJU.SALES",
                        "PRD", "SD_ERP", 99, "N"},
                {"ZTAX_MANIFEST_RFC", "Tax manifest submission (RFC)", "RFC", "OUT", "PRIYA.FI",
                        "PRD", "TAX_SYS", 98, "N"},
                {"ZBASIS_TRANSPORT_RFC", "Transport delivery (RFC)", "RFC", "IN", "DEVADMIN",
                        "DEV", "BASIS_TR", 95, "N"},
        };
        for (Object[] definition : definitions) {
            jdbc.update("INSERT INTO IFDEF (IFACE, DESCRIPTION, IF_TYPE, DIRECTION, OWNER, SYSTEM, "
                            + "TARGET, SLA_PCT, CRITICAL) VALUES (?,?,?,?,?,?,?,?,?)",
                    definition[0], definition[1], definition[2], definition[3], definition[4],
                    definition[5], definition[6], definition[7], definition[8]);
        }
    }

    private void seedErrorCatalogue() {
        Object[][] errors = {
                {"ERR_AUTH_403", "CRITICAL",
                        "The target system rejected the call - the service user has no authorisation "
                                + "for the target.",
                        "Grant the missing authorisation in SU01 (S_RFC / S_BAPI) and check the "
                                + "authorisations of the RFC destination in SM59.",
                        "Y"},
                {"ERR_TIMEOUT", "MEDIUM",
                        "The RFC call exceeded the timeout configured on the destination.",
                        "Check SM50 and SM66 for a blocked work process, then raise the timeout "
                                + "parameter on the RFC destination in SM59.",
                        "Y"},
                {"ERR_LOCK_OBJECT", "MEDIUM",
                        "The target object was locked by an open business transaction.",
                        "Find the lock in SM12 and release it once the owning transaction has "
                                + "finished - retrying blindly only makes it worse.",
                        "Y"},
                {"ERR_FIELD_MAPPING", "HIGH",
                        "The IDoc segment does not match the structure of the target.",
                        "Compare the IDoc structure with the target schema in WE02 and correct the "
                                + "mapping in the middleware.",
                        "Y"},
                {"ERR_NO_ROUTE", "HIGH",
                        "The destination is not in the routing table.",
                        "Check the routing table in SM56 and the destination configuration.",
                        "N"},
                {"ERR_DUP_KEY", "MEDIUM",
                        "The message was rejected as a duplicate key on the target table.",
                        "Make the posting idempotent - look up the key and skip an existing record "
                                + "instead of failing the message.",
                        "Y"},
                {"ERR_MANDANT", "HIGH",
                        "The target client does not exist, or the client does not match.",
                        "Check the client in the destination configuration and the MANDT field "
                                + "in the IDoc.",
                        "N"},
                {"WARN_SLOW", "LOW",
                        "The call succeeded but the response time is above the interface threshold.",
                        "Check ST12 for the slow SQL statement and consider the index or the amount "
                                + "of data being transferred.",
                        "Y"},
        };
        for (Object[] error : errors) {
            jdbc.update("INSERT INTO IFERR (ERR_CODE, SEVERITY, CAUSE, REMEDIATION, COMMON) "
                    + "VALUES (?,?,?,?,?)",
                    error[0], error[1], error[2], error[3], error[4]);
        }
    }

    /**
     * Generates the traffic.
     *
     * <p>Every message is written with an explicit column list, so the
     * column order can never drift from the values.</p>
     */
    private void seedTraffic() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(properties.getSeed().getDays());
        // The incident: the bank file interface has been failing since 02:00.
        LocalDateTime incidentStart = now.minusDays(1).withHour(2).withMinute(0)
                .withSecond(0).withNano(0);

        Random random = new Random(properties.getSeed().getRandomSeed());
        List<MessageRecord> messages = new ArrayList<MessageRecord>();
        int counter = 0;

        long totalMinutes = java.time.Duration.between(start, now).toMinutes();
        int messageCount = properties.getSeed().getMessagesPerDay()
                * properties.getSeed().getDays();

        for (int index = 0; index < messageCount; index++) {
            LocalDateTime moment = start.plusMinutes(
                    (long) (random.nextDouble() * totalMinutes));
            if (moment.isAfter(now)) {
                moment = now.minusMinutes(random.nextInt(60));
            }

            String[] profile = pickProfile(random, moment, incidentStart);
            counter++;

            MessageRecord record = new MessageRecord();
            record.setMessageId("MSG" + String.format("%06d", counter));
            record.setInterfaceName(profile[0]);
            record.setInterfaceName(profile[0]);
            record.setMessageType(profile[1]);
            record.setDirection(profile[2]);
            record.setStatus(profile[3]);
            record.setErrorCode(profile[4].isEmpty() ? null : profile[4]);
            record.setErrorText(profile[5].isEmpty() ? null : profile[5]);
            record.setDurationMs(Integer.parseInt(profile[6]));
            record.setHost(profile[7]);
            record.setCorrelationId(correlationId(random));
            record.setCreatedAt(moment);
            messages.add(record);
        }

        jdbc.batchUpdate("INSERT INTO IFLOG (MSG_ID, IFACE, MSG_TYPE, DIRECTION, STATUS, ERR_CODE, "
                        + "ERR_TEXT, DURATION_MS, HOST, CORR_ID, CREATED_AT) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                messages,
                500,
                (ps, record) -> {
                    ps.setString(1, record.getMessageId());
                    ps.setString(2, record.getInterfaceName());
                    ps.setString(3, record.getMessageType());
                    ps.setString(4, record.getDirection());
                    ps.setString(5, record.getStatus());
                    ps.setString(6, record.getErrorCode());
                    ps.setString(7, record.getErrorText());
                    ps.setInt(8, record.getDurationMs());
                    ps.setString(9, record.getHost());
                    ps.setString(10, record.getCorrelationId());
                    ps.setTimestamp(11, Timestamp.valueOf(record.getCreatedAt()));
                });
    }

    /**
     * Chooses the interface and the outcome of one message.
     *
     * <p>The interface is picked first, by volume; the outcome then follows
     * that interface's own profile. Doing it the other way round would make
     * a failing interface look busy for the wrong reason.</p>
     *
     * @return { interface, type, direction, status, errorCode, errorText, duration, host }
     */
    private String[] pickProfile(Random random, LocalDateTime moment, LocalDateTime incidentStart) {
        int roll = random.nextInt(100);

        if (roll < 20) {
            return vendor(random);
        }
        if (roll < 34) {
            return payroll(random);
        }
        if (roll < 48) {
            return sales(random);
        }
        if (roll < 58) {
            return mdm(random);
        }
        if (roll < 70) {
            return inventory(random);
        }
        if (roll < 76) {
            return tax(random);
        }
        if (roll < 84) {
            return bank(random, moment, incidentStart);
        }
        return basis(random);
    }

    private String[] vendor(Random random) {
        if (random.nextInt(100) < 3) {
            return new String[]{"ZFI_VENDOR_RFC", "RFC", "OUT", "ERROR", "ERR_TIMEOUT",
                    "RFC call exceeded the timeout configured on the destination",
                    String.valueOf(60000 + random.nextInt(30000)), "prdapp01"};
        }
        return new String[]{"ZFI_VENDOR_RFC", "RFC", "OUT", "OK", "", "",
                String.valueOf(180 + random.nextInt(700)), "prdapp01"};
    }

    private String[] payroll(Random random) {
        return new String[]{"ZHR_PAYROLL_RFC", "RFC", "OUT", "OK", "", "",
                String.valueOf(220 + random.nextInt(900)), "prdapp02"};
    }

    /** Healthy, but slow - a latency breach rather than an error. */
    private String[] sales(Random random) {
        return new String[]{"ZSALES_ORDER_BAPI", "BAPI", "OUT", "OK", "", "",
                String.valueOf(2700 + random.nextInt(1800)), "sdapp01"};
    }

    private String[] mdm(Random random) {
        int roll = random.nextInt(100);
        if (roll < 12) {
            return new String[]{"ZMDM_CUSTOMER_IDOC", "IDOC", "IN", "ERROR", "ERR_DUP_KEY",
                    "Customer record rejected as a duplicate key on the target",
                    String.valueOf(400 + random.nextInt(1600)), "mdmapp02"};
        }
        if (roll < 18) {
            return new String[]{"ZMDM_CUSTOMER_IDOC", "IDOC", "IN", "ERROR", "ERR_FIELD_MAPPING",
                    "IDoc segment does not match the target structure",
                    String.valueOf(400 + random.nextInt(1600)), "mdmapp02"};
        }
        return new String[]{"ZMDM_CUSTOMER_IDOC", "IDOC", "IN", "OK", "", "",
                String.valueOf(300 + random.nextInt(1200)), "mdmapp02"};
    }

    private String[] inventory(Random random) {
        int roll = random.nextInt(100);
        if (roll < 8) {
            return new String[]{"ZINVENTORY_IDOC", "IDOC", "IN", "ERROR", "ERR_LOCK_OBJECT",
                    "Goods movement blocked by an open inventory posting",
                    String.valueOf(250 + random.nextInt(900)), "invapp01"};
        }
        if (roll < 13) {
            return new String[]{"ZINVENTORY_IDOC", "IDOC", "IN", "ERROR", "ERR_FIELD_MAPPING",
                    "IDoc segment does not match the target structure",
                    String.valueOf(250 + random.nextInt(900)), "invapp01"};
        }
        return new String[]{"ZINVENTORY_IDOC", "IDOC", "IN", "OK", "", "",
                String.valueOf(200 + random.nextInt(800)), "invapp01"};
    }

    private String[] tax(Random random) {
        if (random.nextInt(100) < 6) {
            return new String[]{"ZTAX_MANIFEST_RFC", "RFC", "OUT", "ERROR", "ERR_NO_ROUTE",
                    "No route to the destination in the routing table",
                    String.valueOf(150 + random.nextInt(400)), "prdapp01"};
        }
        return new String[]{"ZTAX_MANIFEST_RFC", "RFC", "OUT", "OK", "", "",
                String.valueOf(180 + random.nextInt(500)), "prdapp01"};
    }

    /**
     * The bank file interface.
     *
     * <p>Before 02:00 it was a quiet little interface. Since 02:00 every
     * single call has failed with the same authorisation error - which is
     * exactly the shape of a real incident, and exactly what the triage
     * screen is supposed to turn into one sentence.</p>
     */
    private String[] bank(Random random, LocalDateTime moment, LocalDateTime incidentStart) {
        if (moment.isAfter(incidentStart)) {
            return new String[]{"ZBANK_FILE_IDOC", "IDOC", "IN", "ERROR", "ERR_AUTH_403",
                    "Authorisation missing on the target system for service user FI_INTF",
                    String.valueOf(300 + random.nextInt(700)), "banksys01"};
        }
        if (random.nextInt(100) < 5) {
            return new String[]{"ZBANK_FILE_IDOC", "IDOC", "IN", "WARNING", "WARN_SLOW",
                    "Bank file processed, but the acknowledgement took longer than the threshold",
                    String.valueOf(2600 + random.nextInt(1500)), "banksys01"};
        }
        return new String[]{"ZBANK_FILE_IDOC", "IDOC", "IN", "OK", "", "",
                String.valueOf(400 + random.nextInt(900)), "banksys01"};
    }

    private String[] basis(Random random) {
        if (random.nextInt(100) < 25) {
            return new String[]{"ZBASIS_TRANSPORT_RFC", "RFC", "IN", "WARNING", "WARN_SLOW",
                    "Transport delivered, but the import ran longer than the threshold",
                    String.valueOf(2800 + random.nextInt(1400)), "devapp01"};
        }
        return new String[]{"ZBASIS_TRANSPORT_RFC", "RFC", "IN", "OK", "", "",
                String.valueOf(300 + random.nextInt(800)), "devapp01"};
    }

    private String correlationId(Random random) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < 12; index++) {
            builder.append(Integer.toHexString(random.nextInt(16)));
        }
        return builder.toString();
    }
}
