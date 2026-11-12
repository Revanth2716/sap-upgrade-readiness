package com.revzz.sap.upgrade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * SAP Upgrade &amp; Support Package Readiness.
 *
 * <p>Answers the question an upgrade project starts with and never finishes
 * answering: <em>can this system actually be upgraded</em>, and if not, what
 * is stopping it.</p>
 *
 * <p>It finds the obsolete components that block an upgrade outright, the
 * components left behind inside their own system, the component patched
 * ahead of the one it depends on, the patch that failed, and how far each
 * system has drifted behind the landscape.</p>
 */
@SpringBootApplication
public class UpgradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(UpgradeApplication.class, args);
    }

    /** Entry point for a WAR deployment on the SAP NetWeaver Java stack. */
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(UpgradeApplication.class);
    }
}
