-- DROP SEQUENCE genericobjects_id_SEQ;


PROMPT Creating Sequence genericobjects_id_SEQ ...
CREATE SEQUENCE  genericobjects_id_SEQ
  MINVALUE 1 MAXVALUE 999999999999999999999999 INCREMENT BY 1  NOCYCLE ;

-- DROP SEQUENCE configobjects_id_SEQ;


PROMPT Creating Sequence configobjects_id_SEQ ...
CREATE SEQUENCE  configobjects_id_SEQ
  MINVALUE 1 MAXVALUE 999999999999999999999999 INCREMENT BY 1  NOCYCLE ;

-- DROP SEQUENCE relationships_id_SEQ;


PROMPT Creating Sequence relationships_id_SEQ ...
CREATE SEQUENCE  relationships_id_SEQ
  MINVALUE 1 MAXVALUE 999999999999999999999999 INCREMENT BY 1  NOCYCLE ;

-- DROP SEQUENCE managedobjects_id_SEQ;


PROMPT Creating Sequence managedobjects_id_SEQ ...
CREATE SEQUENCE  managedobjects_id_SEQ
  MINVALUE 1 MAXVALUE 999999999999999999999999 INCREMENT BY 1  NOCYCLE ;

-- DROP SEQUENCE schedulerobjects_id_SEQ;


PROMPT Creating Sequence schedulerobjects_id_SEQ ...
CREATE SEQUENCE  schedulerobjects_id_SEQ
  MINVALUE 1 MAXVALUE 999999999999999999999999 INCREMENT BY 1  NOCYCLE ;

-- DROP SEQUENCE clusterobjects_id_SEQ;


PROMPT Creating Sequence clusterobjects_id_SEQ ...
CREATE SEQUENCE  clusterobjects_id_SEQ
  MINVALUE 1 MAXVALUE 999999999999999999999999 INCREMENT BY 1  NOCYCLE ;


-- DROP SEQUENCE objecttypes_id_SEQ;


PROMPT Creating Sequence objecttypes_id_SEQ ...
CREATE SEQUENCE  objecttypes_id_SEQ
  MINVALUE 1 MAXVALUE 999999999999999999999999 INCREMENT BY 1  NOCYCLE ;

-- DROP SEQUENCE updateobjects_id_SEQ;


PROMPT Creating Sequence updateobjects_id_SEQ ...
CREATE SEQUENCE  updateobjects_id_SEQ
  MINVALUE 1 MAXVALUE 999999999999999999999999 INCREMENT BY 1  NOCYCLE ;


-- DROP TABLE uinotification CASCADE CONSTRAINTS;


