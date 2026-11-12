package com.revzz.sap.upgrade.integration;

import com.revzz.sap.upgrade.domain.ComponentStatus;
import com.revzz.sap.upgrade.domain.UpgradeSystem;
import com.sap.jco.JCoDestination;
import com.sap.jco.JCoException;
import com.sap.jco.JCoFunction;
import com.sap.jco.JCoTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Live SAP gateway - RFC through the SAP Java Connector.
 *
 * <p><b>Only compiled when the {@code with-jco} Maven profile is active</b>,
 * because the SAP Java Connector is published on SAP's own Maven repository
 * rather than on Maven Central. The default build therefore compiles and
 * tests anywhere.</p>
 *
 * <pre>
 *   mvn -Pwith-jco clean package
 * </pre>
 *
 * <p>The remote function module {@code Z_SPAM_STATUS} must be
 * <b>Remote Enabled</b> in transaction SE37, otherwise SAP will refuse the
 * call with {@code RFC function module not found} - which is the single most
 * common reason an RFC adapter that "should work" does not.</p>
 */
@Component
@Primary
@ConditionalOnProperty(name = "upgrade.gateway", havingValue = "jco")
public class RfcUpgradeGateway implements UpgradeGateway {

    private static final Logger LOG = LoggerFactory.getLogger(RfcUpgradeGateway.class);

    /** RFC-enabled ABAP function module that reads the support package history. */
    private static final String FUNCTION_SPAM_STATUS = "Z_SPAM_STATUS";

    private final String destinationName;

    public RfcUpgradeGateway() {
        this("SAPPRD");
    }

    public RfcUpgradeGateway(String destinationName) {
        this.destinationName = destinationName;
    }

    @Override
    public String description() {
        return "SAP system over RFC (" + destinationName + ")";
    }

    @Override
    public List<UpgradeSystem> fetchSystems() {
        List<UpgradeSystem> systems = new ArrayList<UpgradeSystem>();
        try {
            JCoDestination destination = buildDestination();
            JCoFunction function = JCoFunction.getInstance(destination, FUNCTION_SPAM_STATUS);
            function.execute(destination);

            JCoTable table = function.getTable("IT_PACKAGES");
            if (table == null) {
                return systems;
            }
            table.first();
            while (!table.isEof()) {
                UpgradeSystem system = new UpgradeSystem();
                system.setSystemId(table.getField("SP_DIR").getString());
                system.setDescription(table.getField("SP_NAME").getString());
                system.setPatchLevel(table.getField("SP_LEVEL").getInt());
                system.setPatchedBy(table.getField("SP_USER").getString());
                system.setLastPatch(toLocalDateTime(table.getField("SP_DATE").getString()));
                systems.add(system);
                table.next();
            }
            LOG.info("Fetched {} support package records over RFC", systems.size());
        } catch (JCoException exception) {
            LOG.error("Could not read the support package history over RFC: {}",
                    exception.getMessage());
        }
        return systems;
    }

    @Override
    public List<ComponentStatus> fetchComponentStates(String systemId) {
        // The component state - which product has been replaced, which
        // component is obsolete - is held by the Software Update Manager in
        // its own component list on the application server, not in the
        // database. A live implementation reads that list; the RFC path
        // therefore returns nothing for it rather than guessing.
        return new ArrayList<ComponentStatus>();
    }

    private LocalDateTime toLocalDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim().replace(' ', 'T'));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private JCoDestination buildDestination() throws JCoException {
        Properties properties = new Properties();
        try {
            java.io.InputStream stream = new java.io.FileInputStream("jco.properties");
            try {
                properties.load(stream);
            } finally {
                stream.close();
            }
        } catch (java.io.IOException exception) {
            throw new JCoException("jco.properties not found - copy jco.properties.example");
        }

        JCoDestination destination = JCoDestination.createDestination(destinationName);
        destination.setApplicationServer(properties.getProperty("sap.applicationServer"));
        destination.setSystemNumber(properties.getProperty("sap.systemNumber"));
        destination.setClient(properties.getProperty("sap.client"));
        destination.setUser(properties.getProperty("sap.user"));
        destination.setPassword(properties.getProperty("sap.password"));
        destination.setPoolSize(2);
        return destination;
    }
}
