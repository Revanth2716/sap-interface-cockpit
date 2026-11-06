package com.revzz.sap.cockpit.service;

import com.revzz.sap.cockpit.domain.ErrorCluster;
import com.revzz.sap.cockpit.domain.ErrorCode;
import com.revzz.sap.cockpit.domain.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ErrorTriageServiceTest {

    @Autowired
    private ErrorTriageService triageService;

    @Test
    @DisplayName("the failures are grouped into one problem per interface and cause")
    void groupsFailuresIntoClusters() {
        List<ErrorCluster> clusters = triageService.clusters();

        assertThat(clusters).isNotEmpty();

        Set<String> keys = new HashSet<String>();
        for (ErrorCluster cluster : clusters) {
            keys.add(cluster.getInterfaceName() + "|" + cluster.getErrorCode());
        }
        assertThat(keys).hasSameSizeAs(clusters);
    }

    @Test
    @DisplayName("a cluster carries the cause and the remediation from the catalogue")
    void enrichesClustersFromTheCatalogue() {
        ErrorCluster cluster = null;
        for (ErrorCluster candidate : triageService.clusters()) {
            if ("ERR_AUTH_403".equals(candidate.getErrorCode())) {
                cluster = candidate;
                break;
            }
        }

        assertThat(cluster).isNotNull();
        assertThat(cluster.getInterfaceName()).isEqualTo("ZBANK_FILE_IDOC");
        assertThat(cluster.getCause()).containsIgnoringCase("authorisation");
        assertThat(cluster.getRemediation()).contains("SU01");
        assertThat(cluster.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(cluster.getOccurrences()).isGreaterThan(1);
    }

    @Test
    @DisplayName("the clusters are ordered by severity, then by size")
    void ordersClustersBySeverity() {
        List<ErrorCluster> clusters = triageService.clusters();

        for (int index = 1; index < clusters.size(); index++) {
            Severity previous = clusters.get(index - 1).getSeverity();
            Severity current = clusters.get(index).getSeverity();
            assertThat(previous.rank()).isLessThanOrEqualTo(current.rank());
        }
    }

    @Test
    @DisplayName("a single cluster can be looked up")
    void looksUpOneCluster() {
        ErrorCluster cluster = triageService.cluster("ZBANK_FILE_IDOC", "ERR_AUTH_403");

        assertThat(cluster).isNotNull();
        assertThat(cluster.getInterfaceName()).isEqualTo("ZBANK_FILE_IDOC");
        assertThat(triageService.cluster("ZBANK_FILE_IDOC", "ERR_NOPE")).isNull();
    }

    @Test
    @DisplayName("the catalogue explains every error code")
    void theCatalogueIsComplete() {
        List<ErrorCode> codes = triageService.catalogue();

        assertThat(codes).hasSizeGreaterThanOrEqualTo(8);
        for (ErrorCode code : codes) {
            assertThat(code.getCause()).isNotEmpty();
            assertThat(code.getRemediation()).isNotEmpty();
            assertThat(code.getSeverity()).isNotNull();
        }
    }

    @Test
    @DisplayName("a cluster counts the distinct hosts it came from")
    void countsDistinctHosts() {
        ErrorCluster cluster = triageService.cluster("ZBANK_FILE_IDOC", "ERR_AUTH_403");

        assertThat(cluster).isNotNull();
        assertThat(cluster.getDistinctHosts()).isGreaterThanOrEqualTo(1);
    }
}
