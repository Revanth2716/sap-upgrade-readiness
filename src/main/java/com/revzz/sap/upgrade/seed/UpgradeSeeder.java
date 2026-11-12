package com.revzz.sap.upgrade.seed;

import com.revzz.sap.upgrade.config.AppProperties;
import com.revzz.sap.upgrade.domain.ChangeEntry;
import com.revzz.sap.upgrade.domain.ComponentDependency;
import com.revzz.sap.upgrade.domain.ComponentStatus;
import com.revzz.sap.upgrade.domain.PatchError;
import com.revzz.sap.upgrade.domain.SoftwareComponent;
import com.revzz.sap.upgrade.domain.SupportPackage;
import com.revzz.sap.upgrade.domain.UpgradeSystem;
import com.revzz.sap.upgrade.repo.UpgradeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds an upgrade landscape with a story.
 *
 * <p>Three systems, each with the problem a real customer has:</p>
 *
 * <ul>
 *   <li><b>DEV</b> - on the latest patch level, migrated off the replaced
 *       product, and clean. The control.</li>
 *   <li><b>QA</b> - two levels behind, one component still on the old
 *       product, and a component left behind inside its own system.</li>
 *   <li><b>PRD</b> - four levels behind, two components that cannot be
 *       patched at all, a patch that failed, and one component patched
 *       ahead of the one it depends on.</li>
 * </ul>
 *
 * <p>Random noise would prove nothing. Every number here exists because it
 * demonstrates one specific finding.</p>
 */
