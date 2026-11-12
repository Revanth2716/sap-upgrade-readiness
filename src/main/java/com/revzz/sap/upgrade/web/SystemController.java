package com.revzz.sap.upgrade.web;

import com.revzz.sap.upgrade.domain.ComponentStatus;
import com.revzz.sap.upgrade.domain.PatchError;
import com.revzz.sap.upgrade.domain.SoftwareComponent;
import com.revzz.sap.upgrade.domain.UpgradeAssessment;
import com.revzz.sap.upgrade.domain.UpgradeSystem;
import com.revzz.sap.upgrade.repo.UpgradeRepository;
import com.revzz.sap.upgrade.service.AuditService;
import com.revzz.sap.upgrade.service.UpgradeAnalysisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One system, in detail.
 *
 * <p>The detail screen is where an upgrade project actually works: the
 * component states, the components that block the upgrade, the patch log
 * and the findings with the action each one needs. It is also the only
 * place a patch attempt can be recorded - recording the failure is the write
 * path of this application.</p>
 */
@Controller
public class SystemController {

    private final UpgradeAnalysisService analysisService;
    private final UpgradeRepository repository;
    private final AuditService auditService;

    public SystemController(UpgradeAnalysisService analysisService,
                            UpgradeRepository repository,
                            AuditService auditService) {
        this.analysisService = analysisService;
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping("/systems/{id}")
    public String system(@PathVariable String id, Model model) {
        UpgradeAssessment assessment = analysisService.assess(id);
        if (assessment == null) {
            model.addAttribute("error", "There is no system called " + id + ".");
            return "error";
        }

        List<SoftwareComponent> catalogue = repository.findComponents();
        List<String> blockers = new ArrayList<String>();
        java.util.Map<String, String> descriptions =
                new java.util.LinkedHashMap<String, String>();
        for (ComponentStatus blocker : assessment.getBlockers()) {
            blockers.add(blocker.getComponentId());
        }
        for (SoftwareComponent component : catalogue) {
            descriptions.put(component.getComponentId(), component.getDescription());
        }

        model.addAttribute("assessment", assessment);
        model.addAttribute("system", assessment.getSystem());
        model.addAttribute("catalogue", catalogue);
        model.addAttribute("descriptions", descriptions);
        model.addAttribute("blockerList", blockers);
        model.addAttribute("errors", assessment.getErrors());
        return "system";
    }

    /**
     * Records a patch attempt that failed.
     *
     * <p>The failure is the evidence that matters: a patch that failed leaves
     * the system in a state nobody can reason about, and until somebody
     * records it, the next upgrade attempt finds out the hard way.</p>
     */
    @PostMapping("/systems/{id}/patch-error")
    @RequireRole("UPGRADE_ADMIN")
    public String recordPatchError(@PathVariable String id,
                                   @RequestParam String componentId,
                                   @RequestParam String spId,
                                   @RequestParam(required = false) String logText,
                                   Model model,
                                   HttpServletRequest request) {
        UpgradeSystem system = repository.findSystem(id);
        if (system == null) {
            model.addAttribute("error", "There is no system called " + id + ".");
            return "error";
        }

        PatchError error = new PatchError();
        error.setErrorId("ER" + System.currentTimeMillis() % 100000);
        error.setSystemId(id);
        error.setComponentId(componentId);
        error.setSpId(spId);
        error.setLogText(logText == null || logText.trim().isEmpty()
                ? "The patch was aborted in the import phase."
                : logText.trim());
        error.setAt(LocalDateTime.now());
        repository.insertError(error);

        auditService.record(CurrentUser.username(request.getSession(false)),
                "PATCH_ERROR_RECORDED",
                id + ": the patch of " + componentId + " (" + spId + ") was recorded as failed");
        return "redirect:/systems/" + id + "?recorded=1";
    }
}
