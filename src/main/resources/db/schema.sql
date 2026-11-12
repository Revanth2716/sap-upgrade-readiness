-- ---------------------------------------------------------------------------
-- SAP Upgrade & Support Package Readiness - database schema
--
-- The upgrade picture a BASIS team is accountable for:
--
--   SP_SYSTEM      the landscape - which system is development, which is
--                  quality, which is production, and what patch level it is on
--   SP_COMPONENT   the software components - SAP_BASIS, SAP_ABA, SAP_HANA ...
--   SP_STATUS      the state of one component in one system: its patch level
--                  and whether it is patched, new, obsolete or released
--   SP_PACKAGE     the support packages and the patch levels they carry
--   SP_CHANGELOG   what changed in a support package, per component
--   SP_DEPENDENCY  which components have to move together
--   SP_ERROR       the failed patch attempts, from the SPAM patch log
--
-- In a live landscape the patch levels come from the SPAM tables (SPDIR,
-- SPOBJ, SPHIST) and the component states from the Software Update
-- Manager's component list, which SUM keeps on the file system rather than
-- in the database - so the component state is maintained here.
--
-- All statements are portable ANSI SQL: valid on H2 (local) and SAP HANA.
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS SP_ERROR;
DROP TABLE IF EXISTS SP_CHANGELOG;
DROP TABLE IF EXISTS SP_DEPENDENCY;
DROP TABLE IF EXISTS SP_STATUS;
DROP TABLE IF EXISTS SP_PACKAGE;
DROP TABLE IF EXISTS SP_COMPONENT;
DROP TABLE IF EXISTS SP_SYSTEM;
DROP TABLE IF EXISTS PORTAL_AUDIT;
DROP TABLE IF EXISTS PORTAL_USER;
DROP TABLE IF EXISTS SAPUSER;

CREATE TABLE SAPUSER (
    USERID   VARCHAR(20) NOT NULL,
    FULLNAME VARCHAR(60) NOT NULL,
    UFLAG    VARCHAR(1)  NOT NULL,
    DEPT     VARCHAR(40) NOT NULL,
    CONSTRAINT PK_UPG_SAPUSER PRIMARY KEY (USERID)
);

-- The landscape.
CREATE TABLE SP_SYSTEM (
    SYSTEM_ID   VARCHAR(10) NOT NULL,
    DESCRIPTION VARCHAR(60) NOT NULL,
    SYS_ROLE    VARCHAR(20) NOT NULL,   -- DEVELOPMENT / QUALITY / PRODUCTION
    IS_TARGET   VARCHAR(1)  NOT NULL,   -- Y / N
    PATCH_LEVEL INTEGER     NOT NULL,   -- the patch level the system is on
    LAST_PATCH  TIMESTAMP   NULL,       -- when it was last patched
    PATCHED_BY  VARCHAR(20) NOT NULL,
    CONSTRAINT PK_UPG_SP_SYSTEM PRIMARY KEY (SYSTEM_ID)
);

-- The software components.
CREATE TABLE SP_COMPONENT (
    COMPONENT_ID   VARCHAR(12) NOT NULL,   -- SAP_BASIS, SAP_ABA, SAP_HANA ...
    DESCRIPTION    VARCHAR(80) NOT NULL,
    PRODUCT        VARCHAR(40) NOT NULL,
    REPLACED_BY    VARCHAR(12) NOT NULL,   -- the component that replaces it
    CONSTRAINT PK_UPG_SP_COMPONENT PRIMARY KEY (COMPONENT_ID)
);

