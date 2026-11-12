package com.revzz.sap.upgrade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application configuration.
 *
 * <p>The policy lives here, not in the code: how far a system may drift
 * behind the landscape, how far a component may sit behind, and how long a
 * system may go unpatched. These are the numbers a customer agrees with its
 * auditors and its SAP account team, so they belong in configuration.</p>
 */
@Component
@ConfigurationProperties(prefix = "upgrade")
public class AppProperties {

    private String applicationName = "SAP Upgrade & Support Package Readiness";
    private String sapSystem = "SPAM";
    private boolean seedDemoUsers = true;
    private boolean seedDemoData = true;
    private String demoPassword = "demo1234";
    private String gateway = "local";
    private Seed seed = new Seed();
    private Policy policy = new Policy();
    private Mail mail = new Mail();

    public static class Seed {
        private long randomSeed = 20261112L;

        public long getRandomSeed() {
            return randomSeed;
        }

        public void setRandomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
        }
    }

    public static class Policy {
        private int stalePatchWindowDays = 90;
        private int componentDriftLevels = 1;
        private int maxSystemDriftLevels = 2;

        public int getStalePatchWindowDays() {
            return stalePatchWindowDays;
        }

        public void setStalePatchWindowDays(int stalePatchWindowDays) {
            this.stalePatchWindowDays = stalePatchWindowDays;
        }

        public int getComponentDriftLevels() {
            return componentDriftLevels;
        }

        public void setComponentDriftLevels(int componentDriftLevels) {
            this.componentDriftLevels = componentDriftLevels;
        }

        public int getMaxSystemDriftLevels() {
            return maxSystemDriftLevels;
        }

        public void setMaxSystemDriftLevels(int maxSystemDriftLevels) {
            this.maxSystemDriftLevels = maxSystemDriftLevels;
        }
    }

    public static class Mail {
        private boolean enabled = false;
        private String from = "upgrade-readiness@example.com";
        private String to = "basis-team@example.com";
        private String subjectPrefix = "[SAP Upgrade]";

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

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public Seed getSeed() {
        return seed;
    }

    public void setSeed(Seed seed) {
        this.seed = seed;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }
}
