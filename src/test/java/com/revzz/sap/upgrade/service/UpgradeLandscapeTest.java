package com.revzz.sap.upgrade.service;

import com.revzz.sap.upgrade.domain.ChangeEntry;
import com.revzz.sap.upgrade.domain.ComponentStatus;
import com.revzz.sap.upgrade.domain.Severity;
import com.revzz.sap.upgrade.domain.SupportPackage;
import com.revzz.sap.upgrade.domain.UpgradeAssessment;
import com.revzz.sap.upgrade.domain.UpgradeFinding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seeded upgrade landscape.
 *
 * <p>These tests check the story the demo data has to tell. If the seeding
 * changes and the story changes with it, these tests are the ones that say
 * so first.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class UpgradeLandscapeTest {

    @Autowired
    private UpgradeAnalysisService analysisService;

    @Autowired
    private ChangelogService changelogService;

    @Test
    @DisplayName("the landscape holds three systems")
    void threeSystems() {
        assertEquals(3, analysisService.systems().size());
        assertEquals(3, analysisService.assessments().size());
    }

    @Test
    @DisplayName("DEV is clean - it is on the latest level and has migrated the replaced product")
    void devIsClean() {
        UpgradeAssessment dev = analysisService.assess("DEV");

        assertNotNull(dev);
        assertEquals(100, dev.getScore());
        assertEquals(Severity.INFO, dev.getSeverity());
        assertTrue(dev.getFindings().isEmpty(),
                "DEV should have no findings but had " + dev.getFindings());
        assertTrue(dev.getBlockers().isEmpty());
        assertEquals(0, dev.getDrift());
        assertTrue(dev.isReady());
    }

    @Test
    @DisplayName("PRD is critical - four levels behind, two blockers, a failed patch and a broken dependency")
    void prdIsCritical() {
        UpgradeAssessment prd = analysisService.assess("PRD");

        assertNotNull(prd);
        assertEquals(Severity.CRITICAL, prd.getSeverity());
        assertEquals(5, prd.getScore());
        assertEquals(4, prd.getDrift());
        assertTrue(prd.hasFinding("OBSOLETE_COMPONENT"));
        assertTrue(prd.hasFinding("PATCH_FAILED"));
        assertTrue(prd.hasFinding("COMPONENT_DEPENDENCY_BROKEN"));
        assertTrue(prd.hasFinding("VERSION_DRIFT"));
        assertEquals(2, prd.getBlockers().size());
        assertEquals(1, prd.getErrors().size());
    }

    @Test
    @DisplayName("QA is critical - an obsolete component, a component left behind and a stale window")
    void qaIsCritical() {
        UpgradeAssessment qa = analysisService.assess("QA");

        assertNotNull(qa);
        assertEquals(Severity.CRITICAL, qa.getSeverity());
        assertEquals(45, qa.getScore());
        assertTrue(qa.hasFinding("OBSOLETE_COMPONENT"));
        assertTrue(qa.hasFinding("COMPONENT_BEHIND"));
        assertTrue(qa.hasFinding("STALE_PATCH_WINDOW"));
        assertFalse(qa.hasFinding("VERSION_DRIFT"),
                "QA is two levels behind, and the policy allows two");
        assertEquals(1, qa.getBlockers().size());
    }

    @Test
    @DisplayName("the blocked components are the ones whose product was replaced")
    void theBlockersAreTheReplacedProducts() {
        UpgradeAssessment prd = analysisService.assess("PRD");

        boolean sawStPi = false;
        boolean sawMdm = false;
        for (ComponentStatus blocker : prd.getBlockers()) {
            if ("SAP_ST-PI".equals(blocker.getComponentId())) {
                sawStPi = true;
                assertEquals(ComponentStatus.STATUS_OBSOLETE, blocker.getStatus());
            }
            if ("SAP_MDM".equals(blocker.getComponentId())) {
                sawMdm = true;
                assertEquals(ComponentStatus.STATUS_RELEASED, blocker.getStatus());
            }
        }
        assertTrue(sawStPi, "SAP_ST-PI blocks the PRD upgrade");
        assertTrue(sawMdm, "SAP_MDM blocks the PRD upgrade");
    }

    @Test
    @DisplayName("the counts add up to the landscape")
    void countsAddUp() {
        int counted = analysisService.countBySeverity(Severity.CRITICAL)
                + analysisService.countBySeverity(Severity.HIGH)
                + analysisService.countBySeverity(Severity.MEDIUM)
                + analysisService.countBySeverity(Severity.LOW)
                + analysisService.countBySeverity(Severity.INFO);

        assertEquals(analysisService.systems().size(), counted);
        assertEquals(2, analysisService.countBySeverity(Severity.CRITICAL));
        assertEquals(1, analysisService.countBySeverity(Severity.INFO));
        assertEquals(3, analysisService.blockerCount());
        assertEquals(50, analysisService.averageScore());
    }

    @Test
    @DisplayName("the landscape is sorted worst first")
    void sortedWorstFirst() {
        List<UpgradeAssessment> assessments = analysisService.assessments();

        assertEquals("PRD", assessments.get(0).getSystem().getSystemId());
        assertEquals("QA", assessments.get(1).getSystem().getSystemId());
        assertEquals("DEV", assessments.get(2).getSystem().getSystemId());
    }

    @Test
    @DisplayName("every finding carries a recommended action")
    void everyFindingHasAnAction() {
        for (UpgradeFinding finding : analysisService.allFindings()) {
            assertNotNull(finding.getRecommendation());
            assertFalse(finding.getRecommendation().trim().isEmpty(),
                    "finding " + finding.getCategory() + " has no recommendation");
            assertFalse(finding.getMessage().trim().isEmpty());
        }
    }

    @Test
    @DisplayName("the report names every system and every finding")
    void reportCoversEverything() {
        String report = analysisService.renderReport();

        assertTrue(report.contains("# Upgrade readiness report"));
        for (String id : new String[]{"DEV", "QA", "PRD"}) {
            assertTrue(report.contains(id), "the report does not mention " + id);
        }
        assertTrue(report.contains("OBSOLETE_COMPONENT"));
        assertTrue(report.contains("## Upgrade blockers"));
        assertTrue(report.contains("## Component state"));
    }

    // -- the changelog -------------------------------------------------------

    @Test
    @DisplayName("seven support packages carry a changelog")
    void sevenPackages() {
        List<SupportPackage> packages = changelogService.packages();

        assertEquals(7, packages.size());
        assertEquals(16, changelogService.allChangelog().size());
    }

    @Test
    @DisplayName("the incompatible changes can be pulled out on their own")
    void theBreakingChangesCanBePulledOut() {
        int breaking = 0;
        for (SupportPackage pack : changelogService.packages()) {
            breaking += changelogService.breakingCount(pack.getSpId());
        }

        assertEquals(4, breaking);
        assertEquals(4, changelogService.packagesWithBreakingChanges().size());
        assertEquals(1, changelogService.breakingEntries("SP07").size());
        assertEquals(0, changelogService.breakingEntries("SP03").size());
    }

    @Test
    @DisplayName("the changelog of one package can be read per component")
    void theChangelogCanBeReadPerComponent() {
        List<ChangeEntry> entries = changelogService.changelog("SP07", "SAP_HANA");

        assertEquals(1, entries.size());
        assertEquals(ChangeEntry.CATEGORY_SECURITY, entries.get(0).getCategory());
        assertFalse(entries.get(0).isBreaking());
    }

    @Test
    @DisplayName("the packages are offered newest first")
    void packagesAreOfferedNewestFirst() {
        List<SupportPackage> packages = changelogService.packagesNewestFirst();

        assertEquals("SP07", packages.get(0).getSpId());
        assertEquals("SP01", packages.get(packages.size() - 1).getSpId());
    }
}
