package com.revzz.sap.upgrade.web;

import com.revzz.sap.upgrade.domain.ChangeEntry;
import com.revzz.sap.upgrade.domain.SupportPackage;
import com.revzz.sap.upgrade.domain.UpgradeAssessment;
import com.revzz.sap.upgrade.domain.UpgradeFinding;
import com.revzz.sap.upgrade.service.ChangelogService;
import com.revzz.sap.upgrade.service.UpgradeAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for reporting and integrations.
 *
 * <p>Read-only on purpose: the API exposes the same analysis the screens
 * show, so an upgrade board or a monitoring script can consume it without
 * scraping HTML.</p>
 *
 * <p>Every endpoint except the health check needs a signed-in session. The
 * data describes how close a production system is to being un-upgradeable,
 * so it is not something to serve to anyone who asks.</p>
 */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final UpgradeAnalysisService analysisService;
    private final ChangelogService changelogService;

    public ApiController(UpgradeAnalysisService analysisService,
                         ChangelogService changelogService) {
        this.analysisService = analysisService;
        this.changelogService = changelogService;
    }

    /** The health check stays open - a load balancer has to be able to reach it. */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> health() {
        Map<String, Object> health = new LinkedHashMap<String, Object>();
        health.put("status", "UP");
        health.put("systems", analysisService.systems().size());
        health.put("blockers", analysisService.blockerCount());
        health.put("averageScore", analysisService.averageScore());
        return health;
    }

    @GetMapping(value = "/readiness", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> readiness(HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(analysisService.assessments());
    }

    @GetMapping(value = "/systems", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> systems(HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(analysisService.systems());
    }

    @GetMapping(value = "/systems/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> system(@PathVariable String id, HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        UpgradeAssessment assessment = analysisService.assess(id);
        if (assessment == null) {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("error", "no such system");
            body.put("system", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        return ResponseEntity.ok(assessment);
    }

    @GetMapping(value = "/findings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findings(HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(analysisService.allFindings());
    }

    @GetMapping(value = "/changelog", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> changelog(HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(changelogService.allChangelog());
    }

    @GetMapping(value = "/changelog/{spId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> packageChangelog(@PathVariable String spId,
                                                   HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        SupportPackage pack = changelogService.findPackage(spId);
        if (pack == null) {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("error", "no such support package");
            body.put("sp", spId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("package", pack);
        List<ChangeEntry> entries = changelogService.changelog(spId);
        body.put("entries", entries);
        body.put("breakingCount", changelogService.breakingCount(spId));
        return ResponseEntity.ok(body);
    }

    /**
     * Rejects an anonymous caller.
     *
     * @return null when the caller is signed in, otherwise a 401 with a JSON body
     */
    private ResponseEntity<Object> requireSession(HttpSession session) {
        if (CurrentUser.isSignedIn(session)) {
            return null;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("error", "authentication required");
        body.put("hint", "sign in at /login and send the session cookie");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
