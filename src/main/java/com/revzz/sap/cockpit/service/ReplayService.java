package com.revzz.sap.cockpit.service;

import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.domain.ReplayEntry;
import com.revzz.sap.cockpit.repo.InterfaceRepository;
import com.revzz.sap.cockpit.repo.OperationsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The replay queue.
 *
 * <p>Re-processing a failed message is what an integration team does at
 * 07:00 after a night-time outage. Doing it by hand is slow and
 * untraceable, so the queue keeps the attempt, who made it and what
 * happened.</p>
 *
 * <p><b>On the simulation:</b> this project cannot call a customer system,
 * so the replay is simulated - but it is simulated honestly. An error caused
 * by a transient condition (a timeout, a lock) succeeds when it is retried,
 * because that is what actually happens; an error caused by configuration
 * fails again, because that is what actually happens too. That is why the
 * queue is useful as a triage instrument and not just a to-do list.</p>
 */
@Service
public class ReplayService {

    /** Errors caused by a condition that clears by itself. */
    private static final Set<String> TRANSIENT_ERRORS = new HashSet<String>(Arrays.asList(
            "ERR_TIMEOUT", "ERR_LOCK_OBJECT"));

    private final OperationsRepository operations;
    private final InterfaceRepository interfaces;

    public ReplayService(OperationsRepository operations, InterfaceRepository interfaces) {
        this.operations = operations;
        this.interfaces = interfaces;
    }

    public List<ReplayEntry> queue() {
        return operations.findQueue();
    }

    /**
     * Queue a failed message for re-processing.
     *
     * @return the queue entry, existing or new
     */
    public ReplayEntry queue(String messageId, String username) {
        ReplayEntry existing = operations.findQueueEntry(messageId);
        if (existing != null) {
            return existing;
        }

        MessageRecord message = interfaces.findMessage(messageId);
        if (message == null) {
            throw new IllegalArgumentException("Message " + messageId + " was not found.");
        }
        if (!message.isError()) {
            throw new IllegalArgumentException(
                    "Message " + messageId + " succeeded - there is nothing to replay.");
        }

        ReplayEntry entry = new ReplayEntry();
        entry.setQueueId("RQ" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        entry.setMessageId(messageId);
        entry.setInterfaceName(message.getInterfaceName());
        entry.setState(ReplayEntry.STATE_QUEUED);
        entry.setQueuedAt(LocalDateTime.now());
        operations.insertQueueEntry(entry);
        return entry;
    }

    /**
     * Re-process a queued message.
     *
     * @return the queue entry after the attempt
     */
    public ReplayEntry play(String queueId, String username) {
        ReplayEntry entry = null;
        for (ReplayEntry candidate : operations.findQueue()) {
            if (candidate.getQueueId().equals(queueId)) {
                entry = candidate;
                break;
            }
        }
        if (entry == null) {
            throw new IllegalArgumentException("Queue entry " + queueId + " was not found.");
        }

        MessageRecord message = interfaces.findMessage(entry.getMessageId());
        String errorCode = message == null ? null : message.getErrorCode();

        if (errorCode != null && TRANSIENT_ERRORS.contains(errorCode)) {
            entry.setState(ReplayEntry.STATE_PLAYED);
            entry.setResult("Re-processed successfully - the condition that caused "
                    + errorCode + " had cleared.");
        } else {
            entry.setState(ReplayEntry.STATE_FAILED);
            entry.setResult("Re-processing failed with the same error: " + errorCode
                    + ". This cause needs a fix, not a retry.");
        }
        entry.setPlayedAt(LocalDateTime.now());
        entry.setPlayedBy(username);
        operations.updateQueueEntry(entry);
        return entry;
    }

    /** The queued entries that have not been played yet. */
    public List<ReplayEntry> pending() {
        java.util.List<ReplayEntry> pending = new java.util.ArrayList<ReplayEntry>();
        for (ReplayEntry entry : operations.findQueue()) {
            if (ReplayEntry.STATE_QUEUED.equals(entry.getState())) {
                pending.add(entry);
            }
        }
        return pending;
    }
}