-- The state of one component in one system.
CREATE TABLE SP_STATUS (
    SYSTEM_ID    VARCHAR(10) NOT NULL,
    COMPONENT_ID VARCHAR(12) NOT NULL,
    PATCH_LEVEL  INTEGER     NOT NULL,   -- the level this component is on
    STATUS       VARCHAR(12) NOT NULL,   -- PATCHED / NEW / OBSOLETE / RELEASED
    STATUS_TEXT  VARCHAR(60) NOT NULL,
    LAST_CHANGED TIMESTAMP   NOT NULL,
    CONSTRAINT PK_UPG_SP_STATUS PRIMARY KEY (SYSTEM_ID, COMPONENT_ID),
    CONSTRAINT FK_UPG_SP_STATUS FOREIGN KEY (SYSTEM_ID) REFERENCES SP_SYSTEM (SYSTEM_ID),
    CONSTRAINT FK_UPG_SP_STATUS_COMP FOREIGN KEY (COMPONENT_ID) REFERENCES SP_COMPONENT (COMPONENT_ID)
);

-- The support packages.
CREATE TABLE SP_PACKAGE (
    SP_ID       VARCHAR(12) NOT NULL,   -- SP05, SP07 ...
    PATCH_LEVEL INTEGER     NOT NULL,
    RELEASED_ON DATE        NOT NULL,
    DESCRIPTION VARCHAR(80) NOT NULL,
    CONSTRAINT PK_UPG_SP_PACKAGE PRIMARY KEY (SP_ID)
);

-- What changed, per component, per support package.
CREATE TABLE SP_CHANGELOG (
    ENTRY_ID     VARCHAR(20) NOT NULL,
    SP_ID        VARCHAR(12) NOT NULL,
    COMPONENT_ID VARCHAR(12) NOT NULL,
    ENTRY_TEXT   VARCHAR(300) NOT NULL,
    CATEGORY     VARCHAR(20) NOT NULL,   -- FEATURE / FIX / SECURITY / INCOMPATIBLE
    CONSTRAINT PK_UPG_SP_CHANGELOG PRIMARY KEY (ENTRY_ID),
    CONSTRAINT FK_UPG_SP_CHANGELOG FOREIGN KEY (SP_ID) REFERENCES SP_PACKAGE (SP_ID)
);

-- Which components have to move together.
CREATE TABLE SP_DEPENDENCY (
    DEP_ID             VARCHAR(20) NOT NULL,
    COMPONENT_ID       VARCHAR(12) NOT NULL,
    REQUIRES_COMPONENT VARCHAR(12) NOT NULL,
    CONSTRAINT PK_UPG_SP_DEPENDENCY PRIMARY KEY (DEP_ID)
);

-- The failed patch attempts, from the SPAM patch log.
CREATE TABLE SP_ERROR (
    ERROR_ID     VARCHAR(20) NOT NULL,
    SYSTEM_ID    VARCHAR(10) NOT NULL,
    COMPONENT_ID VARCHAR(12) NOT NULL,
    SP_ID        VARCHAR(12) NOT NULL,
    LOG_TEXT     VARCHAR(300) NOT NULL,
    AT_TS        TIMESTAMP   NOT NULL,
    CONSTRAINT PK_UPG_SP_ERROR PRIMARY KEY (ERROR_ID)
);

-- Portal accounts and the audit trail.
CREATE TABLE PORTAL_USER (
    USERNAME      VARCHAR(40) NOT NULL,
    PASSWORD_HASH VARCHAR(120) NOT NULL,
    DISPLAY_NAME  VARCHAR(60) NOT NULL,
    PORTAL_ROLE   VARCHAR(20) NOT NULL,
    ENABLED       VARCHAR(1)  NOT NULL,
    CONSTRAINT PK_UPG_PORTAL_USER PRIMARY KEY (USERNAME)
);

CREATE TABLE PORTAL_AUDIT (
    AUD_ID     VARCHAR(20) NOT NULL,
    USERNAME   VARCHAR(40) NOT NULL,
    ACTION     VARCHAR(40) NOT NULL,
    DETAIL_TXT VARCHAR(400) NOT NULL,
    AT_TS      TIMESTAMP   NOT NULL,
    CONSTRAINT PK_UPG_PORTAL_AUDIT PRIMARY KEY (AUD_ID)
);
