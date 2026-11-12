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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The upgrade readiness.
 *
 * <p>Six questions, one per system:</p>
 *
 * <ol>
 *   <li><b>Can this system be upgraded at all?</b> — a component whose
 *       product has been replaced has to be migrated first. Nothing else
 *       matters until that is done.</li>
 *   <li><b>Did the last patch work?</b> — a failed patch leaves the system
 *       in a state that nobody can reason about.</li>
 *   <li><b>Are the components consistent with each other?</b> — a component
 *       patched ahead of the one it depends on is a state SAP does not
 *       support.</li>
 *   <li><b>How far behind is the system?</b> — a system three patch levels
 *       behind the landscape is not just old, it is a different system.</li>
 *   <li><b>Is every component on the level of its own system?</b> — the
 *       component that lags inside its own system is the one that breaks the
 *       upgrade, because SUM moves them together.</li>
 *   <li><b>When was it last patched?</b> — the component list moves on
 *       whether you patch or not.</li>
 * </ol>
 *
 * <p>The score is 100 minus the penalties and exists only so the systems can
 * be sorted. The <em>severity</em> is the worst finding, so an obsolete
 * component can never hide behind an otherwise healthy system.</p>
 */
@Service
public class UpgradeAnalysisService {

    /** Penalty for each finding, and the severity it carries. */
    private static final int PENALTY_OBSOLETE = 30;
    private static final int PENALTY_PATCH_FAILED = 25;
    private static final int PENALTY_DEPENDENCY = 20;
    private static final int PENALTY_VERSION_DRIFT = 20;
    private static final int PENALTY_NO_PATCH_HISTORY = 20;
    private static final int PENALTY_COMPONENT_BEHIND = 15;
    private static final int PENALTY_STALE_WINDOW = 10;

    private final UpgradeRepository repository;
    private final AppProperties properties;

