-- ---------------------------------------------------------------------------
-- SAP Interface Cockpit - database schema
--
-- A simplified model of what an integration operations team reads every day:
--
--   IFDEF   the interface catalogue - which interfaces exist, who owns them,
--           what they connect to and what their SLA is
--   IFLOG   the message log - one row per RFC / IDoc / BAPI call
--   IFERR   the error catalogue - error code to cause and remediation
--   IFALERT raised alerts
--   IFQUEUE the replay queue for failed messages
--
-- NOTE ON FIDELITY: the field sets are trimmed to what the tool needs. In a
-- live landscape the message log is usually written by the integration
-- middleware or read from the RFC statistics buffer (TRFC) and the IDoc
-- log (EDIDC). Verify the real structures in SE11 before pointing this at
-- a production system.
--
-- All statements are portable ANSI SQL: valid on H2 (local) and SAP HANA.
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS IFQUEUE;
DROP TABLE IF EXISTS IFALERT;
DROP TABLE IF EXISTS IFLOG;
DROP TABLE IF EXISTS IFERR;
DROP TABLE IF EXISTS IFDEF;
DROP TABLE IF EXISTS PORTAL_AUDIT;
DROP TABLE IF EXISTS PORTAL_USER;
DROP TABLE IF EXISTS SAPUSER;

CREATE TABLE SAPUSER (
    USERID   VARCHAR(20) NOT NULL,
    FULLNAME VARCHAR(60) NOT NULL,
    UFLAG    VARCHAR(1)  NOT NULL,
    DEPT     VARCHAR(40) NOT NULL,
    CONSTRAINT PK_COCKPIT_SAPUSER PRIMARY KEY (USERID)
);

-- The interface catalogue.
CREATE TABLE IFDEF (
    IFACE       VARCHAR(40) NOT NULL,
    DESCRIPTION VARCHAR(120) NOT NULL,
    IF_TYPE     VARCHAR(6)  NOT NULL,   -- RFC / IDOC / BAPI
    DIRECTION   VARCHAR(3)  NOT NULL,   -- IN / OUT
    OWNER       VARCHAR(20) NOT NULL,
    SYSTEM      VARCHAR(10) NOT NULL,
    TARGET      VARCHAR(20) NOT NULL,
    SLA_PCT     INTEGER     NOT NULL,   -- target success rate, in percent
    CRITICAL    VARCHAR(1)  NOT NULL,   -- Y / N
    CONSTRAINT PK_COCKPIT_IFDEF PRIMARY KEY (IFACE)
);

-- The error catalogue: what an error code means and what to do about it.
CREATE TABLE IFERR (
    ERR_CODE    VARCHAR(20) NOT NULL,
    SEVERITY    VARCHAR(10) NOT NULL,   -- CRITICAL / HIGH / MEDIUM / LOW
    CAUSE       VARCHAR(200) NOT NULL,
    REMEDIATION VARCHAR(300) NOT NULL,
    COMMON      VARCHAR(1)  NOT NULL,   -- Y = a known, recurring cause
    CONSTRAINT PK_COCKPIT_IFERR PRIMARY KEY (ERR_CODE)
);

-- One row per interface call.
CREATE TABLE IFLOG (
    MSG_ID     VARCHAR(20) NOT NULL,
    IFACE      VARCHAR(40) NOT NULL,
    MSG_TYPE   VARCHAR(6)  NOT NULL,   -- RFC / IDOC / BAPI
    DIRECTION  VARCHAR(3)  NOT NULL,   -- IN / OUT
    STATUS     VARCHAR(8)  NOT NULL,   -- OK / ERROR / WARNING
    ERR_CODE   VARCHAR(20) NULL,
    ERR_TEXT   VARCHAR(200) NULL,
    DURATION_MS INTEGER    NOT NULL,
    HOST       VARCHAR(40) NOT NULL,
    CORR_ID    VARCHAR(24) NOT NULL,
    CREATED_AT TIMESTAMP  NOT NULL,
    CONSTRAINT PK_COCKPIT_IFLOG PRIMARY KEY (MSG_ID),
    CONSTRAINT FK_COCKPIT_IFLOG_FACE FOREIGN KEY (IFACE) REFERENCES IFDEF (IFACE)
);

CREATE TABLE IFALERT (
    ALERT_ID  VARCHAR(20) NOT NULL,
    IFACE     VARCHAR(40) NOT NULL,
    RULE      VARCHAR(30) NOT NULL,
    SEVERITY  VARCHAR(10) NOT NULL,
    MESSAGE   VARCHAR(300) NOT NULL,
    RAISED_AT TIMESTAMP  NOT NULL,
    ACK_FLAG  VARCHAR(1)  NOT NULL,
    ACK_BY    VARCHAR(40) NULL,
    ACK_AT    TIMESTAMP  NULL,
    CONSTRAINT PK_COCKPIT_IFALERT PRIMARY KEY (ALERT_ID),
    CONSTRAINT UQ_COCKPIT_IFALERT UNIQUE (IFACE, RULE)
);

CREATE TABLE IFQUEUE (
    QUEUE_ID  VARCHAR(20) NOT NULL,
    MSG_ID    VARCHAR(20) NOT NULL,
    IFACE     VARCHAR(40) NOT NULL,
    STATE     VARCHAR(12) NOT NULL,   -- QUEUED / PLAYED / FAILED
    QUEUED_AT TIMESTAMP  NOT NULL,
    PLAYED_AT TIMESTAMP  NULL,
    PLAYED_BY VARCHAR(40) NULL,
    RESULT    VARCHAR(200) NULL,
    CONSTRAINT PK_COCKPIT_IFQUEUE PRIMARY KEY (QUEUE_ID),
    CONSTRAINT UQ_COCKPIT_IFQUEUE UNIQUE (MSG_ID)
);

-- Portal accounts and the audit trail.
CREATE TABLE PORTAL_USER (
    USERNAME      VARCHAR(40) NOT NULL,
    PASSWORD_HASH VARCHAR(120) NOT NULL,
    DISPLAY_NAME  VARCHAR(60) NOT NULL,
    PORTAL_ROLE   VARCHAR(20) NOT NULL,
    ENABLED       VARCHAR(1)  NOT NULL,
    CONSTRAINT PK_COCKPIT_PORTAL_USER PRIMARY KEY (USERNAME)
);

CREATE TABLE PORTAL_AUDIT (
    AUD_ID     VARCHAR(20) NOT NULL,
    USERNAME   VARCHAR(40) NOT NULL,
    ACTION     VARCHAR(40) NOT NULL,
    DETAIL_TXT VARCHAR(400) NOT NULL,
    AT_TS      TIMESTAMP   NOT NULL,
    CONSTRAINT PK_COCKPIT_PORTAL_AUDIT PRIMARY KEY (AUD_ID)
);
