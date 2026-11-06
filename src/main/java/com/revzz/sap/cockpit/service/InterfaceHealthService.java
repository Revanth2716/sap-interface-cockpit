package com.revzz.sap.cockpit.service;

import com.revzz.sap.cockpit.config.AppProperties;
import com.revzz.sap.cockpit.domain.HourlyBucket;
import com.revzz.sap.cockpit.domain.InterfaceDef;
import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.domain.SlaReport;
import com.revzz.sap.cockpit.repo.InterfaceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Interface health: the SLA picture and the traffic trend.
 *
 * <p>The aggregates are computed in Java rather than in SQL, because the
 * percentile function and the date bucketing differ between SQLite and
 * HANA. One behaviour that is easy to test is worth more than a query that
 * only runs on one engine.</p>
 */
@Service
public class InterfaceHealthService {

    private final InterfaceRepository repository;
    private final AppProperties properties;

    public InterfaceHealthService(InterfaceRepository repository, AppProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public List<InterfaceDef> interfaces() {
        return repository.findAllInterfaces();
    }

    public InterfaceDef interfaceDef(String name) {
        return repository.findInterface(name);
    }

    /** The start of the monitoring window. */
    public LocalDateTime windowStart() {
        return LocalDateTime.now().minusHours(properties.getMonitoring().getSlaWindowHours());
    }

    /** Every message inside the monitoring window. */
    public List<MessageRecord> messagesInWindow() {
        return repository.findMessagesSince(windowStart());
    }

    /** The messages of one interface inside the window. */
    public List<MessageRecord> messagesInWindow(String interfaceName) {
        return repository.findMessagesSince(interfaceName, windowStart());
    }

    /** The most recent messages, newest first, trimmed in Java. */
    public List<MessageRecord> recentMessages(String interfaceName, int limit) {
        List<MessageRecord> messages = new ArrayList<MessageRecord>(messagesInWindow(interfaceName));
        Collections.reverse(messages);
        return messages.size() > limit ? messages.subList(0, limit) : messages;
    }

    /** The SLA picture for every interface, worst first. */
    public List<SlaReport> slaReports() {
        List<SlaReport> reports = new ArrayList<SlaReport>();
        for (InterfaceDef definition : interfaces()) {
            reports.add(slaFor(definition, messagesInWindow(definition.getName())));
        }
        Collections.sort(reports, new Comparator<SlaReport>() {
            @Override
            public int compare(SlaReport left, SlaReport right) {
                int byRate = Double.compare(left.getSuccessRate(), right.getSuccessRate());
                return byRate != 0 ? byRate : left.getInterfaceName().compareTo(right.getInterfaceName());
            }
        });
        return reports;
    }

    /** The SLA picture for one interface. */
    public SlaReport slaFor(String interfaceName) {
        InterfaceDef definition = interfaceDef(interfaceName);
        if (definition == null) {
            return null;
        }
        return slaFor(definition, messagesInWindow(interfaceName));
    }

    private SlaReport slaFor(InterfaceDef definition, List<MessageRecord> messages) {
        SlaReport report = new SlaReport();
        report.setInterfaceName(definition.getName());
        report.setDescription(definition.getDescription());
        report.setType(definition.getType());
        report.setDirection(definition.getDirection());
        report.setOwner(definition.getOwner());
        report.setSystem(definition.getSystem());
        report.setTarget(definition.getTarget());
        report.setCritical(definition.isCritical());
        report.setSlaPercent(definition.getSlaPercent());
        report.setTotal(messages.size());

        int ok = 0;
        int errors = 0;
        int warnings = 0;
        long durationSum = 0;
        long max = 0;
        List<Long> durations = new ArrayList<Long>();
        LocalDateTime oldestFailure = null;

        for (MessageRecord message : messages) {
            if (message.isError()) {
                errors++;
                if (oldestFailure == null || message.getCreatedAt().isBefore(oldestFailure)) {
                    oldestFailure = message.getCreatedAt();
                }
            } else if (message.isWarning()) {
                warnings++;
            } else {
                ok++;
            }
            durationSum += message.getDurationMs();
            if (message.getDurationMs() > max) {
                max = message.getDurationMs();
            }
            durations.add((long) message.getDurationMs());
        }

        report.setOk(ok);
        report.setErrors(errors);
        report.setWarnings(warnings);
        report.setSuccessRate(messages.isEmpty()
                ? 100.0
                : Math.round(10000.0 * (messages.size() - errors) / messages.size()) / 100.0);
        report.setAverageMs(messages.isEmpty() ? 0 : durationSum / messages.size());
        report.setMaxMs(max);
        report.setP95Ms(percentile(durations, 95));
        if (oldestFailure != null) {
            report.setOldestFailureAgeHours(
                    (int) java.time.Duration.between(oldestFailure, LocalDateTime.now()).toHours());
        }
        return report;
    }

    /** The p95 of a duration list, computed the same way everywhere. */
    public long percentile(List<Long> values, int percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<Long>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= sorted.size()) {
            index = sorted.size() - 1;
        }
        return sorted.get(index);
    }

    /**
     * The hourly traffic trend of one interface.
     *
     * @param hours how many hours back to show
     */
    public List<HourlyBucket> hourlyTrend(String interfaceName, int hours) {
        LocalDateTime currentHour = LocalDateTime.now()
                .withMinute(0).withSecond(0).withNano(0);

        List<HourlyBucket> buckets = new ArrayList<HourlyBucket>();
        java.util.Map<LocalDateTime, HourlyBucket> byHour =
                new java.util.HashMap<LocalDateTime, HourlyBucket>();

        for (int offset = hours - 1; offset >= 0; offset--) {
            LocalDateTime hourStart = currentHour.minusHours(offset);
            HourlyBucket bucket = new HourlyBucket();
            bucket.setHour(hourStart.getHour());
            bucket.setLabel(String.format("%02d:00", hourStart.getHour()));
            buckets.add(bucket);
            byHour.put(hourStart, bucket);
        }

        List<MessageRecord> messages =
                repository.findMessagesSince(interfaceName, currentHour.minusHours(hours));
        for (MessageRecord message : messages) {
            if (message.getCreatedAt() == null) {
                continue;
            }
            LocalDateTime hourStart = message.getCreatedAt()
                    .withMinute(0).withSecond(0).withNano(0);
            HourlyBucket bucket = byHour.get(hourStart);
            if (bucket == null) {
                continue;
            }
            bucket.setTotal(bucket.getTotal() + 1);
            if (message.isError()) {
                bucket.setErrors(bucket.getErrors() + 1);
            } else if (message.isWarning()) {
                bucket.setWarnings(bucket.getWarnings() + 1);
            }
            bucket.setAverageMs(bucket.getAverageMs() + message.getDurationMs());
        }

        for (HourlyBucket bucket : buckets) {
            if (bucket.getTotal() > 0) {
                bucket.setAverageMs(bucket.getAverageMs() / bucket.getTotal());
            }
        }
        return buckets;
    }
}