    public UpgradeAnalysisService(UpgradeRepository repository, AppProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public List<UpgradeSystem> systems() {
        return repository.findSystems();
    }

    /** The readiness of every system, worst first. */
    public List<UpgradeAssessment> assessments() {
        UpgradeSnapshot snapshot = UpgradeSnapshot.load(repository);
        List<UpgradeAssessment> results = new ArrayList<UpgradeAssessment>();
        for (UpgradeSystem system : snapshot.systems()) {
            results.add(assess(system, snapshot));
        }
        Collections.sort(results, new Comparator<UpgradeAssessment>() {
            @Override
            public int compare(UpgradeAssessment left, UpgradeAssessment right) {
                int bySeverity = left.getSeverity().rank() - right.getSeverity().rank();
                return bySeverity != 0 ? bySeverity : left.getScore() - right.getScore();
            }
        });
        return results;
    }

    /** The readiness of one system, or null when it does not exist. */
    public UpgradeAssessment assess(String systemId) {
        UpgradeSnapshot snapshot = UpgradeSnapshot.load(repository);
        UpgradeSystem system = null;
        for (UpgradeSystem candidate : snapshot.systems()) {
            if (candidate.getSystemId().equals(systemId)) {
                system = candidate;
            }
        }
        return system == null ? null : assess(system, snapshot);
    }

    /** The readiness of one system. */
    public UpgradeAssessment assess(UpgradeSystem system, UpgradeSnapshot snapshot) {
        UpgradeAssessment assessment = new UpgradeAssessment();
        assessment.setSystem(system);
        assessment.setHighestLevel(snapshot.highestLevel());

        List<UpgradeFinding> findings = new ArrayList<UpgradeFinding>();
        int penalty = 0;
        LocalDateTime now = LocalDateTime.now();

        List<ComponentStatus> statuses = snapshot.statuses(system.getSystemId());
        assessment.setComponentStatuses(statuses);

        int drift = snapshot.highestLevel() - system.getPatchLevel();
        assessment.setDrift(drift < 0 ? 0 : drift);

        // -- 1. can this system be upgraded at all? ---------------------------
        List<ComponentStatus> blockers = new ArrayList<ComponentStatus>();
        for (ComponentStatus status : statuses) {
            if (status.isBlocking()) {
                blockers.add(status);
            }
        }
        assessment.setBlockers(blockers);

        if (!blockers.isEmpty()) {
            penalty += PENALTY_OBSOLETE;
            findings.add(new UpgradeFinding(Severity.CRITICAL, "OBSOLETE_COMPONENT",
                    system.getSystemId(),
                    describe(blockers) + " on " + system.getSystemId()
                            + " cannot be patched - the product has been replaced.",
                    "Migrate " + describe(blockers) + " to "
                            + replacementOf(blockers, snapshot)
                            + " before the next upgrade. An obsolete component is the one "
                            + "thing that stops an upgrade outright, and it is invisible in "
                            + "the patch level."));
        }

        // -- 2. did the last patch work? --------------------------------------
        List<PatchError> errors = snapshot.errors(system.getSystemId());
        assessment.setErrors(errors);

        if (!errors.isEmpty()) {
            penalty += PENALTY_PATCH_FAILED;
            PatchError latest = errors.get(0);
            findings.add(new UpgradeFinding(Severity.CRITICAL, "PATCH_FAILED",
                    system.getSystemId(),
                    "The last patch of " + latest.getComponentId() + " (" + latest.getSpId()
                            + ") failed on " + latest.getAtDisplay() + ": " + latest.getLogText(),
                    "Read the SPAM patch log and resolve it before anything else. A failed "
                            + "patch leaves the system in a state nobody can reason about, "
                            + "and the next upgrade will refuse to start."));
        }

        // -- 3. are the components consistent with each other? ----------------
        List<String> broken = new ArrayList<String>();
        for (ComponentDependency dependency : snapshot.dependencies()) {
            ComponentStatus dependent = snapshot.status(system.getSystemId(),
                    dependency.getComponentId());
            ComponentStatus required = snapshot.status(system.getSystemId(),
                    dependency.getRequiresComponent());
            if (dependent == null || required == null) {
                continue;
            }
            // A component that is being replaced is not compared - it is not
            // going to move with the rest of the system anyway.
            if (dependent.isBlocking() || required.isBlocking()) {
                continue;
            }
            if (dependent.getPatchLevel() > required.getPatchLevel()) {
                broken.add(dependency.getComponentId() + " (level "
                        + dependent.getPatchLevel() + ") is ahead of "
                        + dependency.getRequiresComponent() + " (level "
                        + required.getPatchLevel() + ")");
            }
        }
        if (!broken.isEmpty()) {
            penalty += PENALTY_DEPENDENCY;
            findings.add(new UpgradeFinding(Severity.CRITICAL, "COMPONENT_DEPENDENCY_BROKEN",
                    system.getSystemId(),
                    "On " + system.getSystemId() + ", " + join(broken) + ".",
                    "Bring the dependency forward, or move the dependent component back to "
                            + "the same level. A component ahead of the one it requires is a "
                            + "combination SAP does not support, and SUM will stop."));
        }

        // -- 4. how far behind is the system? ---------------------------------
        if (drift > properties.getPolicy().getMaxSystemDriftLevels()) {
            penalty += PENALTY_VERSION_DRIFT;
            findings.add(new UpgradeFinding(Severity.HIGH, "VERSION_DRIFT",
                    system.getSystemId(),
                    system.getSystemId() + " is on patch level " + system.getPatchLevel()
                            + ", while the landscape has reached level "
                            + snapshot.highestLevel() + " - " + drift + " levels behind.",
                    "Move the system forward. " + drift + " patch levels is not a patching "
                            + "task any more, it is a project, and the gap grows on its own."));
        }

        // -- 5. has it ever been patched? -------------------------------------
        if (system.getLastPatch() == null) {
            penalty += PENALTY_NO_PATCH_HISTORY;
            findings.add(new UpgradeFinding(Severity.HIGH, "NO_PATCH_HISTORY",
                    system.getSystemId(),
                    system.getSystemId() + " has no record of ever having been patched.",
                    "Find out whether that is true. A system with no patch history is either "
                            + "brand new or nobody has been maintaining it, and both need a "
                            + "decision."));
        }

        // -- 6. is every component on the level of its own system? -------------
        List<String> behind = new ArrayList<String>();
        for (ComponentStatus status : statuses) {
            if (status.isBlocking()) {
                continue;
            }
            int behindBy = system.getPatchLevel() - status.getPatchLevel();
            if (behindBy > properties.getPolicy().getComponentDriftLevels()) {
                behind.add(status.getComponentId() + " (level " + status.getPatchLevel() + ")");
            }
        }
        if (!behind.isEmpty()) {
            penalty += PENALTY_COMPONENT_BEHIND;
            findings.add(new UpgradeFinding(Severity.MEDIUM, "COMPONENT_BEHIND",
                    system.getSystemId(),
                    "On " + system.getSystemId() + ", " + join(behind)
                            + " sit behind the system level of " + system.getPatchLevel() + ".",
                    "Move the lagging component forward. SUM moves the components of a system "
                            + "together, so one component left behind holds the whole upgrade "
                            + "up."));
        }

        // -- 7. when was it last patched? -------------------------------------
        int days = system.daysSinceLastPatch(now);
        if (system.getLastPatch() != null
                && days > properties.getPolicy().getStalePatchWindowDays()) {
            penalty += PENALTY_STALE_WINDOW;
            findings.add(new UpgradeFinding(Severity.MEDIUM, "STALE_PATCH_WINDOW",
                    system.getSystemId(),
                    system.getSystemId() + " was last patched " + days + " days ago.",
                    "Patch it. The component list moves on whether you patch or not, and every "
                            + "month in a window makes the next upgrade longer."));
        }

        // -- score and severity ------------------------------------------------
        int score = Math.max(0, 100 - penalty);
        assessment.setScore(score);
        assessment.setFindings(findings);
        assessment.setSeverity(worstSeverity(findings, score));
        return assessment;
    }

    /** The severity of a system is the worst finding it has. */
    private Severity worstSeverity(List<UpgradeFinding> findings, int score) {
        Severity worst = null;
        for (UpgradeFinding finding : findings) {
            if (worst == null || finding.getSeverity().rank() < worst.rank()) {
                worst = finding.getSeverity();
            }
        }
        if (worst != null) {
            return worst;
        }
        if (score >= 80) {
            return Severity.INFO;
        }
        if (score >= 60) {
            return Severity.MEDIUM;
        }
        if (score >= 35) {
            return Severity.HIGH;
        }
        return Severity.CRITICAL;
    }

    /** Every finding across the landscape, most severe first. */
    public List<UpgradeFinding> allFindings() {
        List<UpgradeFinding> findings = new ArrayList<UpgradeFinding>();
        for (UpgradeAssessment assessment : assessments()) {
            findings.addAll(assessment.getFindings());
        }
        Collections.sort(findings, new Comparator<UpgradeFinding>() {
            @Override
            public int compare(UpgradeFinding left, UpgradeFinding right) {
                int bySeverity = left.getSeverity().rank() - right.getSeverity().rank();
                return bySeverity != 0 ? bySeverity
                        : left.getSystemId().compareTo(right.getSystemId());
            }
        });
        return findings;
    }

    /** How many systems are in each severity band. */
    public int countBySeverity(Severity severity) {
        int count = 0;
        for (UpgradeAssessment assessment : assessments()) {
            if (assessment.getSeverity() == severity) {
                count++;
            }
        }
        return count;
    }

    /** The average readiness score across the landscape. */
    public int averageScore() {
        List<UpgradeAssessment> all = assessments();
        if (all.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (UpgradeAssessment assessment : all) {
            total += assessment.getScore();
        }
        return Math.round((float) total / all.size());
    }

    /** How many components block an upgrade across the landscape. */
    public int blockerCount() {
        int count = 0;
        for (UpgradeAssessment assessment : assessments()) {
            count += assessment.getBlockers().size();
        }
        return count;
    }

    private String describe(List<ComponentStatus> blockers) {
        List<String> names = new ArrayList<String>();
        for (ComponentStatus blocker : blockers) {
            names.add(blocker.getComponentId());
        }
        return join(names);
    }

    private String replacementOf(List<ComponentStatus> blockers, UpgradeSnapshot snapshot) {
        List<String> names = new ArrayList<String>();
        for (ComponentStatus blocker : blockers) {
            SoftwareComponent component = snapshot.component(blocker.getComponentId());
            names.add(component == null || component.getReplacedBy() == null
                    || component.getReplacedBy().isEmpty()
                    ? "the current product"
                    : component.getReplacedBy());
        }
        return join(names);
    }

    private static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(value);
        }
        return out.toString();
    }

