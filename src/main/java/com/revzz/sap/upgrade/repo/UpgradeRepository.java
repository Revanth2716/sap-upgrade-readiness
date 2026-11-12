package com.revzz.sap.upgrade.repo;

import com.revzz.sap.upgrade.domain.ChangeEntry;
import com.revzz.sap.upgrade.domain.ComponentDependency;
import com.revzz.sap.upgrade.domain.ComponentStatus;
import com.revzz.sap.upgrade.domain.PatchError;
import com.revzz.sap.upgrade.domain.SoftwareComponent;
import com.revzz.sap.upgrade.domain.SupportPackage;
import com.revzz.sap.upgrade.domain.UpgradeSystem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * The upgrade data: the landscape, the component states, the support
 * packages, the changelog and the patch log.
 *
 * <p>All statements are portable ANSI SQL, so the same repository runs
 * against the local H2 database and against a live SAP HANA instance.</p>
 */
@Repository
public class UpgradeRepository {

    private final JdbcTemplate jdbc;

    public UpgradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<UpgradeSystem> SYSTEM_MAPPER =
            new RowMapper<UpgradeSystem>() {
                @Override
                public UpgradeSystem mapRow(ResultSet rs, int rowNum) throws SQLException {
                    UpgradeSystem system = new UpgradeSystem();
                    system.setSystemId(rs.getString("SYSTEM_ID"));
                    system.setDescription(rs.getString("DESCRIPTION"));
                    system.setSystemRole(rs.getString("SYS_ROLE"));
                    system.setTarget("Y".equalsIgnoreCase(rs.getString("IS_TARGET")));
                    system.setPatchLevel(rs.getInt("PATCH_LEVEL"));
                    Timestamp patched = rs.getTimestamp("LAST_PATCH");
                    system.setLastPatch(patched == null ? null : patched.toLocalDateTime());
                    system.setPatchedBy(rs.getString("PATCHED_BY"));
                    return system;
                }
            };

