package com.revzz.sap.cockpit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SAP Interface Cockpit.
 *
 * <p>Integration operations for an SAP landscape: RFC, IDoc and BAPI message
 * monitoring, SLA tracking, error triage with an actionable catalogue, rule
 * based alerting and a replay queue for failed messages.</p>
 *
 * <p>The cockpit answers the three questions an integration team asks every
 * morning: <em>what is broken</em>, <em>what is causing it</em>, and
 * <em>what do we do about it</em>.</p>
 */
@SpringBootApplication
@EnableScheduling
public class CockpitApplication {

    public static void main(String[] args) {
        SpringApplication.run(CockpitApplication.class, args);
    }

    /** Entry point for a WAR deployment on the SAP NetWeaver Java stack. */
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CockpitApplication.class);
    }
}
