package com.revzz.sap.upgrade.integration;

import com.revzz.sap.upgrade.domain.ComponentStatus;
import com.revzz.sap.upgrade.domain.UpgradeSystem;
import com.revzz.sap.upgrade.repo.UpgradeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The default gateway: reads the patch data from the local database.
 *
 * <p>This is what the application runs with when there is no SAP system to
 * talk to. It is also the implementation the unit tests exercise, which is
 * why the rest of the code never depends on RFC being available.</p>
 */
@Component
@ConditionalOnProperty(name = "upgrade.gateway", havingValue = "local", matchIfMissing = true)
public class LocalUpgradeGateway implements UpgradeGateway {

    private final UpgradeRepository repository;

    public LocalUpgradeGateway(UpgradeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String description() {
        return "Local database";
    }

    @Override
    public List<UpgradeSystem> fetchSystems() {
        return repository.findSystems();
    }

    @Override
    public List<ComponentStatus> fetchComponentStates(String systemId) {
        return repository.findStatus(systemId);
    }
}
