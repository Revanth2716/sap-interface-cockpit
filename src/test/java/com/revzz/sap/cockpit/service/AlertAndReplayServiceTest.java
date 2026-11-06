package com.revzz.sap.cockpit.service;

import com.revzz.sap.cockpit.domain.Alert;
import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.domain.ReplayEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AlertAndReplayServiceTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private ReplayService replayService;

    @Autowired
    private InterfaceHealthService healthService;

    // -- alerting -------------------------------------------------------------

    @Test
    @DisplayName("the failure burst rule fires for the broken interface")
    void raisesFailureBurst() {
        alertService.evaluate();

        List<Alert> alerts = alertService.list();
        boolean found = false;
        for (Alert alert : alerts) {
            if ("ZBANK_FILE_IDOC".equals(alert.getInterfaceName())
                    && Alert.RULE_FAILURE_BURST.equals(alert.getRule())) {
                found = true;
                assertThat(alert.getSeverity().rank()).isEqualTo(1);
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("the latency rule fires for the slow interface")
    void raisesLatencyBreach() {
        alertService.evaluate();

        boolean found = false;
        for (Alert alert : alertService.list()) {
            if ("ZSALES_ORDER_BAPI".equals(alert.getInterfaceName())
                    && Alert.RULE_LATENCY_BREACH.equals(alert.getRule())) {
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("evaluating twice refreshes the alerts instead of duplicating them")
    void doesNotDuplicateAlerts() {
        alertService.evaluate();
        alertService.evaluate();

        int failures = 0;
        for (Alert alert : alertService.list()) {
            if ("ZBANK_FILE_IDOC".equals(alert.getInterfaceName())
                    && Alert.RULE_FAILURE_BURST.equals(alert.getRule())) {
                failures++;
            }
        }
        assertThat(failures).isEqualTo(1);
    }

    @Test
    @DisplayName("an alert can be acknowledged")
    void acknowledgesAlert() {
        alertService.evaluate();
        Alert alert = alertService.open().get(0);

        alertService.acknowledge(alert.getAlertId(), "ops.lead");

        Alert reloaded = null;
        for (Alert candidate : alertService.list()) {
            if (candidate.getAlertId().equals(alert.getAlertId())) {
                reloaded = candidate;
            }
        }
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.isAcknowledged()).isTrue();
        assertThat(reloaded.getAcknowledgedBy()).isEqualTo("ops.lead");
    }

    @Test
    @DisplayName("acknowledging an unknown alert is rejected")
    void rejectsUnknownAlert() {
        assertThatThrownBy(() -> alertService.acknowledge("AL-NOPE", "ops.lead"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -- replay queue ---------------------------------------------------------

    @Test
    @DisplayName("a failed message can be queued")
    void queuesAFailedMessage() {
        // A message no other test touches, so the assertion cannot depend on
        // the order the test methods run in.
        MessageRecord failed = firstFailedMessage("ERR_FIELD_MAPPING");

        ReplayEntry entry = replayService.queue(failed.getMessageId(), "ops.lead");

        assertThat(entry).isNotNull();
        assertThat(entry.getState()).isEqualTo(ReplayEntry.STATE_QUEUED);
        assertThat(entry.getInterfaceName()).isEqualTo(failed.getInterfaceName());
    }

    @Test
    @DisplayName("queueing the same message twice returns the same entry")
    void doesNotQueueTwice() {
        MessageRecord failed = firstFailedMessage("ERR_FIELD_MAPPING");

        ReplayEntry first = replayService.queue(failed.getMessageId(), "ops.lead");
        ReplayEntry second = replayService.queue(failed.getMessageId(), "ops.lead");

        assertThat(second.getQueueId()).isEqualTo(first.getQueueId());
    }

    @Test
    @DisplayName("a transient failure succeeds when it is retried")
    void transientErrorSucceedsOnReplay() {
        MessageRecord failed = firstFailedMessage("ERR_TIMEOUT");
        ReplayEntry entry = replayService.queue(failed.getMessageId(), "ops.lead");

        ReplayEntry played = replayService.play(entry.getQueueId(), "ops.lead");

        assertThat(played.getState()).isEqualTo(ReplayEntry.STATE_PLAYED);
        assertThat(played.getResult()).containsIgnoringCase("successfully");
        assertThat(played.getPlayedBy()).isEqualTo("ops.lead");
    }

    @Test
    @DisplayName("a configuration cause fails again when it is retried")
    void configurationErrorFailsAgainOnReplay() {
        MessageRecord failed = firstFailedMessage("ERR_AUTH_403");
        ReplayEntry entry = replayService.queue(failed.getMessageId(), "ops.lead");

        ReplayEntry played = replayService.play(entry.getQueueId(), "ops.lead");

        assertThat(played.getState()).isEqualTo(ReplayEntry.STATE_FAILED);
        assertThat(played.getResult()).containsIgnoringCase("needs a fix, not a retry");
    }

    @Test
    @DisplayName("a successful message cannot be queued")
    void cannotQueueASuccessfulMessage() {
        String found = null;
        for (MessageRecord message : healthService.messagesInWindow()) {
            if (message.isOk()) {
                found = message.getMessageId();
                break;
            }
        }
        assertThat(found).isNotNull();
        final String messageId = found;

        assertThatThrownBy(() -> replayService.queue(messageId, "ops.lead"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nothing to replay");
    }

    @Test
    @DisplayName("an unknown message cannot be queued")
    void cannotQueueAnUnknownMessage() {
        assertThatThrownBy(() -> replayService.queue("MSG-NOPE", "ops.lead"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private MessageRecord firstFailedMessage(String errorCode) {
        for (MessageRecord message : healthService.messagesInWindow()) {
            if (message.isError() && errorCode.equals(message.getErrorCode())) {
                return message;
            }
        }
        throw new AssertionError("no failed message with " + errorCode + " in the window");
    }
}
