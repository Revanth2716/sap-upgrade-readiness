package com.revzz.sap.upgrade.integration;

import com.revzz.sap.upgrade.domain.ComponentStatus;
import com.revzz.sap.upgrade.domain.UpgradeSystem;

import java.util.List;

/**
 * The boundary between the analyzer and a real SAP landscape.
 *
 * <p>Everything above this interface works on plain domain objects and knows
 * nothing about RFC, JCo or ABAP. Everything below it knows how to ask a
 * real system for its patch history.</p>
 *
 * <p>Two implementations exist:</p>
 * <ul>
 *   <li>{@code LocalUpgradeGateway} - the default, reads the local
 *       database. It is what the demo runs on.</li>
 *   <li>{@code RfcUpgradeGateway} - the live path, calls the RFC-enabled
 *       ABAP function module {@code Z_SPAM_STATUS} through the SAP Java
 *       Connector. It is only compiled when the {@code with-jco} Maven
 *       profile is active, because the SAP Java Connector is not published
 *       on Maven Central.</li>
 * </ul>
 */
public interface UpgradeGateway {

    /** A short description of where the data comes from, shown in the UI. */
    String description();

    /** The systems of the landscape. */
    List<UpgradeSystem> fetchSystems();

    /** The component states of one system. */
    List<ComponentStatus> fetchComponentStates(String systemId);
}