    private static final RowMapper<ComponentStatus> STATUS_MAPPER =
            new RowMapper<ComponentStatus>() {
                @Override
                public ComponentStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
                    ComponentStatus status = new ComponentStatus();
                    status.setSystemId(rs.getString("SYSTEM_ID"));
                    status.setComponentId(rs.getString("COMPONENT_ID"));
                    status.setPatchLevel(rs.getInt("PATCH_LEVEL"));
                    status.setStatus(rs.getString("STATUS"));
                    status.setStatusText(rs.getString("STATUS_TEXT"));
                    Timestamp changed = rs.getTimestamp("LAST_CHANGED");
                    status.setLastChanged(changed == null ? null : changed.toLocalDateTime());
                    return status;
                }
            };

    private static final RowMapper<SoftwareComponent> COMPONENT_MAPPER =
            new RowMapper<SoftwareComponent>() {
                @Override
                public SoftwareComponent mapRow(ResultSet rs, int rowNum) throws SQLException {
                    SoftwareComponent component = new SoftwareComponent();
                    component.setComponentId(rs.getString("COMPONENT_ID"));
                    component.setDescription(rs.getString("DESCRIPTION"));
                    component.setProduct(rs.getString("PRODUCT"));
                    component.setReplacedBy(rs.getString("REPLACED_BY"));
                    return component;
                }
            };

    private static final RowMapper<SupportPackage> PACKAGE_MAPPER =
            new RowMapper<SupportPackage>() {
                @Override
                public SupportPackage mapRow(ResultSet rs, int rowNum) throws SQLException {
                    SupportPackage pack = new SupportPackage();
                    pack.setSpId(rs.getString("SP_ID"));
                    pack.setPatchLevel(rs.getInt("PATCH_LEVEL"));
                    Date released = rs.getDate("RELEASED_ON");
                    pack.setReleasedOn(released == null ? null : released.toLocalDate());
                    pack.setDescription(rs.getString("DESCRIPTION"));
                    return pack;
                }
            };

    private static final RowMapper<ChangeEntry> CHANGELOG_MAPPER =
            new RowMapper<ChangeEntry>() {
                @Override
                public ChangeEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
                    ChangeEntry entry = new ChangeEntry();
                    entry.setEntryId(rs.getString("ENTRY_ID"));
                    entry.setSpId(rs.getString("SP_ID"));
                    entry.setComponentId(rs.getString("COMPONENT_ID"));
                    entry.setEntryText(rs.getString("ENTRY_TEXT"));
                    entry.setCategory(rs.getString("CATEGORY"));
                    return entry;
                }
            };

    private static final RowMapper<ComponentDependency> DEPENDENCY_MAPPER =
            new RowMapper<ComponentDependency>() {
                @Override
                public ComponentDependency mapRow(ResultSet rs, int rowNum) throws SQLException {
                    ComponentDependency dependency = new ComponentDependency();
                    dependency.setDependencyId(rs.getString("DEP_ID"));
                    dependency.setComponentId(rs.getString("COMPONENT_ID"));
                    dependency.setRequiresComponent(rs.getString("REQUIRES_COMPONENT"));
                    return dependency;
                }
            };

    private static final RowMapper<PatchError> ERROR_MAPPER =
            new RowMapper<PatchError>() {
                @Override
                public PatchError mapRow(ResultSet rs, int rowNum) throws SQLException {
                    PatchError error = new PatchError();
                    error.setErrorId(rs.getString("ERROR_ID"));
                    error.setSystemId(rs.getString("SYSTEM_ID"));
                    error.setComponentId(rs.getString("COMPONENT_ID"));
                    error.setSpId(rs.getString("SP_ID"));
                    error.setLogText(rs.getString("LOG_TEXT"));
                    Timestamp at = rs.getTimestamp("AT_TS");
                    error.setAt(at == null ? null : at.toLocalDateTime());
                    return error;
                }
            };

    // -- the landscape --------------------------------------------------------

    public List<UpgradeSystem> findSystems() {
        return jdbc.query("SELECT SYSTEM_ID, DESCRIPTION, SYS_ROLE, IS_TARGET, PATCH_LEVEL, "
                + "LAST_PATCH, PATCHED_BY FROM SP_SYSTEM ORDER BY IS_TARGET DESC, SYSTEM_ID",
                SYSTEM_MAPPER);
    }

    public UpgradeSystem findSystem(String systemId) {
        List<UpgradeSystem> rows = jdbc.query("SELECT SYSTEM_ID, DESCRIPTION, SYS_ROLE, "
                        + "IS_TARGET, PATCH_LEVEL, LAST_PATCH, PATCHED_BY FROM SP_SYSTEM "
                        + "WHERE SYSTEM_ID = ?", SYSTEM_MAPPER, systemId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int countSystems() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM SP_SYSTEM", Integer.class);
        return count == null ? 0 : count;
    }

    public void insertSystem(UpgradeSystem system) {
        jdbc.update("INSERT INTO SP_SYSTEM (SYSTEM_ID, DESCRIPTION, SYS_ROLE, IS_TARGET, "
                        + "PATCH_LEVEL, LAST_PATCH, PATCHED_BY) VALUES (?,?,?,?,?,?,?)",
                system.getSystemId(), system.getDescription(), system.getSystemRole(),
                system.isTarget() ? "Y" : "N", system.getPatchLevel(),
                system.getLastPatch() == null ? null : Timestamp.valueOf(system.getLastPatch()),
                system.getPatchedBy());
    }

    // -- components -----------------------------------------------------------

    public List<SoftwareComponent> findComponents() {
        return jdbc.query("SELECT COMPONENT_ID, DESCRIPTION, PRODUCT, REPLACED_BY "
                + "FROM SP_COMPONENT ORDER BY COMPONENT_ID", COMPONENT_MAPPER);
    }

    public SoftwareComponent findComponent(String componentId) {
        List<SoftwareComponent> rows = jdbc.query("SELECT COMPONENT_ID, DESCRIPTION, PRODUCT, "
                + "REPLACED_BY FROM SP_COMPONENT WHERE COMPONENT_ID = ?",
                COMPONENT_MAPPER, componentId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertComponent(SoftwareComponent component) {
        jdbc.update("INSERT INTO SP_COMPONENT (COMPONENT_ID, DESCRIPTION, PRODUCT, REPLACED_BY) "
                        + "VALUES (?,?,?,?)",
                component.getComponentId(), component.getDescription(), component.getProduct(),
                component.getReplacedBy());
    }

    // -- component status -----------------------------------------------------

    public List<ComponentStatus> findStatus(String systemId) {
        return jdbc.query("SELECT SYSTEM_ID, COMPONENT_ID, PATCH_LEVEL, STATUS, STATUS_TEXT, "
                        + "LAST_CHANGED FROM SP_STATUS WHERE SYSTEM_ID = ? "
                        + "ORDER BY COMPONENT_ID",
                STATUS_MAPPER, systemId);
    }

    public List<ComponentStatus> findAllStatus() {
        return jdbc.query("SELECT SYSTEM_ID, COMPONENT_ID, PATCH_LEVEL, STATUS, STATUS_TEXT, "
                + "LAST_CHANGED FROM SP_STATUS ORDER BY SYSTEM_ID, COMPONENT_ID",
                STATUS_MAPPER);
    }

    public void insertStatus(ComponentStatus status) {
        jdbc.update("INSERT INTO SP_STATUS (SYSTEM_ID, COMPONENT_ID, PATCH_LEVEL, STATUS, "
                        + "STATUS_TEXT, LAST_CHANGED) VALUES (?,?,?,?,?,?)",
                status.getSystemId(), status.getComponentId(), status.getPatchLevel(),
                status.getStatus(), status.getStatusText(),
                Timestamp.valueOf(status.getLastChanged()));
    }

    // -- support packages -----------------------------------------------------

    public List<SupportPackage> findPackages() {
        return jdbc.query("SELECT SP_ID, PATCH_LEVEL, RELEASED_ON, DESCRIPTION FROM SP_PACKAGE "
                + "ORDER BY PATCH_LEVEL", PACKAGE_MAPPER);
    }

    public SupportPackage findPackage(String spId) {
        List<SupportPackage> rows = jdbc.query("SELECT SP_ID, PATCH_LEVEL, RELEASED_ON, "
                        + "DESCRIPTION FROM SP_PACKAGE WHERE SP_ID = ?", PACKAGE_MAPPER, spId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertPackage(SupportPackage pack) {
        jdbc.update("INSERT INTO SP_PACKAGE (SP_ID, PATCH_LEVEL, RELEASED_ON, DESCRIPTION) "
                        + "VALUES (?,?,?,?)",
                pack.getSpId(), pack.getPatchLevel(), Date.valueOf(pack.getReleasedOn()),
                pack.getDescription());
    }

    // -- changelog ------------------------------------------------------------

    public List<ChangeEntry> findChangelog(String spId) {
        return jdbc.query("SELECT ENTRY_ID, SP_ID, COMPONENT_ID, ENTRY_TEXT, CATEGORY "
                + "FROM SP_CHANGELOG WHERE SP_ID = ? ORDER BY CATEGORY, COMPONENT_ID",
                CHANGELOG_MAPPER, spId);
    }

    public List<ChangeEntry> findAllChangelog() {
        return jdbc.query("SELECT ENTRY_ID, SP_ID, COMPONENT_ID, ENTRY_TEXT, CATEGORY "
                + "FROM SP_CHANGELOG ORDER BY SP_ID, CATEGORY", CHANGELOG_MAPPER);
    }

    public void insertChange(ChangeEntry entry) {
        jdbc.update("INSERT INTO SP_CHANGELOG (ENTRY_ID, SP_ID, COMPONENT_ID, ENTRY_TEXT, "
                        + "CATEGORY) VALUES (?,?,?,?,?)",
                entry.getEntryId(), entry.getSpId(), entry.getComponentId(), entry.getEntryText(),
                entry.getCategory());
    }

    // -- dependencies ---------------------------------------------------------

    public List<ComponentDependency> findDependencies() {
        return jdbc.query("SELECT DEP_ID, COMPONENT_ID, REQUIRES_COMPONENT FROM SP_DEPENDENCY "
                + "ORDER BY COMPONENT_ID", DEPENDENCY_MAPPER);
    }

    public void insertDependency(ComponentDependency dependency) {
        jdbc.update("INSERT INTO SP_DEPENDENCY (DEP_ID, COMPONENT_ID, REQUIRES_COMPONENT) "
                        + "VALUES (?,?,?)",
                dependency.getDependencyId(), dependency.getComponentId(),
                dependency.getRequiresComponent());
    }

    // -- the patch log --------------------------------------------------------

    public List<PatchError> findErrors(String systemId) {
        return jdbc.query("SELECT ERROR_ID, SYSTEM_ID, COMPONENT_ID, SP_ID, LOG_TEXT, AT_TS "
                        + "FROM SP_ERROR WHERE SYSTEM_ID = ? ORDER BY AT_TS DESC",
                ERROR_MAPPER, systemId);
    }

    public List<PatchError> findAllErrors() {
        return jdbc.query("SELECT ERROR_ID, SYSTEM_ID, COMPONENT_ID, SP_ID, LOG_TEXT, AT_TS "
                + "FROM SP_ERROR ORDER BY AT_TS DESC", ERROR_MAPPER);
    }

    public void insertError(PatchError error) {
        jdbc.update("INSERT INTO SP_ERROR (ERROR_ID, SYSTEM_ID, COMPONENT_ID, SP_ID, LOG_TEXT, "
                        + "AT_TS) VALUES (?,?,?,?,?,?)",
                error.getErrorId(), error.getSystemId(), error.getComponentId(),
                error.getSpId(), error.getLogText(), Timestamp.valueOf(error.getAt()));
    }
}
