package com.revzz.sap.cockpit.service;

import com.revzz.sap.cockpit.domain.HourlyBucket;
import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.domain.SlaReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class InterfaceHealthServiceTest {

    @Autowired
    private InterfaceHealthService healthService;

    @Test
    @DisplayName("the catalogue holds every interface")
    void listsTheInterfaces() {
        assertThat(healthService.interfaces()).hasSize(8);
        assertThat(healthService.interfaces())
                .extracting(definition -> definition.getName())
                .contains("ZBANK_FILE_IDOC", "ZSALES_ORDER_BAPI", "ZHR_PAYROLL_RFC");
    }

    @Test
    @DisplayName("the failing interface breaches its SLA")
    void failingInterfaceBreachesSla() {
        SlaReport report = healthService.slaFor("ZBANK_FILE_IDOC");

        assertThat(report).isNotNull();
        assertThat(report.getErrors()).isGreaterThan(0);
        assertThat(report.getSuccessRate()).isLessThan(report.getSlaPercent());
        assertThat(report.breaching()).isTrue();
    }

    @Test
    @DisplayName("a healthy interface does not breach its SLA")
    void healthyInterfaceDoesNotBreach() {
        SlaReport report = healthService.slaFor("ZHR_PAYROLL_RFC");

        assertThat(report).isNotNull();
        assertThat(report.getErrors()).isZero();
        assertThat(report.breaching()).isFalse();
        assertThat(report.getSuccessRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("a slow interface fails the latency target but not the error rate")
    void slowInterfaceIsNotFailing() {
        SlaReport report = healthService.slaFor("ZSALES_ORDER_BAPI");

        assertThat(report).isNotNull();
        assertThat(report.getErrors()).isZero();
        assertThat(report.getP95Ms()).isGreaterThan(2500L);
    }

    @Test
    @DisplayName("the reports are ordered worst first")
    void reportsAreOrderedWorstFirst() {
        List<SlaReport> reports = healthService.slaReports();

        assertThat(reports).hasSize(8);
        for (int index = 1; index < reports.size(); index++) {
            assertThat(reports.get(index - 1).getSuccessRate())
                    .isLessThanOrEqualTo(reports.get(index).getSuccessRate());
        }
    }

    @Test
    @DisplayName("the percentile is computed the same way everywhere")
    void computesPercentile() {
        List<Long> values = Arrays.asList(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L);

        assertThat(healthService.percentile(values, 95)).isEqualTo(100L);
        assertThat(healthService.percentile(values, 50)).isEqualTo(50L);
        assertThat(healthService.percentile(values, 0)).isEqualTo(10L);
        assertThat(healthService.percentile(Arrays.<Long>asList(), 95)).isEqualTo(0L);
    }

    @Test
    @DisplayName("the hourly trend has one bucket per hour")
    void buildsHourlyTrend() {
        List<HourlyBucket> buckets = healthService.hourlyTrend("ZBANK_FILE_IDOC", 24);

        assertThat(buckets).hasSize(24);
        assertThat(buckets.get(0).getLabel()).isNotEmpty();

        int total = 0;
        for (HourlyBucket bucket : buckets) {
            total += bucket.getTotal();
        }
        assertThat(total).isGreaterThan(0);
    }

    @Test
    @DisplayName("the oldest failure of a broken interface is reported")
    void reportsOldestFailure() {
        SlaReport report = healthService.slaFor("ZBANK_FILE_IDOC");

        assertThat(report.getOldestFailureAgeHours()).isGreaterThan(0);
    }

    @Test
    @DisplayName("an unknown interface has no report")
    void unknownInterfaceHasNoReport() {
        assertThat(healthService.slaFor("Z_DOES_NOT_EXIST")).isNull();
    }

    @Test
    @DisplayName("the recent messages are newest first and trimmed")
    void returnsRecentMessages() {
        List<MessageRecord> messages = healthService.recentMessages("ZBANK_FILE_IDOC", 10);

        assertThat(messages).isNotEmpty();
        assertThat(messages.size()).isLessThanOrEqualTo(10);
        for (int index = 1; index < messages.size(); index++) {
            assertThat(messages.get(index - 1).getCreatedAt())
                    .isAfterOrEqualTo(messages.get(index).getCreatedAt());
        }
    }
}
