package com.revzz.sap.upgrade.web;

import com.revzz.sap.upgrade.domain.Severity;
import com.revzz.sap.upgrade.domain.SoftwareComponent;
import com.revzz.sap.upgrade.domain.UpgradeAssessment;
import com.revzz.sap.upgrade.domain.UpgradeFinding;
import com.revzz.sap.upgrade.repo.UpgradeRepository;
import com.revzz.sap.upgrade.service.AuditService;
import com.revzz.sap.upgrade.service.UpgradeAnalysisService;
import com.revzz.sap.upgrade.support.UpgradeChartRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * The upgrade readiness dashboard.
 *
 * <p>The first screen answers one question: which system cannot be upgraded,
 * and what exactly is stopping it.</p>
 */
@Controller
public class DashboardController {

    private final UpgradeAnalysisService analysisService;
    private final UpgradeRepository repository;
    private final AuditService auditService;

    public DashboardController(UpgradeAnalysisService analysisService,
                               UpgradeRepository repository,
                               AuditService auditService) {
        this.analysisService = analysisService;
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<UpgradeAssessment> assessments = analysisService.assessments();
        List<UpgradeFinding> findings = analysisService.allFindings();
        List<SoftwareComponent> components = repository.findComponents();

        model.addAttribute("assessments", assessments);
        model.addAttribute("findings", findings);
        model.addAttribute("components", components);
        model.addAttribute("scoreChart", UpgradeChartRenderer.readinessBars(assessments));
        model.addAttribute("matrixChart", UpgradeChartRenderer.componentMatrix(assessments,
                components));
        model.addAttribute("systemCount", assessments.size());
        model.addAttribute("blockerCount", analysisService.blockerCount());
        model.addAttribute("criticalCount", analysisService.countBySeverity(Severity.CRITICAL));
        model.addAttribute("highCount", analysisService.countBySeverity(Severity.HIGH));
        model.addAttribute("averageScore", analysisService.averageScore());
        model.addAttribute("findingCount", findings.size());
        model.addAttribute("audit", auditService.recent(8));
        return "overview";
    }
}
