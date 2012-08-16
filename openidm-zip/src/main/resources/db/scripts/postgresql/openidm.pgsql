--DROP SCHEMA IF EXISTS "openidm" CASCADE;
CREATE SCHEMA "openidm" AUTHORIZATION "openidm";

-- -----------------------------------------------------
-- Table "openidm"."auditactivity"
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
  "propvalue" TEXT,
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
  "fullobject" TEXT,
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
  "propvalue" TEXT,
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
  "message" TEXT,
  "subjectid" VARCHAR(511) DEFAULT NULL,
  "subjectrev" VARCHAR(38) DEFAULT NULL,
  "requester" TEXT,
  "approver" TEXT,
  "subjectbefore" TEXT,
  "subjectafter" TEXT,
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
  "message" TEXT,
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
-- Data for table "openidm"."internaluser"
-- -----------------------------------------------------
START TRANSACTION;
INSERT INTO "openidm"."internaluser" ("objectid", "rev", "pwd", "roles") VALUES ('openidm-admin', '0', '{"$crypto":{"value":{"iv":"CQRP6Ev4yRHU8Qst/WVq8Q==","data":"OCHb0Y/dU1eAF7mxJRK7vg==","cipher":"AES/CBC/PKCS5Padding","key":"dev-key"},"type":"x-simple-encryption"}}', 'openidm-admin,openidm-authorized');
INSERT INTO "openidm"."internaluser" ("objectid", "rev", "pwd", "roles") VALUES ('anonymous', '0', 'anonymous', 'openidm-reg');

COMMIT;
