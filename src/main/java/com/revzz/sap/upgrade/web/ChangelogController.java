package com.revzz.sap.upgrade.web;

import com.revzz.sap.upgrade.domain.ChangeEntry;
import com.revzz.sap.upgrade.domain.SupportPackage;
import com.revzz.sap.upgrade.service.ChangelogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * The support package changelog.
 *
 * <p>The part of the upgrade that everybody skips and everybody needs: the
 * incompatible changes. They are the entries that break custom code in a
 * system that was tested three months ago.</p>
 */
@Controller
@RequireRole({"ANALYST", "UPGRADE_ADMIN"})
public class ChangelogController {

    private final ChangelogService changelogService;

    public ChangelogController(ChangelogService changelogService) {
        this.changelogService = changelogService;
    }

    @GetMapping("/changelog")
    public String changelog(Model model) {
        List<SupportPackage> packages = changelogService.packagesNewestFirst();
        List<ChangeEntry> all = changelogService.allChangelog();

        int breaking = 0;
        java.util.Map<String, Integer> breakingByPackage =
                new java.util.LinkedHashMap<String, Integer>();
        for (SupportPackage pack : packages) {
            int count = changelogService.breakingCount(pack.getSpId());
            breakingByPackage.put(pack.getSpId(), count);
            breaking += count;
        }

        model.addAttribute("packages", packages);
        model.addAttribute("entries", all);
        model.addAttribute("breakingCount", breaking);
        model.addAttribute("breakingByPackage", breakingByPackage);
        model.addAttribute("entryCount", all.size());
        return "changelog";
    }

    @GetMapping("/changelog/{spId}")
    public String packageChangelog(@PathVariable String spId, Model model) {
        SupportPackage pack = changelogService.findPackage(spId);
        if (pack == null) {
            model.addAttribute("error", "There is no support package called " + spId + ".");
            return "error";
        }
        List<ChangeEntry> entries = changelogService.changelog(spId);
        List<ChangeEntry> breaking = changelogService.breakingEntries(spId);

        model.addAttribute("pack", pack);
        model.addAttribute("entries", entries);
        model.addAttribute("breaking", breaking);
        model.addAttribute("breakingCount", breaking.size());
        model.addAttribute("entryCount", entries.size());
        return "changelog-package";
    }
}
