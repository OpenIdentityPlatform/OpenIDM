--DROP SCHEMA IF EXISTS "openidm" CASCADE;
CREATE SCHEMA "openidm" AUTHORIZATION "openidm";

-- -----------------------------------------------------
-- Table "openidm"."objecttypes"
-- -----------------------------------------------------

CREATE TABLE "openidm"."objecttypes" (
  "id" BIGSERIAL NOT NULL,
  "objecttype" VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "idx_objecttypes_objecttype" UNIQUE ("objecttype")
);



-- -----------------------------------------------------
-- Table "openidm"."genericobjects"
-- -----------------------------------------------------

CREATE TABLE "openidm"."genericobjects" (
  "id" BIGSERIAL NOT NULL,
  "objecttypes_id" BIGINT NOT NULL,
  "objectid" VARCHAR(255) NOT NULL,
  "rev" VARCHAR(38) NOT NULL,
  "fullobject" TEXT,
  PRIMARY KEY ("id"),
  CONSTRAINT "fk_genericobjects_objecttypes" FOREIGN KEY ("objecttypes_id") REFERENCES "openidm"."objecttypes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT "idx_genericobjects_object" UNIQUE ("objecttypes_id", "objectid")
);



-- -----------------------------------------------------
-- Table "openidm"."genericobjectproperties"
-- -----------------------------------------------------

CREATE TABLE "openidm"."genericobjectproperties" (
  "genericobjects_id" BIGINT NOT NULL,
  "propkey" VARCHAR(255) NOT NULL,
  "proptype" VARCHAR(32) DEFAULT NULL,
  "propvalue" TEXT NULL,
  CONSTRAINT "fk_genericobjectproperties_genericobjects" FOREIGN KEY ("genericobjects_id") REFERENCES "openidm"."genericobjects" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
);
CREATE INDEX "fk_genericobjectproperties_genericobjects" ON "openidm"."genericobjectproperties" ("genericobjects_id");
CREATE INDEX "idx_genericobjectproperties_prop" ON "openidm"."genericobjectproperties" ("propkey","propvalue");



-- -----------------------------------------------------
-- Table "openidm"."managedobjects"
-- -----------------------------------------------------

