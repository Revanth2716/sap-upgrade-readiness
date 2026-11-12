package com.revzz.sap.upgrade.web;

import com.revzz.sap.upgrade.service.AuditService;
import com.revzz.sap.upgrade.service.UpgradeAnalysisService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

/**
 * The upgrade readiness report.
 *
 * <p>The deliverable: a document that can be attached to an upgrade project
 * plan, pasted into a risk register or read out in a change board.</p>
 */
@Controller
@RequireRole({"ANALYST", "UPGRADE_ADMIN"})
public class ReportController {

    private final UpgradeAnalysisService analysisService;
    private final AuditService auditService;

    public ReportController(UpgradeAnalysisService analysisService, AuditService auditService) {
        this.analysisService = analysisService;
        this.auditService = auditService;
    }

    @GetMapping("/report")
    public String report(Model model, HttpServletRequest request) {
        String markdown = analysisService.renderReport();
        model.addAttribute("markdown", markdown);
        model.addAttribute("lineCount", markdown.split("\n").length);

        auditService.record(CurrentUser.username(request.getSession(false)), "REPORT_VIEWED",
                "Upgrade readiness report opened");
        return "report";
    }

    /** The same report as a downloadable Markdown document. */
    @GetMapping("/report.md")
    public ResponseEntity<String> download(HttpServletRequest request) {
        String markdown = analysisService.renderReport();

        auditService.record(CurrentUser.username(request.getSession(false)), "REPORT_EXPORTED",
                "Upgrade readiness report exported as Markdown");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "markdown", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", "upgrade-readiness-report.md");
        return new ResponseEntity<String>(markdown, headers, HttpStatus.OK);
    }
}
