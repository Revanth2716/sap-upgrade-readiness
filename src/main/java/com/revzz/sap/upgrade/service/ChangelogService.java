package com.revzz.sap.upgrade.service;

import com.revzz.sap.upgrade.domain.ChangeEntry;
import com.revzz.sap.upgrade.domain.SupportPackage;
import com.revzz.sap.upgrade.repo.UpgradeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The support package changelog.
 *
 * <p>This is the part of the upgrade that everybody skips and everybody
 * needs. A support package does not only bring features - it brings
 * <b>incompatible changes</b>, and those are the entries that break custom
 * code in a system that was tested three months ago.</p>
 *
 * <p>The changelog itself is a SAP-published document, not something the
 * database holds. It is maintained here, and the entries are categorised so
 * that the breaking ones can be pulled out on their own.</p>
 */
@Service
public class ChangelogService {

    private final UpgradeRepository repository;

    public ChangelogService(UpgradeRepository repository) {
        this.repository = repository;
    }

    public List<SupportPackage> packages() {
        return repository.findPackages();
    }

    public SupportPackage findPackage(String spId) {
        return repository.findPackage(spId);
    }

    /** The changelog of one support package. */
    public List<ChangeEntry> changelog(String spId) {
        return repository.findChangelog(spId);
    }

    /** Every changelog entry in the landscape. */
    public List<ChangeEntry> allChangelog() {
        return repository.findAllChangelog();
    }

    /** The entries of one component in one support package. */
    public List<ChangeEntry> changelog(String spId, String componentId) {
        List<ChangeEntry> matching = new ArrayList<ChangeEntry>();
        for (ChangeEntry entry : repository.findChangelog(spId)) {
            if (entry.getComponentId().equals(componentId)) {
                matching.add(entry);
            }
        }
        return matching;
    }

    /**
     * The incompatible changes of one support package.
     *
     * <p>The list a test team needs before the regression window opens.</p>
     */
    public List<ChangeEntry> breakingEntries(String spId) {
        List<ChangeEntry> breaking = new ArrayList<ChangeEntry>();
        for (ChangeEntry entry : repository.findChangelog(spId)) {
            if (entry.isBreaking()) {
                breaking.add(entry);
            }
        }
        return breaking;
    }

    /** How many incompatible changes a support package carries. */
    public int breakingCount(String spId) {
        return breakingEntries(spId).size();
    }

    /** How many entries a support package carries, in total. */
    public int entryCount(String spId) {
        return repository.findChangelog(spId).size();
    }

    /** The support packages that carry at least one incompatible change. */
    public List<SupportPackage> packagesWithBreakingChanges() {
        List<SupportPackage> result = new ArrayList<SupportPackage>();
        for (SupportPackage pack : repository.findPackages()) {
            if (!breakingEntries(pack.getSpId()).isEmpty()) {
                result.add(pack);
            }
        }
        return result;
    }

    /** The support packages, newest first. */
    public List<SupportPackage> packagesNewestFirst() {
        List<SupportPackage> packages = new ArrayList<SupportPackage>(repository.findPackages());
        Collections.sort(packages, new java.util.Comparator<SupportPackage>() {
            @Override
            public int compare(SupportPackage left, SupportPackage right) {
                return right.getPatchLevel() - left.getPatchLevel();
            }
        });
        return packages;
    }
}
