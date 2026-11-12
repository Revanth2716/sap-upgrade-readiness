package com.revzz.sap.upgrade.service;

import com.revzz.sap.upgrade.config.AppProperties;
import com.revzz.sap.upgrade.domain.ComponentDependency;
import com.revzz.sap.upgrade.domain.ComponentStatus;
import com.revzz.sap.upgrade.domain.PatchError;
import com.revzz.sap.upgrade.domain.Severity;
import com.revzz.sap.upgrade.domain.SoftwareComponent;
import com.revzz.sap.upgrade.domain.UpgradeAssessment;
import com.revzz.sap.upgrade.domain.UpgradeFinding;
import com.revzz.sap.upgrade.domain.UpgradeSystem;
import com.revzz.sap.upgrade.repo.UpgradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The upgrade rules, tested one at a time.
 *
 * <p>Each test builds exactly one situation, so that a failure says which
 * rule broke rather than "the score changed".</p>
 */
class UpgradeAnalysisServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    private UpgradeRepository repository;
    private final List<UpgradeSystem> systems = new ArrayList<UpgradeSystem>();
    private final List<ComponentStatus> statuses = new ArrayList<ComponentStatus>();
    private final List<ComponentDependency> dependencies =
            new ArrayList<ComponentDependency>();
    private final List<PatchError> errors = new ArrayList<PatchError>();
    private final List<SoftwareComponent> components = new ArrayList<SoftwareComponent>();

    @BeforeEach
    void setUp() {
        repository = mock(UpgradeRepository.class);
        when(repository.findSystems()).thenReturn(systems);
        when(repository.findAllStatus()).thenReturn(statuses);
        when(repository.findDependencies()).thenReturn(dependencies);
        when(repository.findAllErrors()).thenReturn(errors);
        when(repository.findComponents()).thenReturn(components);
    }

    // -- the rules -----------------------------------------------------------

    @Test
    @DisplayName("an obsolete component blocks the upgrade")
    void obsoleteComponentBlocksTheUpgrade() {
        system("PRD", 5, 30);
        component("SAP_MDM", "SAP Mobile", "SAP_MOB");
        status("PRD", "SAP_MDM", 3, ComponentStatus.STATUS_OBSOLETE);

        UpgradeAssessment assessment = service().assess("PRD");

        assertTrue(assessment.hasFinding("OBSOLETE_COMPONENT"));
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
        assertEquals(1, assessment.getBlockers().size());
        assertFalse(assessment.isReady());
    }

    @Test
    @DisplayName("a released component blocks the upgrade too")
    void releasedComponentBlocksTheUpgrade() {
        system("PRD", 5, 30);
        component("SAP_MDM", "SAP Mobile", "SAP_MOB");
        status("PRD", "SAP_MDM", 2, ComponentStatus.STATUS_RELEASED);

        UpgradeAssessment assessment = service().assess("PRD");

        assertTrue(assessment.hasFinding("OBSOLETE_COMPONENT"));
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
    }

    @Test
    @DisplayName("a failed patch is critical")
    void failedPatchIsCritical() {
        system("PRD", 5, 30);
        status("PRD", "SAP_BASIS", 5, ComponentStatus.STATUS_PATCHED);
        PatchError error = new PatchError();
        error.setErrorId("ER1");
        error.setSystemId("PRD");
        error.setComponentId("SAP_KERNEL");
        error.setSpId("SP05");
        error.setLogText("the update was aborted");
        error.setAt(NOW.minusDays(3));
        errors.add(error);

        UpgradeAssessment assessment = service().assess("PRD");

        assertTrue(assessment.hasFinding("PATCH_FAILED"));
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
        assertEquals(1, assessment.getErrors().size());
    }

    @Test
    @DisplayName("a component ahead of the one it requires breaks the dependency")
    void dependencyAheadIsBroken() {
        system("PRD", 3, 30);
        status("PRD", "SAP_BASIS", 3, ComponentStatus.STATUS_PATCHED);
        status("PRD", "SAP_HANA", 4, ComponentStatus.STATUS_PATCHED);
        dependency("SAP_HANA", "SAP_BASIS");

        UpgradeAssessment assessment = service().assess("PRD");

        assertTrue(assessment.hasFinding("COMPONENT_DEPENDENCY_BROKEN"));
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
    }

    @Test
    @DisplayName("a component at the same level as its requirement is not broken")
    void dependencyAtTheSameLevelIsFine() {
        system("PRD", 3, 30);
        status("PRD", "SAP_BASIS", 3, ComponentStatus.STATUS_PATCHED);
        status("PRD", "SAP_HANA", 3, ComponentStatus.STATUS_PATCHED);
        dependency("SAP_HANA", "SAP_BASIS");

        UpgradeAssessment assessment = service().assess("PRD");

        assertFalse(assessment.hasFinding("COMPONENT_DEPENDENCY_BROKEN"));
    }

    @Test
    @DisplayName("a component that is being replaced is left out of the dependency check")
    void blockingComponentIsNotDependencyChecked() {
        system("PRD", 3, 30);
        status("PRD", "SAP_BASIS", 3, ComponentStatus.STATUS_PATCHED);
        status("PRD", "SAP_HANA", 4, ComponentStatus.STATUS_OBSOLETE);
        dependency("SAP_HANA", "SAP_BASIS");

        UpgradeAssessment assessment = service().assess("PRD");

        assertFalse(assessment.hasFinding("COMPONENT_DEPENDENCY_BROKEN"),
                "a component that is being replaced is not going to move with the system");
        assertTrue(assessment.hasFinding("OBSOLETE_COMPONENT"));
    }

    @Test
    @DisplayName("a system more than the policy level behind is drifting")
    void versionDriftBeyondThePolicyIsAFinding() {
        system("DEV", 7, 5);
        system("PRD", 3, 30);

        UpgradeAssessment assessment = service().assess("PRD");

        assertTrue(assessment.hasFinding("VERSION_DRIFT"));
        assertEquals(Severity.HIGH, assessment.getSeverity());
        assertEquals(4, assessment.getDrift());
    }

    @Test
    @DisplayName("a system inside the drift policy is not a finding")
    void driftInsideThePolicyIsNotAFinding() {
        system("DEV", 7, 5);
        system("PRD", 5, 30);

        UpgradeAssessment assessment = service().assess("PRD");

        assertFalse(assessment.hasFinding("VERSION_DRIFT"));
        assertEquals(2, assessment.getDrift());
    }

    @Test
    @DisplayName("a component left behind inside its own system is a finding")
    void componentBehindItsOwnSystemIsAFinding() {
        system("PRD", 5, 30);
        status("PRD", "SAP_BASIS", 5, ComponentStatus.STATUS_PATCHED);
        status("PRD", "SAP_HANA", 3, ComponentStatus.STATUS_PATCHED);

        UpgradeAssessment assessment = service().assess("PRD");

        assertTrue(assessment.hasFinding("COMPONENT_BEHIND"));
        assertEquals(Severity.MEDIUM, assessment.getSeverity());
    }

    @Test
    @DisplayName("a system outside the patch window is a finding")
    void stalePatchWindowIsAFinding() {
        system("PRD", 5, 120);

        UpgradeAssessment assessment = service().assess("PRD");

        assertTrue(assessment.hasFinding("STALE_PATCH_WINDOW"));
        assertEquals(Severity.MEDIUM, assessment.getSeverity());
    }

    @Test
    @DisplayName("a system inside the patch window is not a finding")
    void freshPatchIsNotAFinding() {
        system("PRD", 5, 30);

        UpgradeAssessment assessment = service().assess("PRD");

        assertFalse(assessment.hasFinding("STALE_PATCH_WINDOW"));
    }

    @Test
    @DisplayName("a system that was never patched is a finding")
    void noPatchHistoryIsAFinding() {
        system("PRD", 5, -1);

        UpgradeAssessment assessment = service().assess("PRD");

        assertTrue(assessment.hasFinding("NO_PATCH_HISTORY"));
        assertEquals(Severity.HIGH, assessment.getSeverity());
    }

    @Test
    @DisplayName("a system with nothing wrong produces no findings at all")
    void cleanSystemHasNoFindings() {
        system("DEV", 7, 5);
        status("DEV", "SAP_BASIS", 7, ComponentStatus.STATUS_PATCHED);
        status("DEV", "SAP_MOB", 7, ComponentStatus.STATUS_NEW);

        UpgradeAssessment assessment = service().assess("DEV");

        assertTrue(assessment.getFindings().isEmpty(),
                "expected no findings but got " + assessment.getFindings());
        assertEquals(100, assessment.getScore());
        assertEquals(Severity.INFO, assessment.getSeverity());
        assertTrue(assessment.isReady());
    }

    @Test
    @DisplayName("the severity is the worst finding, not the score band")
    void severityIsTheWorstFindingNotTheScoreBand() {
        // A failed patch costs 25 points, so the score is 75 - and 75 sits in
        // the "medium" band. The severity has to say "critical".
        system("PRD", 5, 30);
        status("PRD", "SAP_BASIS", 5, ComponentStatus.STATUS_PATCHED);
        PatchError error = new PatchError();
        error.setErrorId("ER1");
        error.setSystemId("PRD");
        error.setComponentId("SAP_KERNEL");
        error.setSpId("SP05");
        error.setLogText("aborted");
        error.setAt(NOW.minusDays(3));
        errors.add(error);

        UpgradeAssessment assessment = service().assess("PRD");

        assertEquals(75, assessment.getScore());
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
    }

    @Test
    @DisplayName("the score never goes below zero")
    void scoreNeverGoesNegative() {
        system("PRD", 3, 300);
        component("SAP_MDM", "SAP Mobile", "SAP_MOB");
        status("PRD", "SAP_MDM", 2, ComponentStatus.STATUS_OBSOLETE);
        status("PRD", "SAP_HANA", 4, ComponentStatus.STATUS_OBSOLETE);
        dependency("SAP_HANA", "SAP_BASIS");
        PatchError error = new PatchError();
        error.setErrorId("ER1");
        error.setSystemId("PRD");
        error.setComponentId("SAP_KERNEL");
        error.setSpId("SP03");
        error.setLogText("aborted");
        error.setAt(NOW.minusDays(3));
        errors.add(error);

        UpgradeAssessment assessment = service().assess("PRD");

        assertTrue(assessment.getScore() >= 0);
    }

    // -- helpers -------------------------------------------------------------

    private UpgradeAnalysisService service() {
        return new UpgradeAnalysisService(repository, new AppProperties());
    }

    /**
     * A system of the landscape.
     *
     * @param daysAgoPatch negative means "never patched"
     */
    private UpgradeSystem system(String id, int level, int daysAgoPatch) {
        UpgradeSystem system = new UpgradeSystem();
        system.setSystemId(id);
        system.setDescription("System " + id);
        system.setSystemRole(UpgradeSystem.ROLE_PRODUCTION);
        system.setTarget(true);
        system.setPatchLevel(level);
        system.setLastPatch(daysAgoPatch < 0 ? null : NOW.minusDays(daysAgoPatch));
        system.setPatchedBy("BASIS.LEAD");
        systems.add(system);
        return system;
    }

    private SoftwareComponent component(String id, String description, String replacedBy) {
        SoftwareComponent component = new SoftwareComponent();
        component.setComponentId(id);
        component.setDescription(description);
        component.setProduct("SAP NetWeaver");
        component.setReplacedBy(replacedBy);
        components.add(component);
        return component;
    }

    private ComponentStatus status(String systemId, String componentId, int level,
                                   String state) {
        ComponentStatus status = new ComponentStatus();
        status.setSystemId(systemId);
        status.setComponentId(componentId);
        status.setPatchLevel(level);
        status.setStatus(state);
        status.setStatusText(state);
        status.setLastChanged(NOW.minusDays(10));
        statuses.add(status);
        return status;
    }

    private void dependency(String componentId, String requires) {
        ComponentDependency dependency = new ComponentDependency();
        dependency.setDependencyId("DP" + dependencies.size());
        dependency.setComponentId(componentId);
        dependency.setRequiresComponent(requires);
        dependencies.add(dependency);
    }

    private UpgradeFinding finding(UpgradeAssessment assessment, String category) {
        for (UpgradeFinding finding : assessment.getFindings()) {
            if (category.equals(finding.getCategory())) {
                return finding;
            }
        }
        throw new AssertionError("no finding with category " + category);
    }
}
