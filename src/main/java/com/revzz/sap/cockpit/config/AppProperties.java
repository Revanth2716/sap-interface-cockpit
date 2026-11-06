package com.revzz.sap.cockpit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application configuration.
 *
 * <p>The monitoring thresholds live here rather than in code, because the
 * first conversation with an integration team is always about numbers: what
 * counts as a burst, how old is too old, what is the latency target.</p>
 */
@Component
@ConfigurationProperties(prefix = "cockpit")
public class AppProperties {

    private String applicationName = "SAP Interface Cockpit";
    private String sapSystem = "NW7";
    private boolean seedDemoUsers = true;
    private boolean seedDemoData = true;
    private String demoPassword = "demo1234";
    private Seed seed = new Seed();
    private Monitoring monitoring = new Monitoring();
    private Mail mail = new Mail();

    public static class Seed {
        private int days = 7;
        private int messagesPerDay = 220;
        private long randomSeed = 20261106L;

        public int getDays() {
            return days;
        }

        public void setDays(int days) {
            this.days = days;
        }

        public int getMessagesPerDay() {
            return messagesPerDay;
        }

        public void setMessagesPerDay(int messagesPerDay) {
            this.messagesPerDay = messagesPerDay;
        }

        public long getRandomSeed() {
            return randomSeed;
        }

        public void setRandomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
        }
    }

    public static class Monitoring {
        private int slaWindowHours = 24;
        private int failureBurstThreshold = 5;
        private int ageingHours = 12;
        private int latencyThresholdMs = 2500;

        public int getSlaWindowHours() {
            return slaWindowHours;
        }

        public void setSlaWindowHours(int slaWindowHours) {
            this.slaWindowHours = slaWindowHours;
        }

        public int getFailureBurstThreshold() {
            return failureBurstThreshold;
        }

        public void setFailureBurstThreshold(int failureBurstThreshold) {
            this.failureBurstThreshold = failureBurstThreshold;
        }

        public int getAgeingHours() {
            return ageingHours;
        }

        public void setAgeingHours(int ageingHours) {
            this.ageingHours = ageingHours;
        }

        public int getLatencyThresholdMs() {
            return latencyThresholdMs;
        }

        public void setLatencyThresholdMs(int latencyThresholdMs) {
            this.latencyThresholdMs = latencyThresholdMs;
        }
    }

    public static class Mail {
        private boolean enabled = false;
        private String from = "cockpit@example.com";
        private String to = "integration-ops@example.com";
        private String subjectPrefix = "[SAP Interface Cockpit]";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getSubjectPrefix() {
            return subjectPrefix;
        }

        public void setSubjectPrefix(String subjectPrefix) {
            this.subjectPrefix = subjectPrefix;
        }
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getSapSystem() {
        return sapSystem;
    }

    public void setSapSystem(String sapSystem) {
        this.sapSystem = sapSystem;
    }

    public boolean isSeedDemoUsers() {
        return seedDemoUsers;
    }

    public void setSeedDemoUsers(boolean seedDemoUsers) {
        this.seedDemoUsers = seedDemoUsers;
    }

    public boolean isSeedDemoData() {
        return seedDemoData;
    }

    public void setSeedDemoData(boolean seedDemoData) {
        this.seedDemoData = seedDemoData;
    }

    public String getDemoPassword() {
        return demoPassword;
    }

    public void setDemoPassword(String demoPassword) {
        this.demoPassword = demoPassword;
    }

    public Seed getSeed() {
        return seed;
    }

    public void setSeed(Seed seed) {
        this.seed = seed;
    }

    public Monitoring getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(Monitoring monitoring) {
        this.monitoring = monitoring;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }
}