CREATE TABLE "openidm"."managedobjects" (
  "id" BIGSERIAL NOT NULL,
  "objecttypes_id" BIGINT NOT NULL,
  "objectid" VARCHAR(255) NOT NULL,
  "rev" VARCHAR(38) NOT NULL,
  "fullobject" TEXT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "fk_managedobjects_objectypes" FOREIGN KEY ("objecttypes_id") REFERENCES "openidm"."objecttypes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE UNIQUE INDEX "idx-managedobjects_object" ON "openidm"."managedobjects" ("objecttypes_id","objectid");
CREATE INDEX "fk_managedobjects_objectypes" ON "openidm"."managedobjects" ("objecttypes_id");


-- -----------------------------------------------------
-- Table "openidm"."managedobjectproperties"
-- -----------------------------------------------------

CREATE TABLE "openidm"."managedobjectproperties" (
  "managedobjects_id" BIGINT NOT NULL,
  "propkey" VARCHAR(255) NOT NULL,
  "proptype" VARCHAR(32) DEFAULT NULL,
  "propvalue" TEXT NULL,
  CONSTRAINT "fk_managedobjectproperties_managedobjects" FOREIGN KEY ("managedobjects_id") REFERENCES "openidm"."managedobjects" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE INDEX "fk_managedobjectproperties_managedobjects" ON "openidm"."managedobjectproperties" ("managedobjects_id");
CREATE INDEX "idx_managedobjectproperties_prop" ON "openidm"."managedobjectproperties" ("propkey","propvalue");


-- -----------------------------------------------------
-- Table "openidm"."configobjects"
-- -----------------------------------------------------

CREATE TABLE "openidm"."configobjects" (
  "id" BIGSERIAL NOT NULL,
  "objecttypes_id" BIGINT NOT NULL,
  "objectid" VARCHAR(255) NOT NULL,
  "rev" VARCHAR(38) NOT NULL,
  "fullobject" TEXT,
  PRIMARY KEY ("id"),
  CONSTRAINT "fk_configobjects_objecttypes" FOREIGN KEY ("objecttypes_id") REFERENCES "openidm"."objecttypes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE UNIQUE INDEX "idx_configobjects_object" ON "openidm"."configobjects" ("objecttypes_id","objectid");
CREATE INDEX "fk_configobjects_objecttypes" ON "openidm"."configobjects" ("objecttypes_id");


-- -----------------------------------------------------
-- Table "openidm"."configobjectproperties"
-- -----------------------------------------------------

CREATE TABLE "openidm"."configobjectproperties" (
  "configobjects_id" BIGINT NOT NULL,
  "propkey" VARCHAR(255) NOT NULL,
  "proptype" VARCHAR(32) DEFAULT NULL,
  "propvalue" TEXT,
  CONSTRAINT "fk_configobjectproperties_configobjects" FOREIGN KEY ("configobjects_id") REFERENCES "openidm"."configobjects" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE INDEX "fk_configobjectproperties_configobjects" ON "openidm"."configobjectproperties" ("configobjects_id");
CREATE INDEX "idx_configobjectproperties_prop" ON "openidm"."configobjectproperties" ("propkey","propvalue");


-- -----------------------------------------------------
-- Table "openidm"."links"
-- -----------------------------------------------------

CREATE TABLE "openidm"."links" (
  "objectid" VARCHAR(38) NOT NULL,
  "rev" VARCHAR(38) NOT NULL,
  "linktype" VARCHAR(510) NOT NULL,
  "firstid" VARCHAR(255) NOT NULL,
  "secondid" VARCHAR(255) NOT NULL,
  PRIMARY KEY ("objectid")
);

CREATE INDEX "idx_links_first" ON "openidm"."links" ("linktype", "firstid");
CREATE INDEX "idx_links_second" ON "openidm"."links" ("linktype", "secondid");


-- -----------------------------------------------------
-- Table "openidm"."auditaccess"
-- -----------------------------------------------------

CREATE TABLE "openidm"."auditaccess" (
  "objectid" VARCHAR(38) NOT NULL,
  "activitydate" VARCHAR(29) DEFAULT NULL,
  "activity" VARCHAR(24) DEFAULT NULL,
  "ip" VARCHAR(40) DEFAULT NULL,
  "principal" TEXT,
  "roles" VARCHAR(1024) DEFAULT NULL,
  "status" VARCHAR(7) DEFAULT NULL,
  PRIMARY KEY ("objectid")
);


-- -----------------------------------------------------
-- Table "openidm"."auditactivity"
-- -----------------------------------------------------

CREATE TABLE "openidm"."auditactivity" (
  "objectid" VARCHAR(38) NOT NULL,
  "rootactionid" VARCHAR(511) DEFAULT NULL,
  "parentactionid" VARCHAR(511) DEFAULT NULL,
  "activityid" VARCHAR(511) DEFAULT NULL,
  "activitydate" VARCHAR(29) DEFAULT NULL,
  "activity" VARCHAR(24) DEFAULT NULL,
  "message" TEXT NULL,
  "subjectid" VARCHAR(511) DEFAULT NULL,
  "subjectrev" VARCHAR(38) DEFAULT NULL,
  "requester" TEXT NULL,
  "approver" TEXT NULL,
  "subjectbefore" TEXT NULL,
  "subjectafter" TEXT NULL,
  "changedfields" VARCHAR(255) DEFAULT NULL,
  "passwordchanged" VARCHAR(5) DEFAULT NULL,
  "status" VARCHAR(7) DEFAULT NULL,
  PRIMARY KEY ("objectid")
);

CREATE INDEX "idx_auditactivity_rootactionid" ON "openidm"."auditactivity" ("rootactionid");


-- -----------------------------------------------------
-- Table "openidm"."auditrecon"
-- -----------------------------------------------------

CREATE TABLE "openidm"."auditrecon" (
  "objectid" VARCHAR(38) NOT NULL,
  "entrytype" VARCHAR(7) DEFAULT NULL,
  "rootactionid" VARCHAR(511) DEFAULT NULL,
  "reconid" VARCHAR(36) DEFAULT NULL,
  "reconciling" VARCHAR(12) DEFAULT NULL,
  "sourceobjectid" VARCHAR(511) DEFAULT NULL,
  "targetobjectid" VARCHAR(511) DEFAULT NULL,
  "ambiguoustargetobjectids" TEXT,
  "activitydate" VARCHAR(29) DEFAULT NULL,
  "situation" VARCHAR(24) DEFAULT NULL,
  "activity" VARCHAR(24) DEFAULT NULL,
  "status" VARCHAR(7) DEFAULT NULL,
  "message" TEXT NULL,
  "actionid" VARCHAR(255) NULL ,
  "exceptiondetail" TEXT NULL ,
  "mapping" TEXT NULL ,
  "messagedetail" TEXT NULL ,
  PRIMARY KEY ("objectid")
);


-- -----------------------------------------------------
-- Table "openidm"."internaluser"
-- -----------------------------------------------------

CREATE TABLE "openidm"."internaluser" (
  "objectid" VARCHAR(254) NOT NULL,
  "rev" VARCHAR(38) NOT NULL,
  "pwd" VARCHAR(510) DEFAULT NULL,
  "roles" VARCHAR(1024) DEFAULT NULL,
  PRIMARY KEY ("objectid")
);

-- -----------------------------------------------------
-- Table "openidm"."schedulerobjects"
-- -----------------------------------------------------

CREATE TABLE "openidm"."schedulerobjects" (
  "id" BIGSERIAL NOT NULL,
  "objecttypes_id" BIGINT NOT NULL,
  "objectid" VARCHAR(255) NOT NULL,
  "rev" VARCHAR(38) NOT NULL,
  "fullobject" TEXT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "fk_schedulerobjects_objectypes" FOREIGN KEY ("objecttypes_id") REFERENCES "openidm"."objecttypes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE UNIQUE INDEX "idx-schedulerobjects_object" ON "openidm"."schedulerobjects" ("objecttypes_id","objectid");
CREATE INDEX "fk_schedulerobjects_objectypes" ON "openidm"."schedulerobjects" ("objecttypes_id");


-- -----------------------------------------------------
-- Table "openidm"."schedulerobjectproperties"
-- -----------------------------------------------------

CREATE TABLE "openidm"."schedulerobjectproperties" (
  "schedulerobjects_id" BIGINT NOT NULL,
  "propkey" VARCHAR(255) NOT NULL,
  "proptype" VARCHAR(32) DEFAULT NULL,
  "propvalue" TEXT NULL,
  CONSTRAINT "fk_schedulerobjectproperties_schedulerobjects" FOREIGN KEY ("schedulerobjects_id") REFERENCES "openidm"."schedulerobjects" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE INDEX "fk_schedulerobjectproperties_schedulerobjects" ON "openidm"."schedulerobjectproperties" ("schedulerobjects_id");
CREATE INDEX "idx_schedulerobjectproperties_prop" ON "openidm"."schedulerobjectproperties" ("propkey","propvalue");


-- -----------------------------------------------------
-- Table "openidm"."clusterobjects"
-- -----------------------------------------------------

CREATE TABLE "openidm"."clusterobjects" (
  "id" BIGSERIAL NOT NULL,
  "objecttypes_id" BIGINT NOT NULL,
  "objectid" VARCHAR(255) NOT NULL,
  "rev" VARCHAR(38) NOT NULL,
  "fullobject" TEXT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "fk_clusterobjects_objectypes" FOREIGN KEY ("objecttypes_id") REFERENCES "openidm"."objecttypes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE UNIQUE INDEX "idx-clusterobjects_object" ON "openidm"."clusterobjects" ("objecttypes_id","objectid");
CREATE INDEX "fk_clusterobjects_objectypes" ON "openidm"."clusterobjects" ("objecttypes_id");


-- -----------------------------------------------------
-- Table "openidm"."clusterobjectproperties"
-- -----------------------------------------------------

CREATE TABLE "openidm"."clusterobjectproperties" (
  "clusterobjects_id" BIGINT NOT NULL,
  "propkey" VARCHAR(255) NOT NULL,
  "proptype" VARCHAR(32) DEFAULT NULL,
  "propvalue" TEXT NULL,
  CONSTRAINT "fk_clusterobjectproperties_clusterobjects" FOREIGN KEY ("clusterobjects_id") REFERENCES "openidm"."clusterobjects" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE INDEX "fk_clusterobjectproperties_clusterobjects" ON "openidm"."clusterobjectproperties" ("clusterobjects_id");
CREATE INDEX "idx_clusterobjectproperties_prop" ON "openidm"."clusterobjectproperties" ("propkey","propvalue");


-- -----------------------------------------------------
-- Table "openidm"."uinotification"
-- -----------------------------------------------------
CREATE TABLE "openidm"."uinotification" (
  "objectid" VARCHAR(38) NOT NULL ,
  "rev" VARCHAR(38) NOT NULL ,
  "notificationType" VARCHAR(255) NOT NULL ,
  "createDate" VARCHAR(255) NOT NULL ,
  "message" TEXT NOT NULL ,
  "requester" VARCHAR(255) NULL ,
  "receiverId" VARCHAR(38) NOT NULL ,
  "requesterId" VARCHAR(38) NULL ,
  "notificationSubtype" VARCHAR(255) NULL ,
  PRIMARY KEY ("objectid") );


-- -----------------------------------------------------
-- Data for table "openidm"."internaluser"
-- -----------------------------------------------------
START TRANSACTION;
INSERT INTO "openidm"."internaluser" ("objectid", "rev", "pwd", "roles") VALUES ('openidm-admin', '0', '{"$crypto":{"value":{"iv":"CQRP6Ev4yRHU8Qst/WVq8Q==","data":"OCHb0Y/dU1eAF7mxJRK7vg==","cipher":"AES/CBC/PKCS5Padding","key":"dev-key"},"type":"x-simple-encryption"}}', 'openidm-admin,openidm-authorized');
INSERT INTO "openidm"."internaluser" ("objectid", "rev", "pwd", "roles") VALUES ('anonymous', '0', 'anonymous', 'openidm-reg');

COMMIT;
-- -------------------------------------------
-- openidm database user
-- ------------------------------------------
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA openidm TO openidm;