@Component
@Order(2)
public class UpgradeSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final UpgradeRepository repository;
    private final AppProperties properties;

    public UpgradeSeeder(JdbcTemplate jdbc, UpgradeRepository repository,
                          AppProperties properties) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.isSeedDemoData() || repository.countSystems() > 0) {
            return;
        }
        seedUsers();
        LocalDateTime now = LocalDateTime.now();

        // --- the landscape ----------------------------------------------------
        system("DEV", "Development system", UpgradeSystem.ROLE_DEVELOPMENT, false,
                7, now.minusDays(6), "BASIS.LEAD");
        system("QA", "Quality system", UpgradeSystem.ROLE_QUALITY, false,
                5, now.minusDays(120), "DEVADMIN");
        system("PRD", "Production ERP", UpgradeSystem.ROLE_PRODUCTION, true,
                3, now.minusDays(45), "BASIS.LEAD");

        // --- the components ---------------------------------------------------
        component("SAP_BASIS", "NetWeaver Basis", "SAP NetWeaver", "");
        component("SAP_ABA", "ABAP Runtime", "SAP NetWeaver", "");
        component("SAP_KERNEL", "ABAP Kernel", "SAP NetWeaver", "");
        component("SAP_WEBAS", "Web Application Server", "SAP NetWeaver", "");
        component("SAP_JAVA", "J2EE Engine", "SAP NetWeaver", "");
        component("SAP_ST-PI", "SAP NetWeaver Portal", "SAP NetWeaver", "");
        component("SAP_HANA", "SAP HANA Database", "SAP HANA", "");
        component("SAP_MDM", "Mobile Data Management", "SAP Mobile", "SAP_MOB");
        component("SAP_MOB", "SAP Mobile", "SAP Mobile", "");

        // --- DEV: the latest level, migrated and clean -----------------------
        patched("DEV", "SAP_BASIS", 7, 6);
        patched("DEV", "SAP_ABA", 7, 6);
        patched("DEV", "SAP_KERNEL", 7, 6);
        patched("DEV", "SAP_WEBAS", 7, 6);
        patched("DEV", "SAP_JAVA", 7, 6);
        patched("DEV", "SAP_ST-PI", 7, 6);
        patched("DEV", "SAP_HANA", 7, 6);
        patched("DEV", "SAP_MDM", 7, 6);
        // The replacement product was migrated in last week and has never
        // been patched - which is normal, and is not a finding.
        status("DEV", "SAP_MOB", 7, ComponentStatus.STATUS_NEW,
                "Newly migrated, never patched", 5);

        // --- QA: two levels behind, and a product that cannot be patched -----
        patched("QA", "SAP_BASIS", 5, 120);
        patched("QA", "SAP_ABA", 5, 120);
        patched("QA", "SAP_KERNEL", 5, 120);
        patched("QA", "SAP_WEBAS", 5, 120);
        patched("QA", "SAP_JAVA", 5, 120);
        patched("QA", "SAP_ST-PI", 5, 120);
        // The database component was left behind inside its own system.
        patched("QA", "SAP_HANA", 3, 118);
        // The product has been replaced. There is no patch level that fixes
        // this - it has to be migrated.
        status("QA", "SAP_MDM", 3, ComponentStatus.STATUS_OBSOLETE,
                "Obsolete - the product was replaced by SAP_MOB", 240);

        // --- PRD: four levels behind, and blocked three ways ------------------
        patched("PRD", "SAP_BASIS", 3, 45);
        patched("PRD", "SAP_ABA", 3, 45);
        patched("PRD", "SAP_KERNEL", 3, 45);
        patched("PRD", "SAP_WEBAS", 3, 45);
        patched("PRD", "SAP_JAVA", 3, 45);
        // The database was upgraded separately, so it is now ahead of the
        // Basis it depends on - a combination SAP does not support.
        patched("PRD", "SAP_HANA", 4, 60);
        status("PRD", "SAP_ST-PI", 3, ComponentStatus.STATUS_OBSOLETE,
                "Obsolete - the product was replaced by SAP MOB", 300);
        status("PRD", "SAP_MDM", 2, ComponentStatus.STATUS_RELEASED,
                "Released - SAP no longer delivers this component", 300);

        // --- the patch log ----------------------------------------------------
        error("PRD", "SAP_KERNEL", "SP03",
                "Component SAP_KERNEL: the update was aborted in the import phase - "
                        + "a required patch level of SAP_ABA was not present in the system.",
                now.minusDays(30));

        // --- the components that move together --------------------------------
        dependency("SAP_ABA", "SAP_BASIS");
        dependency("SAP_KERNEL", "SAP_ABA");
        dependency("SAP_HANA", "SAP_BASIS");
        dependency("SAP_JAVA", "SAP_WEBAS");
        dependency("SAP_ST-PI", "SAP_WEBAS");
        dependency("SAP_MOB", "SAP_BASIS");

        // --- the support packages ---------------------------------------------
        pack("SP01", 1, LocalDate.now().minusDays(900), "Support package 01");
        pack("SP02", 2, LocalDate.now().minusDays(760), "Support package 02");
        pack("SP03", 3, LocalDate.now().minusDays(610), "Support package 03");
        pack("SP04", 4, LocalDate.now().minusDays(450), "Support package 04");
        pack("SP05", 5, LocalDate.now().minusDays(300), "Support package 05");
        pack("SP06", 6, LocalDate.now().minusDays(160), "Support package 06");
        pack("SP07", 7, LocalDate.now().minusDays(20), "Support package 07");

        // --- the changelog ----------------------------------------------------
        // The incompatible changes are the ones a test team needs, and the
        // ones everybody skips.
        change("SP01", "SAP_BASIS", ChangeEntry.CATEGORY_FEATURE,
                "Basis: the support package manager records the patch level of every "
                        + "component in the system.");
        change("SP02", "SAP_KERNEL", ChangeEntry.CATEGORY_FEATURE,
                "ABAP Kernel: new kernel statistics for the Unicode conversions.");

        change("SP03", "SAP_BASIS", ChangeEntry.CATEGORY_FEATURE,
                "Basis: support for the transport of copies of ABAP objects.");
        change("SP03", "SAP_ABA", ChangeEntry.CATEGORY_FIX,
                "ABAP Runtime: OPEN CURSOR over an internal table no longer reads the whole "
                        + "table when only the key is needed.");

        change("SP04", "SAP_MDM", ChangeEntry.CATEGORY_INCOMPATIBLE,
                "Mobile Data Management: the synchronisation protocol changed. A device on "
                        + "SP03 cannot talk to a server on SP04 - both sides have to move.");
        change("SP04", "SAP_BASIS", ChangeEntry.CATEGORY_SECURITY,
                "Basis: the SAP_ALL authorisation check now covers the new diagnostic services.");

        change("SP05", "SAP_HANA", ChangeEntry.CATEGORY_INCOMPATIBLE,
                "HANA: the SQL plan cache is no longer compatible across patch levels. After "
                        + "the upgrade the cache has to be cleared, or the old plans keep being "
                        + "used against the new statistics.");
        change("SP05", "SAP_BASIS", ChangeEntry.CATEGORY_FEATURE,
                "Basis: the transport of copies of ABAP objects is supported end to end.");
        change("SP05", "SAP_JAVA", ChangeEntry.CATEGORY_FIX,
                "J2EE Engine: corrected the shutdown sequence of the JMS provider, which "
                        + "left messages in the queue on a controlled shutdown.");

        change("SP06", "SAP_WEBAS", ChangeEntry.CATEGORY_INCOMPATIBLE,
                "Web Application Server: the deprecated com.sap.ewp.diagnostic package is "
                        + "removed. Custom code that used it will not compile.");
        change("SP06", "SAP_ABA", ChangeEntry.CATEGORY_FIX,
                "ABAP Runtime: improved the performance of OPEN CURSOR over internal tables.");
        change("SP06", "SAP_ST-PI", ChangeEntry.CATEGORY_SECURITY,
                "Portal: the cross-site scripting filter now also covers the iframe portlet.");

        change("SP07", "SAP_ABA", ChangeEntry.CATEGORY_INCOMPATIBLE,
                "ABAP Runtime: an RFC call without an explicit destination now fails with "
                        + "exception 13 instead of silently returning an empty row set.");
        change("SP07", "SAP_BASIS", ChangeEntry.CATEGORY_FEATURE,
                "Basis: a new spool cleanup task removes orphaned print requests that no "
                        + "longer have a device.");
        change("SP07", "SAP_HANA", ChangeEntry.CATEGORY_SECURITY,
                "HANA: the default password policy for the technical database user was "
                        + "strengthened. Existing users are not changed automatically.");
        change("SP07", "SAP_KERNEL", ChangeEntry.CATEGORY_FIX,
                "ABAP Kernel: corrected the Unicode normalisation of character fields in "
                        + "internal tables.");
    }

    private void seedUsers() {
        Object[][] users = {
                {"BASIS.LEAD", "Basis Lead", "A", "IT-Basis"},
                {"DEVADMIN", "Dev Administrator", "A", "IT-Basis"},
                {"UPGRADE.PLANNER", "Upgrade Planner", "A", "IT-Basis"},
                {"DIAG.ANALYST", "Diagnostics Analyst", "A", "IT-Basis"},
        };
        for (Object[] user : users) {
            jdbc.update("INSERT INTO SAPUSER (USERID, FULLNAME, UFLAG, DEPT) VALUES (?,?,?,?)",
                    user[0], user[1], user[2], user[3]);
        }
    }

    private void system(String id, String description, String role, boolean target,
                        int patchLevel, LocalDateTime lastPatch, String patchedBy) {
        UpgradeSystem system = new UpgradeSystem();
        system.setSystemId(id);
        system.setDescription(description);
        system.setSystemRole(role);
        system.setTarget(target);
        system.setPatchLevel(patchLevel);
        system.setLastPatch(lastPatch);
        system.setPatchedBy(patchedBy);
        repository.insertSystem(system);
    }

    private void component(String id, String description, String product, String replacedBy) {
        SoftwareComponent component = new SoftwareComponent();
        component.setComponentId(id);
        component.setDescription(description);
        component.setProduct(product);
        component.setReplacedBy(replacedBy);
        repository.insertComponent(component);
    }

    private void patched(String systemId, String componentId, int level, int daysAgo) {
        status(systemId, componentId, level, ComponentStatus.STATUS_PATCHED,
                "Patched at level " + level, daysAgo);
    }

    private void status(String systemId, String componentId, int level, String status,
                        String statusText, int daysAgo) {
        ComponentStatus componentStatus = new ComponentStatus();
        componentStatus.setSystemId(systemId);
        componentStatus.setComponentId(componentId);
        componentStatus.setPatchLevel(level);
        componentStatus.setStatus(status);
        componentStatus.setStatusText(statusText);
        componentStatus.setLastChanged(LocalDateTime.now().minusDays(daysAgo)
                .withHour(9).withMinute(30).withSecond(0).withNano(0));
        repository.insertStatus(componentStatus);
    }

    private void error(String systemId, String componentId, String spId, String logText,
                       LocalDateTime at) {
        PatchError error = new PatchError();
        error.setErrorId("ER" + (errorCounter++));
        error.setSystemId(systemId);
        error.setComponentId(componentId);
        error.setSpId(spId);
        error.setLogText(logText);
        error.setAt(at);
        repository.insertError(error);
    }

    private void dependency(String componentId, String requires) {
        ComponentDependency dependency = new ComponentDependency();
        dependency.setDependencyId("DP" + (dependencyCounter++));
        dependency.setComponentId(componentId);
        dependency.setRequiresComponent(requires);
        repository.insertDependency(dependency);
    }

    private void pack(String spId, int level, LocalDate releasedOn, String description) {
        SupportPackage pack = new SupportPackage();
        pack.setSpId(spId);
        pack.setPatchLevel(level);
        pack.setReleasedOn(releasedOn);
        pack.setDescription(description);
        repository.insertPackage(pack);
    }

    private void change(String spId, String componentId, String category, String text) {
        ChangeEntry entry = new ChangeEntry();
        entry.setEntryId("CE" + (changeCounter++));
        entry.setSpId(spId);
        entry.setComponentId(componentId);
        entry.setCategory(category);
        entry.setEntryText(text);
        repository.insertChange(entry);
    }

    private int errorCounter = 1;
    private int dependencyCounter = 1;
    private int changeCounter = 1;
}
