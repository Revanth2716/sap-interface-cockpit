package com.revzz.sap.cockpit.service;

import com.revzz.sap.cockpit.domain.ErrorCluster;
import com.revzz.sap.cockpit.domain.ErrorCode;
import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.domain.Severity;
import com.revzz.sap.cockpit.repo.ErrorCatalogueRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Error triage.
 *
 * <p>A wall of failed messages is not an answer. "One interface, one error
 * code, forty times overnight" is an answer - and that is what this service
 * produces, enriched with the cause and the remediation from the error
 * catalogue.</p>
 */
@Service
public class ErrorTriageService {

    private final InterfaceHealthService healthService;
    private final ErrorCatalogueRepository catalogue;

    public ErrorTriageService(InterfaceHealthService healthService,
                               ErrorCatalogueRepository catalogue) {
        this.healthService = healthService;
        this.catalogue = catalogue;
    }

    /**
     * Group the failures in the window by interface and error code.
     *
     * <p>Sorted by severity, then by how many occurrences there are - the
     * biggest problem first.</p>
     */
    public List<ErrorCluster> clusters() {
        Map<String, List<MessageRecord>> grouped =
                new LinkedHashMap<String, List<MessageRecord>>();

        for (MessageRecord message : healthService.messagesInWindow()) {
            if (!message.isError() || message.getErrorCode() == null) {
                continue;
            }
            String key = message.getInterfaceName() + "|" + message.getErrorCode();
            List<MessageRecord> bucket = grouped.get(key);
            if (bucket == null) {
                bucket = new ArrayList<MessageRecord>();
                grouped.put(key, bucket);
            }
            bucket.add(message);
        }

        List<ErrorCluster> clusters = new ArrayList<ErrorCluster>();
        for (Map.Entry<String, List<MessageRecord>> entry : grouped.entrySet()) {
            List<MessageRecord> messages = entry.getValue();
            MessageRecord first = messages.get(0);

            ErrorCluster cluster = new ErrorCluster();
            cluster.setInterfaceName(first.getInterfaceName());
            cluster.setErrorCode(first.getErrorCode());
            cluster.setErrorText(first.getErrorText());
            cluster.setOccurrences(messages.size());

            Set<String> hosts = new LinkedHashSet<String>();
            long firstSeen = Long.MAX_VALUE;
            long lastSeen = 0;
            for (MessageRecord message : messages) {
                hosts.add(message.getHost());
                long epoch = message.getCreatedAt() == null
                        ? 0 : message.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
                firstSeen = Math.min(firstSeen, epoch);
                lastSeen = Math.max(lastSeen, epoch);
            }
            cluster.setDistinctHosts(hosts.size());
            cluster.setFirstSeenEpochMs(firstSeen == Long.MAX_VALUE ? 0 : firstSeen);
            cluster.setLastSeenEpochMs(lastSeen);

            ErrorCode known = catalogue.findByCode(first.getErrorCode());
            if (known != null) {
                cluster.setCause(known.getCause());
                cluster.setRemediation(known.getRemediation());
                cluster.setSeverity(known.getSeverity());
            } else {
                cluster.setCause("Not in the error catalogue.");
                cluster.setRemediation("Capture the message in the middleware log and add an entry "
                        + "to the catalogue.");
                cluster.setSeverity(Severity.MEDIUM);
            }

            List<MessageRecord> interfaceMessages =
                    healthService.messagesInWindow(first.getInterfaceName());
            int total = interfaceMessages.size();
            int errors = 0;
            for (MessageRecord message : interfaceMessages) {
                if (message.isError()) {
                    errors++;
                }
            }
            cluster.setSuccessRate(total == 0
                    ? 100.0
                    : Math.round(10000.0 * (total - errors) / total) / 100.0);
            clusters.add(cluster);
        }

        Collections.sort(clusters, new Comparator<ErrorCluster>() {
            @Override
            public int compare(ErrorCluster left, ErrorCluster right) {
                Severity leftSeverity = left.getSeverity() == null ? Severity.LOW : left.getSeverity();
                Severity rightSeverity = right.getSeverity() == null ? Severity.LOW : right.getSeverity();
                int bySeverity = leftSeverity.rank() - rightSeverity.rank();
                if (bySeverity != 0) {
                    return bySeverity;
                }
                int byCount = right.getOccurrences() - left.getOccurrences();
                return byCount != 0
                        ? byCount
                        : left.getInterfaceName().compareTo(right.getInterfaceName());
            }
        });
        return clusters;
    }

    /** One cluster: a single interface and a single error code. */
    public ErrorCluster cluster(String interfaceName, String errorCode) {
        for (ErrorCluster cluster : clusters()) {
            if (cluster.getInterfaceName().equals(interfaceName)
                    && cluster.getErrorCode().equals(errorCode)) {
                return cluster;
            }
        }
        return null;
    }

    /** The error catalogue, for the reference screen. */
    public List<ErrorCode> catalogue() {
        return catalogue.findAll();
    }

    public ErrorCode lookup(String code) {
        return catalogue.findByCode(code);
    }
}
