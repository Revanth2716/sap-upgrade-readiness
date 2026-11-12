package com.revzz.sap.upgrade.service;

import com.revzz.sap.upgrade.domain.ComponentDependency;
import com.revzz.sap.upgrade.domain.ComponentStatus;
import com.revzz.sap.upgrade.domain.PatchError;
import com.revzz.sap.upgrade.domain.SoftwareComponent;
import com.revzz.sap.upgrade.domain.UpgradeSystem;
import com.revzz.sap.upgrade.repo.UpgradeRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A consistent view of the whole upgrade landscape, loaded once.
 *
 * <p>The drift rule needs the highest patch level anywhere in the landscape,
 * and the dependency rule needs the level of another component. Loading that
 * per system would be an N+1 query per finding, so the landscape is loaded
 * once and every assessment reads from it.</p>
 */
public class UpgradeSnapshot {

    private final List<UpgradeSystem> systems;
    private final Map<String, List<ComponentStatus>> statusBySystem;
    private final Map<String, ComponentStatus> statusByKey;
    private final Map<String, SoftwareComponent> components;
    private final List<ComponentDependency> dependencies;
    private final Map<String, List<PatchError>> errorsBySystem;
    private final int highestLevel;

    private UpgradeSnapshot(List<UpgradeSystem> systems,
                            Map<String, List<ComponentStatus>> statusBySystem,
                            Map<String, SoftwareComponent> components,
                            List<ComponentDependency> dependencies,
                            Map<String, List<PatchError>> errorsBySystem) {
        this.systems = systems;
        this.statusBySystem = statusBySystem;
        this.components = components;
        this.dependencies = dependencies;
        this.errorsBySystem = errorsBySystem;

        this.statusByKey = new HashMap<String, ComponentStatus>();
        int highest = 0;
        for (UpgradeSystem system : systems) {
            highest = Math.max(highest, system.getPatchLevel());
        }
        for (ComponentStatus status : repositoryStatus(statusBySystem)) {
            statusByKey.put(key(status.getSystemId(), status.getComponentId()), status);
        }
        this.highestLevel = highest;
    }

    private static List<ComponentStatus> repositoryStatus(
            Map<String, List<ComponentStatus>> statusBySystem) {
        List<ComponentStatus> all = new ArrayList<ComponentStatus>();
        for (List<ComponentStatus> list : statusBySystem.values()) {
            all.addAll(list);
        }
        return all;
    }

    public static UpgradeSnapshot load(UpgradeRepository repository) {
        List<UpgradeSystem> systems = repository.findSystems();

        Map<String, List<ComponentStatus>> statusBySystem =
                new HashMap<String, List<ComponentStatus>>();
        for (ComponentStatus status : repository.findAllStatus()) {
            List<ComponentStatus> list = statusBySystem.get(status.getSystemId());
            if (list == null) {
                list = new ArrayList<ComponentStatus>();
                statusBySystem.put(status.getSystemId(), list);
            }
            list.add(status);
        }

        Map<String, SoftwareComponent> components =
                new HashMap<String, SoftwareComponent>();
        for (SoftwareComponent component : repository.findComponents()) {
            components.put(component.getComponentId(), component);
        }

        Map<String, List<PatchError>> errorsBySystem =
                new HashMap<String, List<PatchError>>();
        for (PatchError error : repository.findAllErrors()) {
            List<PatchError> list = errorsBySystem.get(error.getSystemId());
            if (list == null) {
                list = new ArrayList<PatchError>();
                errorsBySystem.put(error.getSystemId(), list);
            }
            list.add(error);
        }

        return new UpgradeSnapshot(systems, statusBySystem, components,
                repository.findDependencies(), errorsBySystem);
    }

    private static String key(String systemId, String componentId) {
        return systemId + "/" + componentId;
    }

    public List<UpgradeSystem> systems() {
        return systems;
    }

    public int highestLevel() {
        return highestLevel;
    }

    public List<ComponentStatus> statuses(String systemId) {
        List<ComponentStatus> list = statusBySystem.get(systemId);
        return list == null ? Collections.<ComponentStatus>emptyList() : list;
    }

    public ComponentStatus status(String systemId, String componentId) {
        return statusByKey.get(key(systemId, componentId));
    }

    public SoftwareComponent component(String componentId) {
        return components.get(componentId);
    }

    public List<ComponentDependency> dependencies() {
        return dependencies;
    }

    public List<PatchError> errors(String systemId) {
        List<PatchError> list = errorsBySystem.get(systemId);
        return list == null ? Collections.<PatchError>emptyList() : list;
    }
}