PROMPT Creating Table uinotification ...
CREATE TABLE uinotification (
  objectid VARCHAR2(38 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  notificationType VARCHAR2(255 CHAR) NOT NULL,
  createDate VARCHAR2(255 CHAR) NOT NULL,
  message VARCHAR2(4000 CHAR) NOT NULL,
  requester VARCHAR2(255 CHAR),
  receiverId VARCHAR2(38 CHAR) NOT NULL,
  requesterId VARCHAR2(255 CHAR),
  notificationSubtype VARCHAR2(255 CHAR)
);

PROMPT Creating Primary Key Constraint PRIMARY_0 on table uinotification ...
ALTER TABLE uinotification
ADD CONSTRAINT PRIMARY_0 PRIMARY KEY
(
  objectid
)
ENABLE
;

-- DROP TABLE auditaccess CASCADE CONSTRAINTS;

-- -----------------------------------------------------
-- Table openidm.auditaccess
-- -----------------------------------------------------
PROMPT Creating Table auditaccess ...
CREATE TABLE auditaccess (
  objectid VARCHAR2(56 CHAR) NOT NULL,
  activitydate VARCHAR2(29 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  server_ip VARCHAR2(40 CHAR),
  server_port VARCHAR2(5 CHAR),
  client_ip VARCHAR2(40 CHAR),
  client_port VARCHAR2(5 CHAR),
  request_protocol VARCHAR2(255 CHAR) NULL ,
  request_operation VARCHAR2(255 CHAR) NULL ,
  request_detail CLOB NULL ,
  http_request_secure VARCHAR2(255 CHAR) NULL ,
  http_request_method VARCHAR2(255 CHAR) NULL ,
  http_request_path VARCHAR2(255 CHAR) NULL ,
  http_request_queryparameters CLOB NULL ,
  http_request_headers CLOB NULL ,
  http_request_cookies CLOB NULL ,
  http_response_headers CLOB NULL ,
  response_status VARCHAR2(255 CHAR) NULL ,
  response_statuscode VARCHAR2(255 CHAR) NULL ,
  response_elapsedtime VARCHAR2(255 CHAR) NULL ,
  response_elapsedtimeunits VARCHAR2(255 CHAR) NULL ,
  roles CLOB NULL
);


COMMENT ON COLUMN auditaccess.activitydate IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint PRIMARY on table auditaccess ...
ALTER TABLE auditaccess
ADD CONSTRAINT PRIMARY_OBJECTID PRIMARY KEY
(
  objectid
)
ENABLE
;

-- DROP TABLE auditauthentication CASCADE CONSTRAINTS;

-- -----------------------------------------------------
-- Table openidm.auditauthentication
-- -----------------------------------------------------
PROMPT Creating TABLE auditauthentication ...
CREATE TABLE auditauthentication (
  objectid VARCHAR2(56 CHAR) NOT NULL,
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  activitydate VARCHAR2(29 CHAR) NOT NULL,
  userid VARCHAR2(255 CHAR),
  eventname VARCHAR2(50 CHAR),
  result VARCHAR2(255 CHAR),
  principals CLOB,
  context CLOB,
  entries CLOB,
  trackingids CLOB
);

COMMENT ON COLUMN auditauthentication.activitydate IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating PRIMARY KEY CONSTRAINT PRIMARY ON TABLE auditauthentication ...
ALTER TABLE auditauthentication
ADD CONSTRAINT pk_auditauthentication PRIMARY KEY
(
  objectid
)
ENABLE
;


-- DROP TABLE auditconfig CASCADE CONSTRAINTS;

-- -----------------------------------------------------
-- Table openidm.auditconfig
-- -----------------------------------------------------
PROMPT Creating Table auditconfig ...
CREATE TABLE auditconfig (
  objectid VARCHAR2(56 CHAR) NOT NULL,
  activitydate VARCHAR2(29 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  runas VARCHAR2(255 CHAR),
  configobjectid VARCHAR2(255 CHAR) NULL ,
  operation VARCHAR2(255 CHAR) NULL ,
  beforeObject CLOB,
  afterObject CLOB,
  changedfields CLOB,
  rev VARCHAR2(255 CHAR)
);


COMMENT ON COLUMN auditconfig.activitydate IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint pk_auditconfig on table auditconfig ...
ALTER TABLE auditconfig
ADD CONSTRAINT pk_auditconfig PRIMARY KEY
(
  objectid
)
ENABLE
;
PROMPT Creating Index idx_auditconfig_transactionid on auditconfig ...
CREATE INDEX idx_auditconfig_transactionid ON auditconfig
(
  transactionid
)
;



-- DROP TABLE auditactivity CASCADE CONSTRAINTS;

-- -----------------------------------------------------
-- Table openidm.auditactivity
-- -----------------------------------------------------
PROMPT Creating Table auditactivity ...
CREATE TABLE auditactivity (
  objectid VARCHAR2(56 CHAR) NOT NULL,
  activitydate VARCHAR2(29 CHAR) NOT NULL,
  eventname VARCHAR2(255 CHAR),
  transactionid VARCHAR2(255 CHAR) NOT NULL,
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  runas VARCHAR2(255 CHAR),
  activityobjectid VARCHAR2(255 CHAR) NULL ,
  operation VARCHAR2(255 CHAR) NULL ,
  subjectbefore CLOB,
  subjectafter CLOB,
  changedfields CLOB,
  subjectrev VARCHAR2(255 CHAR),
  passwordchanged VARCHAR2(5 CHAR),
  message CLOB,
  status VARCHAR2(20 CHAR)
);


COMMENT ON COLUMN auditactivity.activitydate IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint pk_auditactivity on table auditactivity ...
ALTER TABLE auditactivity
ADD CONSTRAINT pk_auditactivity PRIMARY KEY
(
  objectid
)
ENABLE
;
PROMPT Creating Index idx_auditactivity_transid on auditactivity ...
CREATE INDEX idx_auditactivity_transid ON auditactivity
(
  transactionid
)
;

-- DROP TABLE auditrecon CASCADE CONSTRAINTS;

-- -----------------------------------------------------
-- Table openidm.auditrecon
-- -----------------------------------------------------
PROMPT Creating Table auditrecon ...
CREATE TABLE auditrecon (
  objectid VARCHAR2(56) NOT NULL,
  transactionid VARCHAR2(255) NOT NULL,
  activitydate VARCHAR2(29 CHAR) NOT NULL,
  eventname VARCHAR2(50 CHAR),
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  activity VARCHAR2(24 CHAR),
  exceptiondetail CLOB,
  linkqualifier VARCHAR2(255 CHAR),
  mapping VARCHAR2(511 CHAR),
  message CLOB,
  messagedetail CLOB,
  situation VARCHAR2(24 CHAR),
  sourceobjectid VARCHAR2(511 CHAR),
  status VARCHAR2(20 CHAR),
  targetobjectid VARCHAR2(511 CHAR),
  reconciling VARCHAR2(12 CHAR),
  ambiguoustargetobjectids CLOB,
  reconaction VARCHAR2(36 CHAR),
  entrytype VARCHAR2(7 CHAR),
  reconid VARCHAR2(56 CHAR)
);


COMMENT ON COLUMN auditrecon.activitydate IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint PRIMARY_1 on table auditrecon ...
ALTER TABLE auditrecon
ADD CONSTRAINT PRIMARY_1 PRIMARY KEY
(
  objectid
)
ENABLE
;

-- DROP TABLE auditsync CASCADE CONSTRAINTS;

-- -----------------------------------------------------
-- Table openidm.auditsync
-- -----------------------------------------------------
PROMPT Creating Table auditsync ...
CREATE TABLE auditsync (
  objectid VARCHAR2(56) NOT NULL,
  transactionid VARCHAR2(255) NOT NULL,
  activitydate VARCHAR2(29 CHAR) NOT NULL,
  eventname VARCHAR2(50 CHAR),
  userid VARCHAR2(255 CHAR),
  trackingids CLOB,
  activity VARCHAR2(24 CHAR),
  exceptiondetail CLOB,
  linkqualifier VARCHAR2(255 CHAR),
  mapping VARCHAR2(511 CHAR),
  message CLOB,
  messagedetail CLOB,
  situation VARCHAR2(24 CHAR),
  sourceobjectid VARCHAR2(511 CHAR),
  status VARCHAR2(20 CHAR),
  targetobjectid VARCHAR2(511 CHAR)
);


COMMENT ON COLUMN auditsync.activitydate IS 'Date format: 2011-09-09T14:58:17.654+02:00'
;

PROMPT Creating Primary Key Constraint PRIMARY_13 on table auditsync ...
ALTER TABLE auditsync
ADD CONSTRAINT PRIMARY_13 PRIMARY KEY
(
  objectid
)
ENABLE
;

-- DROP TABLE configobjectproperties CASCADE CONSTRAINTS;


PROMPT Creating Table configobjectproperties ...
CREATE TABLE configobjectproperties (
  configobjects_id NUMBER(24,0) NOT NULL,
  propkey VARCHAR2(255 CHAR) NOT NULL,
  proptype VARCHAR2(255 CHAR),
  propvalue VARCHAR2(2000 CHAR)
);


PROMPT Creating Index fk_configobjectproperties_conf on configobjectproperties ...
CREATE INDEX fk_configobjectproperties_conf ON configobjectproperties
(
  configobjects_id
)
;
PROMPT Creating Index idx_configobjectpropert_1 on configobjectproperties ...
CREATE INDEX idx_configobjectpropert_1 ON configobjectproperties
(
  propkey
)
;
PROMPT Creating Index idx_configobjectpropert_2 on configobjectproperties ...
CREATE INDEX idx_configobjectpropert_2 ON configobjectproperties
(
  propvalue
)
;

-- DROP TABLE configobjects CASCADE CONSTRAINTS;


PROMPT Creating Table configobjects ...
CREATE TABLE configobjects (
  id NUMBER(24,0) NOT NULL,
  objecttypes_id NUMBER(24,0) NOT NULL,
  objectid VARCHAR2(255 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  fullobject CLOB
);


PROMPT Creating Primary Key Constraint PRIMARY_3 on table configobjects ...
ALTER TABLE configobjects
ADD CONSTRAINT PRIMARY_3 PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Unique Index idx_configobjects_object on configobjects...
CREATE UNIQUE INDEX idx_configobjects_object ON configobjects
(
  objecttypes_id,
  objectid
)
;
PROMPT Creating Index fk_configobjects_objecttypes on configobjects ...
CREATE INDEX fk_configobjects_objecttypes ON configobjects
(
  objecttypes_id
)
;

-- DROP TABLE relationshipproperties CASCADE CONSTRAINTS;


PROMPT Creating Table relationshipproperties ...
CREATE TABLE relationshipproperties (
  relationships_id NUMBER(24,0) NOT NULL,
  propkey VARCHAR2(255 CHAR) NOT NULL,
  proptype VARCHAR2(255 CHAR),
  propvalue VARCHAR2(2000 CHAR)
);


PROMPT Creating Index fk_relationshipproperties_conf on relationshipproperties ...
CREATE INDEX fk_relationshipproperties_conf ON relationshipproperties
(
  relationships_id
)
;
PROMPT Creating Index idx_relationshippropert_1 on relationshipproperties ...
CREATE INDEX idx_relationshippropert_1 ON relationshipproperties
(
  propkey
)
;
PROMPT Creating Index idx_relationshippropert_2 on relationshipproperties ...
CREATE INDEX idx_relationshippropert_2 ON relationshipproperties
(
  propvalue
)
;

-- DROP TABLE relationships CASCADE CONSTRAINTS;


PROMPT Creating Table relationships ...
CREATE TABLE relationships (
  id NUMBER(24,0) NOT NULL,
  objecttypes_id NUMBER(24,0) NOT NULL,
  objectid VARCHAR2(255 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  fullobject CLOB
);


PROMPT Creating Primary Key Constraint pk_relationships on table relationships ...
ALTER TABLE relationships
ADD CONSTRAINT pk_relationships PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Unique Index idx_relationships_object on relationships...
CREATE UNIQUE INDEX idx_relationships_object ON relationships
(
  objecttypes_id,
  objectid
)
;
PROMPT Creating Index fk_relationships_objecttypes on relationships ...
CREATE INDEX fk_relationships_objecttypes ON relationships
(
  objecttypes_id
)
;


-- DROP TABLE genericobjectproperties CASCADE CONSTRAINTS;


PROMPT Creating Table genericobjectproperties ...
CREATE TABLE genericobjectproperties (
  genericobjects_id NUMBER(24,0) NOT NULL,
  propkey VARCHAR2(255 CHAR) NOT NULL,
  proptype VARCHAR2(32 CHAR),
  propvalue VARCHAR2(2000 CHAR)
);


PROMPT Creating Index fk_genericobjectproperties_gen on genericobjectproperties ...
CREATE INDEX fk_genericobjectproperties_gen ON genericobjectproperties
(
  genericobjects_id
)
;
PROMPT Creating Index idx_genericobjectproper_1 on genericobjectproperties ...
CREATE INDEX idx_genericobjectproper_1 ON genericobjectproperties
(
  propkey
)
;
PROMPT Creating Index idx_genericobjectproper_2 on genericobjectproperties ...
CREATE INDEX idx_genericobjectproper_2 ON genericobjectproperties
(
  propvalue
)
;

-- DROP TABLE genericobjects CASCADE CONSTRAINTS;


PROMPT Creating Table genericobjects ...
CREATE TABLE genericobjects (
  id NUMBER(24,0) NOT NULL,
  objecttypes_id NUMBER(24,0) NOT NULL,
  objectid VARCHAR2(255 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  fullobject CLOB
);


PROMPT Creating Primary Key Constraint PRIMARY_5 on table genericobjects ...
ALTER TABLE genericobjects
ADD CONSTRAINT PRIMARY_5 PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Unique Index idx_genericobjects_object on genericobjects...
CREATE UNIQUE INDEX idx_genericobjects_object ON genericobjects
(
  objecttypes_id,
  objectid
)
;
PROMPT Creating Index fk_genericobjects_objecttypes on genericobjects ...
CREATE INDEX fk_genericobjects_objecttypes ON genericobjects
(
  objecttypes_id
)
;


-- DROP TABLE internaluser CASCADE CONSTRAINTS;


PROMPT Creating Table internaluser ...
CREATE TABLE internaluser (
  objectid VARCHAR2(255 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  pwd VARCHAR2(510 CHAR),
  roles VARCHAR2(1024 CHAR)
);


-- DROP TABLE internalrole CASCADE CONSTRAINTS;

PROMPT Creating Table internalrole ...
CREATE TABLE internalrole (
  objectid VARCHAR2(255 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  description VARCHAR2(510 CHAR)
);


PROMPT Creating Primary Key Constraint PRIMARY_2 on table internaluser ...
ALTER TABLE internaluser
ADD CONSTRAINT PRIMARY_2 PRIMARY KEY
(
  objectid
)
ENABLE
;

-- DROP TABLE links CASCADE CONSTRAINTS;


PROMPT Creating Table links ...
CREATE TABLE links (
  objectid VARCHAR2(38 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  linktype VARCHAR2(50 CHAR) NOT NULL,
  linkqualifier VARCHAR2(50 CHAR) NOT NULL,
  firstid VARCHAR2(255 CHAR) NOT NULL,
  secondid VARCHAR2(255 CHAR) NOT NULL
);


PROMPT Creating Primary Key Constraint PRIMARY_4 on table links ...
ALTER TABLE links
ADD CONSTRAINT PRIMARY_4 PRIMARY KEY
(
  objectid
)
ENABLE
;
PROMPT Creating Index idx_links_first on links ...
CREATE UNIQUE INDEX idx_links_first ON links
(
  linktype,
  linkqualifier,
  firstid
)
;
PROMPT Creating Index idx_links_second on links ...
CREATE UNIQUE INDEX idx_links_second ON links
(
  linktype,
  linkqualifier,
  secondid
)
;

-- DROP TABLE security CASCADE CONSTRAINTS;


PROMPT Creating Table security ...
CREATE TABLE security (
  objectid VARCHAR2(38 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  storestring CLOB
);


PROMPT Creating Primary Key Constraint PRIMARY_11 on table security ...
ALTER TABLE security
ADD CONSTRAINT PRIMARY_11 PRIMARY KEY
(
  objectid
)
ENABLE
;

-- DROP TABLE securitykeys CASCADE CONSTRAINTS;


PROMPT Creating Table securitykeys ...
CREATE TABLE securitykeys (
  objectid VARCHAR2(38 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  keypair CLOB
);


PROMPT Creating Primary Key Constraint PRIMARY_12 on table securitykeys ...
ALTER TABLE securitykeys
ADD CONSTRAINT PRIMARY_12 PRIMARY KEY
(
  objectid
)
ENABLE
;

-- DROP TABLE managedobjectproperties CASCADE CONSTRAINTS;


PROMPT Creating Table managedobjectproperties ...
CREATE TABLE managedobjectproperties (
  managedobjects_id NUMBER(24,0) NOT NULL,
  propkey VARCHAR2(255 CHAR) NOT NULL,
  proptype VARCHAR2(32 CHAR),
  propvalue VARCHAR2(2000 CHAR)
);


PROMPT Creating Index fk_managedobjectproperties_man on managedobjectproperties ...
CREATE INDEX fk_managedobjectproperties_man ON managedobjectproperties
(
  managedobjects_id
)
;
PROMPT Creating Index idx_managedobjectproper_1 on managedobjectproperties ...
CREATE INDEX idx_managedobjectproper_1 ON managedobjectproperties
(
  propkey
)
;
PROMPT Creating Index idx_managedobjectproper_2 on managedobjectproperties ...
CREATE INDEX idx_managedobjectproper_2 ON managedobjectproperties
(
  propvalue
)
;

-- DROP TABLE managedobjects CASCADE CONSTRAINTS;


PROMPT Creating Table managedobjects ...
CREATE TABLE managedobjects (
  id NUMBER(24,0) NOT NULL,
  objecttypes_id NUMBER(24,0) NOT NULL,
  objectid VARCHAR2(255 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  fullobject CLOB
);


PROMPT Creating Primary Key Constraint PRIMARY_6 on table managedobjects ...
ALTER TABLE managedobjects
ADD CONSTRAINT PRIMARY_6 PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Unique Index idx_managedobjects_object on managedobjects...
CREATE UNIQUE INDEX idx_managedobjects_object ON managedobjects
(
  objecttypes_id,
  objectid
)
;
PROMPT Creating Index fk_managedobjects_objectypes on managedobjects ...
CREATE INDEX fk_managedobjects_objectypes ON managedobjects
(
  objecttypes_id
)
;

-- DROP TABLE schedobjectproperties CASCADE CONSTRAINTS;


PROMPT Creating Table schedobjectproperties ...
CREATE TABLE schedobjectproperties (
  schedulerobjects_id NUMBER(24,0) NOT NULL,
  propkey VARCHAR2(255 CHAR) NOT NULL,
  proptype VARCHAR2(32 CHAR),
  propvalue VARCHAR2(2000 CHAR)
);


PROMPT Creating Index fk_schedobjectproperties_man on schedobjectproperties ...
CREATE INDEX fk_schedobjectproperties_man ON schedobjectproperties
(
  schedulerobjects_id
)
;
PROMPT Creating Index idx_schedobjectproperties_1 on schedobjectproperties ...
CREATE INDEX idx_schedobjectproperties_1 ON schedobjectproperties
(
  propkey
)
;
PROMPT Creating Index idx_schedobjectproperties_2 on schedobjectproperties ...
CREATE INDEX idx_schedobjectproperties_2 ON schedobjectproperties
(
  propvalue
)
;

-- DROP TABLE schedulerobjects CASCADE CONSTRAINTS;


PROMPT Creating Table schedulerobjects ...
CREATE TABLE schedulerobjects (
  id NUMBER(24,0) NOT NULL,
  objecttypes_id NUMBER(24,0) NOT NULL,
  objectid VARCHAR2(255 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  fullobject CLOB
);


PROMPT Creating Primary Key Constraint PRIMARY_9 on table schedulerobjects ...
ALTER TABLE schedulerobjects
ADD CONSTRAINT PRIMARY_9 PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Unique Index idx_schedulerobjects_object on schedulerobjects...
CREATE UNIQUE INDEX idx_schedulerobjects_object ON schedulerobjects
(
  objecttypes_id,
  objectid
)
;
PROMPT Creating Index fk_schedulerobjects_objectypes on schedulerobjects ...
CREATE INDEX fk_schedulerobjects_objectypes ON schedulerobjects
(
  objecttypes_id
)
;

-- DROP TABLE clusterobjectproperties CASCADE CONSTRAINTS;


PROMPT Creating Table clusterobjectproperties ...
CREATE TABLE clusterobjectproperties (
  clusterobjects_id NUMBER(24,0) NOT NULL,
  propkey VARCHAR2(255 CHAR) NOT NULL,
  proptype VARCHAR2(32 CHAR),
  propvalue VARCHAR2(2000 CHAR)
);


PROMPT Creating Index fk_clusterobjectproperties_man on clusterobjectproperties ...
CREATE INDEX fk_clusterobjectproperties_man ON clusterobjectproperties
(
  clusterobjects_id
)
;
PROMPT Creating Index idx_clusterobjectproperties_1 on clusterobjectproperties ...
CREATE INDEX idx_clusterobjectproperties_1 ON clusterobjectproperties
(
  propkey
)
;
PROMPT Creating Index idx_clusterobjectproperties_2 on clusterobjectproperties ...
CREATE INDEX idx_clusterobjectproperties_2 ON clusterobjectproperties
(
  propvalue
)
;

-- DROP TABLE clusterobjects CASCADE CONSTRAINTS;


PROMPT Creating Table clusterobjects ...
CREATE TABLE clusterobjects (
  id NUMBER(24,0) NOT NULL,
  objecttypes_id NUMBER(24,0) NOT NULL,
  objectid VARCHAR2(255 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  fullobject CLOB
);


PROMPT Creating Primary Key Constraint PRIMARY_10 on table clusterobjects ...
ALTER TABLE clusterobjects
ADD CONSTRAINT PRIMARY_10 PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Unique Index idx_clusterobjects_object on clusterobjects...
CREATE UNIQUE INDEX idx_clusterobjects_object ON clusterobjects
(
  objecttypes_id,
  objectid
)
;
PROMPT Creating Index fk_clusterobjects_objectypes on clusterobjects ...
CREATE INDEX fk_clusterobjects_objectypes ON clusterobjects
(
  objecttypes_id
)
;

-- DROP TABLE updateobjectproperties CASCADE CONSTRAINTS;


PROMPT Creating Table updateobjectproperties ...
CREATE TABLE updateobjectproperties (
  updateobjects_id NUMBER(24,0) NOT NULL,
  propkey VARCHAR2(255 CHAR) NOT NULL,
  proptype VARCHAR2(32 CHAR),
  propvalue VARCHAR2(2000 CHAR)
);


PROMPT Creating Index fk_updateobjectproperties_gen on updateobjectproperties ...
CREATE INDEX fk_updateobjectproperties_gen ON updateobjectproperties
(
  updateobjects_id
)
;
PROMPT Creating Index idx_updateobjectproper_1 on updateobjectproperties ...
CREATE INDEX idx_updateobjectproper_1 ON updateobjectproperties
(
  propkey
)
;
PROMPT Creating Index idx_updateobjectproper_2 on updateobjectproperties ...
CREATE INDEX idx_updateobjectproper_2 ON updateobjectproperties
(
  propvalue
)
;

-- DROP TABLE updateobjects CASCADE CONSTRAINTS;


PROMPT Creating Table updateobjects ...
CREATE TABLE updateobjects (
  id NUMBER(24,0) NOT NULL,
  objecttypes_id NUMBER(24,0) NOT NULL,
  objectid VARCHAR2(255 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  fullobject CLOB
);


PROMPT Creating Primary Key Constraint PRIMARY_14 on table updateobjects ...
ALTER TABLE updateobjects
ADD CONSTRAINT PRIMARY_14 PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Unique Index idx_updateobjects_object on updateobjects...
CREATE UNIQUE INDEX idx_updateobjects_object ON updateobjects
(
  objecttypes_id,
  objectid
)
;
PROMPT Creating Index fk_updateobjects_objecttypes on updateobjects ...
CREATE INDEX fk_updateobjects_objecttypes ON updateobjects
(
  objecttypes_id
)
;

-- DROP TABLE objecttypes CASCADE CONSTRAINTS;


PROMPT Creating Table objecttypes ...
CREATE TABLE objecttypes (
  id NUMBER(24,0) NOT NULL,
  objecttype VARCHAR2(255 CHAR)
);


PROMPT Creating Primary Key Constraint PRIMARY_7 on table objecttypes ...
ALTER TABLE objecttypes
ADD CONSTRAINT PRIMARY_7 PRIMARY KEY
(
  id
)
ENABLE
;
PROMPT Creating Unique Index idx_objecttypes_objecttype on objecttypes...
CREATE UNIQUE INDEX idx_objecttypes_objecttype ON objecttypes
(
  objecttype
)
;

PROMPT Creating Foreign Key Constraint fk_configobjectproperties_conf on table configobjects...
ALTER TABLE configobjectproperties
ADD CONSTRAINT fk_configobjectproperties_conf FOREIGN KEY
(
  configobjects_id
)
REFERENCES configobjects
(
  id
)
ON DELETE CASCADE
ENABLE
;

PROMPT Creating Foreign Key Constraint fk_configobjects_objecttypes on table objecttypes...
ALTER TABLE configobjects
ADD CONSTRAINT fk_configobjects_objecttypes FOREIGN KEY
(
  objecttypes_id
)
REFERENCES objecttypes
(
  id
)
ON DELETE CASCADE
ENABLE
;

PROMPT Creating Foreign Key Constraint fk_genericobjects_objecttypes on table objecttypes...
ALTER TABLE genericobjects
ADD CONSTRAINT fk_genericobjects_objecttypes FOREIGN KEY
(
  objecttypes_id
)
REFERENCES objecttypes
(
  id
)
ON DELETE CASCADE
ENABLE
;

PROMPT Creating Foreign Key Constraint fk_managedobjectproperties_man on table managedobjects...
ALTER TABLE managedobjectproperties
ADD CONSTRAINT fk_managedobjectproperties_man FOREIGN KEY
(
  managedobjects_id
)
REFERENCES managedobjects
(
  id
)
ON DELETE CASCADE
ENABLE
;

PROMPT Creating Foreign Key Constraint fk_managedobjects_objectypes on table objecttypes...
ALTER TABLE managedobjects
ADD CONSTRAINT fk_managedobjects_objectypes FOREIGN KEY
(
  objecttypes_id
)
REFERENCES objecttypes
(
  id
)
ON DELETE CASCADE
ENABLE
;

PROMPT Creating Foreign Key Constraint fk_genericobjectproperties_gen on table genericobjects...
ALTER TABLE genericobjectproperties
ADD CONSTRAINT fk_genericobjectproperties_gen FOREIGN KEY
(
  genericobjects_id
)
REFERENCES genericobjects
(
  id
)
ON DELETE CASCADE
ENABLE
;

PROMPT Creating Foreign Key Constraint fk_updateobjectproperties_man on table updateobjectproperties...
ALTER TABLE updateobjectproperties
ADD CONSTRAINT fk_updateobjectproperties_man FOREIGN KEY
(
  updateobjects_id
)
REFERENCES updateobjects
(
  id
)
ON DELETE CASCADE
ENABLE
;

PROMPT Creating Foreign Key Constraint fk_updateobjects_objectypes on table updateobjects...
ALTER TABLE updateobjects
ADD CONSTRAINT fk_updateobjects_objectypes FOREIGN KEY
(
  objecttypes_id
)
REFERENCES objecttypes
(
  id
)
ON DELETE CASCADE
ENABLE
;

CREATE OR REPLACE TRIGGER genericobjects_id_TRG BEFORE INSERT ON genericobjects
FOR EACH ROW
DECLARE
v_newVal NUMBER(12) := 0;
v_incval NUMBER(12) := 0;
BEGIN
  IF INSERTING AND :new.id IS NULL THEN
    SELECT  genericobjects_id_SEQ.NEXTVAL INTO v_newVal FROM DUAL;
    -- If this is the first time this table have been inserted into (sequence == 1)
    IF v_newVal = 1 THEN
      --get the max indentity value from the table
      SELECT NVL(max(id),0) INTO v_newVal FROM genericobjects;
      v_newVal := v_newVal + 1;
      --set the sequence to that value
      LOOP
           EXIT WHEN v_incval>=v_newVal;
           SELECT genericobjects_id_SEQ.nextval INTO v_incval FROM dual;
      END LOOP;
    END IF;
    --used to emulate LAST_INSERT_ID()
    --mysql_utilities.identity := v_newVal;
   -- assign the value from the sequence to emulate the identity column
   :new.id := v_newVal;
  END IF;
END;

/


CREATE OR REPLACE TRIGGER configobjects_id_TRG BEFORE INSERT ON configobjects
FOR EACH ROW
DECLARE
v_newVal NUMBER(12) := 0;
v_incval NUMBER(12) := 0;
BEGIN
  IF INSERTING AND :new.id IS NULL THEN
    SELECT  configobjects_id_SEQ.NEXTVAL INTO v_newVal FROM DUAL;
    -- If this is the first time this table have been inserted into (sequence == 1)
    IF v_newVal = 1 THEN
      --get the max indentity value from the table
      SELECT NVL(max(id),0) INTO v_newVal FROM configobjects;
      v_newVal := v_newVal + 1;
      --set the sequence to that value
      LOOP
           EXIT WHEN v_incval>=v_newVal;
           SELECT configobjects_id_SEQ.nextval INTO v_incval FROM dual;
      END LOOP;
    END IF;
    --used to emulate LAST_INSERT_ID()
    --mysql_utilities.identity := v_newVal;
   -- assign the value from the sequence to emulate the identity column
   :new.id := v_newVal;
  END IF;
END;

/

CREATE OR REPLACE TRIGGER managedobjects_id_TRG BEFORE INSERT ON managedobjects
FOR EACH ROW
DECLARE
v_newVal NUMBER(12) := 0;
v_incval NUMBER(12) := 0;
BEGIN
  IF INSERTING AND :new.id IS NULL THEN
    SELECT  managedobjects_id_SEQ.NEXTVAL INTO v_newVal FROM DUAL;
    -- If this is the first time this table have been inserted into (sequence == 1)
    IF v_newVal = 1 THEN
      --get the max indentity value from the table
      SELECT NVL(max(id),0) INTO v_newVal FROM managedobjects;
      v_newVal := v_newVal + 1;
      --set the sequence to that value
      LOOP
           EXIT WHEN v_incval>=v_newVal;
           SELECT managedobjects_id_SEQ.nextval INTO v_incval FROM dual;
      END LOOP;
    END IF;
    --used to emulate LAST_INSERT_ID()
    --mysql_utilities.identity := v_newVal;
   -- assign the value from the sequence to emulate the identity column
   :new.id := v_newVal;
  END IF;
END;

/

CREATE OR REPLACE TRIGGER clusterobjects_id_TRG BEFORE INSERT ON clusterobjects
FOR EACH ROW
DECLARE
v_newVal NUMBER(12) := 0;
v_incval NUMBER(12) := 0;
BEGIN
  IF INSERTING AND :new.id IS NULL THEN
    SELECT  clusterobjects_id_SEQ.NEXTVAL INTO v_newVal FROM DUAL;
    -- If this is the first time this table have been inserted into (sequence == 1)
    IF v_newVal = 1 THEN
      --get the max indentity value from the table
      SELECT NVL(max(id),0) INTO v_newVal FROM clusterobjects;
      v_newVal := v_newVal + 1;
      --set the sequence to that value
      LOOP
           EXIT WHEN v_incval>=v_newVal;
           SELECT clusterobjects_id_SEQ.nextval INTO v_incval FROM dual;
      END LOOP;
    END IF;
    --used to emulate LAST_INSERT_ID()
    --mysql_utilities.identity := v_newVal;
   -- assign the value from the sequence to emulate the identity column
   :new.id := v_newVal;
  END IF;
END;

/

CREATE OR REPLACE TRIGGER schedulerobjects_id_TRG BEFORE INSERT ON schedulerobjects
FOR EACH ROW
DECLARE
v_newVal NUMBER(12) := 0;
v_incval NUMBER(12) := 0;
BEGIN
  IF INSERTING AND :new.id IS NULL THEN
    SELECT  schedulerobjects_id_SEQ.NEXTVAL INTO v_newVal FROM DUAL;
    -- If this is the first time this table have been inserted into (sequence == 1)
    IF v_newVal = 1 THEN
      --get the max indentity value from the table
      SELECT NVL(max(id),0) INTO v_newVal FROM schedulerobjects;
      v_newVal := v_newVal + 1;
      --set the sequence to that value
      LOOP
           EXIT WHEN v_incval>=v_newVal;
           SELECT schedulerobjects_id_SEQ.nextval INTO v_incval FROM dual;
      END LOOP;
    END IF;
    --used to emulate LAST_INSERT_ID()
    --mysql_utilities.identity := v_newVal;
   -- assign the value from the sequence to emulate the identity column
   :new.id := v_newVal;
  END IF;
END;

/

CREATE OR REPLACE TRIGGER clusterobjects_id_TRG BEFORE INSERT ON clusterobjects
FOR EACH ROW
  DECLARE
    v_newVal NUMBER(12) := 0;
    v_incval NUMBER(12) := 0;
  BEGIN
    IF INSERTING AND :new.id IS NULL THEN
      SELECT  clusterobjects_id_SEQ.NEXTVAL INTO v_newVal FROM DUAL;
-- If this is the first time this table have been inserted into (sequence == 1)
      IF v_newVal = 1 THEN
--get the max indentity value from the table
        SELECT NVL(max(id),0) INTO v_newVal FROM clusterobjects;
        v_newVal := v_newVal + 1;
--set the sequence to that value
        LOOP
          EXIT WHEN v_incval>=v_newVal;
          SELECT clusterobjects_id_SEQ.nextval INTO v_incval FROM dual;
        END LOOP;
      END IF;
--used to emulate LAST_INSERT_ID()
--mysql_utilities.identity := v_newVal;
-- assign the value from the sequence to emulate the identity column
      :new.id := v_newVal;
    END IF;
  END;

/

CREATE OR REPLACE TRIGGER objecttypes_id_TRG BEFORE INSERT ON objecttypes
FOR EACH ROW
DECLARE
v_newVal NUMBER(12) := 0;
v_incval NUMBER(12) := 0;
BEGIN
  IF INSERTING AND :new.id IS NULL THEN
    SELECT  objecttypes_id_SEQ.NEXTVAL INTO v_newVal FROM DUAL;
    -- If this is the first time this table have been inserted into (sequence == 1)
    IF v_newVal = 1 THEN
      --get the max indentity value from the table
      SELECT NVL(max(id),0) INTO v_newVal FROM objecttypes;
      v_newVal := v_newVal + 1;
      --set the sequence to that value
      LOOP
           EXIT WHEN v_incval>=v_newVal;
           SELECT objecttypes_id_SEQ.nextval INTO v_incval FROM dual;
      END LOOP;
    END IF;
    --used to emulate LAST_INSERT_ID()
    --mysql_utilities.identity := v_newVal;
   -- assign the value from the sequence to emulate the identity column
   :new.id := v_newVal;
  END IF;
END;

/

CREATE OR REPLACE TRIGGER relationships_id_TRG BEFORE INSERT ON relationships
FOR EACH ROW
  DECLARE
    v_newVal NUMBER(12) := 0;
    v_incval NUMBER(12) := 0;
  BEGIN
    IF INSERTING AND :new.id IS NULL THEN
      SELECT  relationships_id_SEQ.NEXTVAL INTO v_newVal FROM DUAL;
      -- If this is the first time this table have been inserted into (sequence == 1)
      IF v_newVal = 1 THEN
        --get the max indentity value from the table
        SELECT NVL(max(id),0) INTO v_newVal FROM relationships;
        v_newVal := v_newVal + 1;
        --set the sequence to that value
        LOOP
          EXIT WHEN v_incval>=v_newVal;
          SELECT relationships_id_SEQ.nextval INTO v_incval FROM dual;
        END LOOP;
      END IF;
      --used to emulate LAST_INSERT_ID()
      --mysql_utilities.identity := v_newVal;
     -- assign the value from the sequence to emulate the identity column
      :new.id := v_newVal;
    END IF;
  END;

/

INSERT INTO internaluser (objectid, rev, pwd, roles) VALUES ('openidm-admin', '0', 'openidm-admin', '[ { "_ref" : "repo/internal/role/openidm-admin" }, { "_ref" : "repo/internal/role/openidm-authorized" } ]');

INSERT INTO internaluser (objectid, rev, pwd, roles) VALUES ('anonymous', '0', 'anonymous', '[ { "_ref" : "repo/internal/role/openidm-reg" } ]');

INSERT ALL
    INTO internalrole (objectid, rev, description)
         VALUES ('openidm-admin', 0, 'Administrative access')
    INTO internalrole (objectid, rev, description)
         VALUES ('openidm-authorized', 0, 'Basic minimum user')
    INTO internalrole (objectid, rev, description)
         VALUES ('openidm-cert', 0, 'Authenticated via certificate')
    INTO internalrole (objectid, rev, description)
         VALUES ('openidm-reg', 0, 'Anonymous access')
    INTO internalrole (objectid, rev, description)
         VALUES ('openidm-tasks-manager', 0, 'Allowed to reassign workflow tasks')
SELECT * FROM dual;

COMMIT;