    /** The upgrade readiness report, as a Markdown document. */
    public String renderReport() {
        List<UpgradeAssessment> assessments = assessments();
        StringBuilder out = new StringBuilder();
        out.append("# Upgrade readiness report\n\n");
        out.append("Generated by SAP Upgrade & Support Package Readiness.\n\n");

        out.append("## Summary\n\n");
        out.append("| System | Description | Role | Patch level | Drift | Score | Status | Findings |\n");
        out.append("|---|---|---|---|---|---|---|---|\n");
        for (UpgradeAssessment assessment : assessments) {
            UpgradeSystem system = assessment.getSystem();
            out.append("| ").append(system.getSystemId())
                    .append(" | ").append(system.getDescription())
                    .append(" | ").append(system.getSystemRole())
                    .append(" | ").append(system.getPatchLevel())
                    .append(" | ").append(assessment.getDrift())
                    .append(" | ").append(assessment.getScore())
                    .append(" | ").append(assessment.getSeverity())
                    .append(" | ").append(assessment.findingCount())
                    .append(" |\n");
        }
        out.append("\n");

        out.append("## Findings\n\n");
        for (UpgradeAssessment assessment : assessments) {
            if (assessment.getFindings().isEmpty()) {
                continue;
            }
            UpgradeSystem system = assessment.getSystem();
            out.append("### ").append(system.getSystemId())
                    .append(" - ").append(system.getDescription()).append("\n\n");
            for (UpgradeFinding finding : assessment.getFindings()) {
                out.append("- **").append(finding.getSeverity()).append(" - ")
                        .append(finding.getCategory()).append("**: ")
                        .append(finding.getMessage()).append("  \n  ")
                        .append(finding.getRecommendation()).append("\n");
            }
            out.append("\n");
        }

        out.append("## Upgrade blockers\n\n");
        boolean anyBlocker = false;
        for (UpgradeAssessment assessment : assessments) {
            if (assessment.getBlockers().isEmpty()) {
                continue;
            }
            anyBlocker = true;
            out.append("| System | Component | State | Patch level |\n");
            out.append("|---|---|---|---|\n");
            for (ComponentStatus blocker : assessment.getBlockers()) {
                out.append("| ").append(assessment.getSystem().getSystemId())
                        .append(" | ").append(blocker.getComponentId())
                        .append(" | ").append(blocker.getStatus())
                        .append(" | ").append(blocker.getPatchLevel())
                        .append(" |\n");
            }
            out.append("\n");
        }
        if (!anyBlocker) {
            out.append("No obsolete components.\n\n");
        }

        out.append("## Component state\n\n");
        out.append("| System | Component | Patch level | Status |\n");
        out.append("|---|---|---|---|\n");
        for (UpgradeAssessment assessment : assessments) {
            for (ComponentStatus status : assessment.getComponentStatuses()) {
                out.append("| ").append(assessment.getSystem().getSystemId())
                        .append(" | ").append(status.getComponentId())
                        .append(" | ").append(status.getPatchLevel())
                        .append(" | ").append(status.getStatus())
                        .append(" |\n");
            }
        }
        out.append("\n");

        out.append("---\n\n");
        out.append("> A patch level is not a number. It is a promise that every component "
                + "on that system is actually there.\n");
        return out.toString();
    }
}
