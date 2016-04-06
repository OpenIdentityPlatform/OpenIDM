CREATE SCHEMA IF NOT EXISTS `openidm`;

CREATE  TABLE IF NOT EXISTS `openidm`.`objecttypes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttype` VARCHAR(255) NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx_objecttypes_objecttype` (`objecttype` ASC) );

CREATE  TABLE IF NOT EXISTS `openidm`.`genericobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx_genericobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  CONSTRAINT `fk_genericobjects_objecttypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION) ;

  CREATE INDEX IF NOT EXISTS `openidm`.`fk_genericobjects_objecttypes`
  ON `openidm`.`genericobjects` ( `objecttypes_id` ASC );


CREATE  TABLE IF NOT EXISTS `openidm`.`genericobjectproperties` (
  `genericobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) NULL ,
  `propvalue` VARCHAR(2000) NULL ,
  CONSTRAINT `fk_genericobjectproperties_genericobjects`
    FOREIGN KEY (`genericobjects_id` )
    REFERENCES `openidm`.`genericobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);


CREATE INDEX  IF NOT EXISTS  `openidm`.`fk_genericobjectproperties_genericobjects` ON
`openidm`.`genericobjectproperties` (`genericobjects_id` ASC);

CREATE INDEX  IF NOT EXISTS  `openidm`.`idx_genericobjectproperties_propkey` ON `openidm`.`genericobjectproperties` (`propkey` ASC);
CREATE INDEX  IF NOT EXISTS  `openidm`.`idx_genericobjectproperties_propvalue` ON `openidm`.`genericobjectproperties` (`propvalue` ASC);

CREATE  TABLE IF NOT EXISTS `openidm`.`managedobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx-managedobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  CONSTRAINT `fk_managedobjects_objectypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `openidm`.`fk_managedobjects_objectypes` ON `openidm`.`managedobjects`  (`objecttypes_id` ASC) ;


CREATE  TABLE IF NOT EXISTS `openidm`.`managedobjectproperties` (
  `managedobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) NULL ,
  `propvalue` VARCHAR(2000) NULL ,
  CONSTRAINT `fk_managedobjectproperties_managedobjects`
    FOREIGN KEY (`managedobjects_id` )
    REFERENCES `openidm`.`managedobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `openidm`.`fk_managedobjectproperties_managedobjects` ON `openidm`.`managedobjectproperties` (`managedobjects_id` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_managedobjectproperties_propkey` ON `openidm`.`managedobjectproperties` (`propkey` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_managedobjectproperties_propvalue` ON `openidm`.`managedobjectproperties` (`propvalue` ASC);


CREATE  TABLE IF NOT EXISTS `openidm`.`configobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx_configobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  CONSTRAINT `fk_configobjects_objecttypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);


CREATE INDEX IF NOT EXISTS `openidm`.`fk_configobjects_objecttypes` ON `openidm`.`configobjects`(`objecttypes_id` ASC);


CREATE  TABLE IF NOT EXISTS `openidm`.`configobjectproperties` (
  `configobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(255) NULL ,
  `propvalue` VARCHAR(2000) NULL ,
  CONSTRAINT `fk_configobjectproperties_configobjects`
    FOREIGN KEY (`configobjects_id` )
    REFERENCES `openidm`.`configobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `openidm`.`fk_configobjectproperties_configobjects` ON `openidm`.`configobjectproperties`(`configobjects_id` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_configobjectproperties_propkey` ON `openidm`.`configobjectproperties`(`propkey` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_configobjectproperties_propvalue` ON `openidm`.`configobjectproperties`(`propvalue` ASC);


CREATE  TABLE IF NOT EXISTS `openidm`.`relationships` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx_relationships_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  CONSTRAINT `fk_relationships_objecttypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);


CREATE INDEX IF NOT EXISTS `openidm`.`fk_relationships_objecttypes` ON `openidm`.`relationships`(`objecttypes_id` ASC);


CREATE  TABLE IF NOT EXISTS `openidm`.`relationshipproperties` (
  `relationships_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(255) NULL ,
  `propvalue` VARCHAR(2000) NULL ,
  CONSTRAINT `fk_relationshipproperties_relationships`
    FOREIGN KEY (`relationships_id` )
    REFERENCES `openidm`.`relationships` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `openidm`.`fk_relationshipproperties_relationships` ON `openidm`.`relationshipproperties`(`relationships_id` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_relationshipproperties_propkey` ON `openidm`.`relationshipproperties`(`propkey` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_relationshipproperties_propvalue` ON `openidm`.`relationshipproperties`(`propvalue` ASC);

CREATE  TABLE IF NOT EXISTS `openidm`.`links` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `linktype` VARCHAR(50) NOT NULL ,
  `linkqualifier` VARCHAR(50) NOT NULL ,
  `firstid` VARCHAR(255) NOT NULL ,
  `secondid` VARCHAR(255) NOT NULL ,
  UNIQUE INDEX `idx_links_first` (`linktype` ASC, `linkqualifier` ASC, `firstid` ASC) ,
  UNIQUE INDEX `idx_links_second` (`linktype` ASC, `linkqualifier` ASC, `secondid` ASC) ,
  PRIMARY KEY (`objectid`) );


CREATE  TABLE IF NOT EXISTS `openidm`.`security` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `storestring` TEXT NULL ,
  PRIMARY KEY (`objectid`) );


CREATE  TABLE IF NOT EXISTS `openidm`.`securitykeys` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `keypair` TEXT NULL ,
  PRIMARY KEY (`objectid`) );

CREATE TABLE IF NOT EXISTS `openidm`.`auditauthentication` (
  `objectid` VARCHAR(56) NOT NULL ,
  `transactionid` VARCHAR(255) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `userid` VARCHAR(255) NULL ,
  `eventname` VARCHAR(50) NULL ,
  `result` VARCHAR(255) NULL ,
  `principals` TEXT ,
  `context` TEXT ,
  `entries` TEXT ,
  `trackingids` TEXT,
  PRIMARY KEY (`objectid`) );

CREATE TABLE IF NOT EXISTS `openidm`.`auditrecon` (
  `objectid` VARCHAR(56) NOT NULL ,
  `transactionid` VARCHAR(255) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `eventname` VARCHAR(50) NULL ,
  `userid` VARCHAR(255) NULL ,
  `trackingids` TEXT,
  `activity` VARCHAR(24) NULL ,
  `exceptiondetail` TEXT NULL ,
  `linkqualifier` VARCHAR(255) NULL ,
  `mapping` VARCHAR(511) NULL ,
  `message` TEXT NULL ,
  `messagedetail` MEDIUMTEXT NULL ,
  `situation` VARCHAR(24) NULL ,
  `sourceobjectid` VARCHAR(511) NULL ,
  `status` VARCHAR(20) NULL ,
  `targetobjectid` VARCHAR(511) NULL ,
  `reconciling` VARCHAR(12) NULL ,
  `ambiguoustargetobjectids` MEDIUMTEXT NULL ,
  `reconaction` VARCHAR(36) NULL ,
  `entrytype` VARCHAR(7) NULL ,
  `reconid` VARCHAR(56) NULL ,
  PRIMARY KEY (`objectid`) );


CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_transactionid` ON `openidm`.`auditrecon` (`transactionid` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_targetobjectid` ON `openidm`.`auditrecon`(`targetobjectid` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_sourceobjectid` ON `openidm`.`auditrecon`(`sourceobjectid` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_activitydate` ON `openidm`.`auditrecon`(`activitydate` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_entrytype` ON `openidm`.`auditrecon`(`entrytype` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_situation` ON `openidm`.`auditrecon`(`situation` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_status` ON `openidm`.`auditrecon`(`status` ASC);


CREATE TABLE IF NOT EXISTS `openidm`.`auditsync` (
  `objectid` VARCHAR(56) NOT NULL ,
  `transactionid` VARCHAR(255) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `eventname` VARCHAR(50) NULL ,
  `userid` VARCHAR(255) NULL ,
  `trackingids` TEXT,
  `activity` VARCHAR(24) NULL ,
  `exceptiondetail` TEXT NULL ,
  `linkqualifier` VARCHAR(255) NULL ,
  `mapping` VARCHAR(511) NULL ,
  `message` TEXT NULL ,
  `messagedetail` MEDIUMTEXT NULL ,
  `situation` VARCHAR(24) NULL ,
  `sourceobjectid` VARCHAR(511) NULL ,
  `status` VARCHAR(20) NULL ,
  `targetobjectid` VARCHAR(511) NULL ,
  PRIMARY KEY (`objectid`) );

CREATE  TABLE IF NOT EXISTS `openidm`.`auditconfig` (
  `objectid` VARCHAR(56) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `eventname` VARCHAR(255) NULL ,
  `transactionid` VARCHAR(255) NOT NULL ,
  `userid` VARCHAR(255) NULL ,
  `trackingids` TEXT,
  `runas` VARCHAR(255) NULL ,
  `configobjectid` VARCHAR(255) NULL ,
  `operation` VARCHAR(255) NULL ,
  `beforeObject` MEDIUMTEXT NULL ,
  `afterObject` MEDIUMTEXT NULL ,
  `changedfields` MEDIUMTEXT NULL ,
  `rev` VARCHAR(255) NULL ,
  PRIMARY KEY (`objectid`));

CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditconfig_transactionid` ON  `openidm`.`auditconfig`(`transactionid` ASC);


CREATE  TABLE IF NOT EXISTS `openidm`.`auditactivity` (
  `objectid` VARCHAR(56) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `eventname` VARCHAR(255) NULL ,
  `transactionid` VARCHAR(255) NOT NULL ,
  `userid` VARCHAR(255) NULL ,
  `trackingids` TEXT,
  `runas` VARCHAR(255) NULL ,
  `activityobjectid` VARCHAR(255) NULL ,
  `operation` VARCHAR(255) NULL ,
  `subjectbefore` MEDIUMTEXT NULL ,
  `subjectafter` MEDIUMTEXT NULL ,
  `changedfields` MEDIUMTEXT NULL ,
  `subjectrev` VARCHAR(255) NULL ,
  `passwordchanged` VARCHAR(5) NULL ,
  `message` TEXT NULL,
  `status` VARCHAR(20) ,
  PRIMARY KEY (`objectid`));

CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditactivity_transactionid` ON  `openidm`.`auditactivity`(`transactionid` ASC);

CREATE  TABLE IF NOT EXISTS `openidm`.`auditaccess` (
  `objectid` VARCHAR(56) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `eventname` VARCHAR(255) ,
  `transactionid` VARCHAR(255) NOT NULL ,
  `userid` VARCHAR(255) NULL ,
  `trackingids` TEXT NULL ,
  `server_ip` VARCHAR(40) ,
  `server_port` VARCHAR(5) ,
  `client_ip` VARCHAR(40) ,
  `client_port` VARCHAR(5) ,
  `request_protocol` VARCHAR(255) NULL ,
  `request_operation` VARCHAR(255) NULL ,
  `request_detail` TEXT NULL ,
  `http_request_secure` VARCHAR(255) NULL ,
  `http_request_method` VARCHAR(255) NULL ,
  `http_request_path` VARCHAR(255) NULL ,
  `http_request_queryparameters` TEXT NULL ,
  `http_request_headers` TEXT NULL ,
  `http_request_cookies` TEXT NULL ,
  `http_response_headers` TEXT NULL ,
  `response_status` VARCHAR(255) NULL ,
  `response_statuscode` VARCHAR(255) NULL ,
  `response_elapsedtime` VARCHAR(255) NULL ,
  `response_elapsedtimeunits` VARCHAR(255) NULL ,
  `roles` TEXT NULL ,
  PRIMARY KEY (`objectid`) );

CREATE  TABLE IF NOT EXISTS `openidm`.`internaluser` (
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `pwd` VARCHAR(510) NULL ,
  `roles` VARCHAR(1024) NULL ,
  PRIMARY KEY (`objectid`) );

CREATE TABLE IF NOT EXISTS `openidm`.`internalrole` (
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `description` VARCHAR(510) NULL,
  PRIMARY KEY (`objectid`) );

CREATE  TABLE IF NOT EXISTS `openidm`.`schedulerobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx-schedulerobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  CONSTRAINT `fk_schedulerobjects_objectypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `openidm`.`fk_schedulerobjects_objectypes` ON  `openidm`.`schedulerobjects` (`objecttypes_id` ASC);

CREATE  TABLE IF NOT EXISTS `openidm`.`schedulerobjectproperties` (
  `schedulerobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) NULL ,
  `propvalue` VARCHAR(2000) NULL ,
  CONSTRAINT `fk_schedulerobjectproperties_schedulerobjects`
    FOREIGN KEY (`schedulerobjects_id` )
    REFERENCES `openidm`.`schedulerobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `openidm`.`fk_schedulerobjectproperties_schedulerobjects` ON `openidm`.`schedulerobjectproperties`(`schedulerobjects_id` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_schedulerobjectproperties_propkey` ON `openidm`.`schedulerobjectproperties`(`propkey` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_schedulerobjectproperties_propvalue` ON `openidm`.`schedulerobjectproperties`(`propvalue` ASC);

CREATE  TABLE IF NOT EXISTS `openidm`.`clusterobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx-clusterobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  CONSTRAINT `fk_clusterobjects_objectypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `openidm`.`fk_clusterobjects_objectypes` ON `openidm`.`clusterobjects` (`objecttypes_id` ASC);

CREATE  TABLE IF NOT EXISTS `openidm`.`clusterobjectproperties` (
  `clusterobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) NULL ,
  `propvalue` VARCHAR(2000) NULL ,
  CONSTRAINT `fk_clusterobjectproperties_clusterobjects`
    FOREIGN KEY (`clusterobjects_id` )
    REFERENCES `openidm`.`clusterobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `fk_clusterobjectproperties_clusterobjects` ON `openidm`.`clusterobjectproperties`  (`clusterobjects_id` ASC);
CREATE INDEX IF NOT EXiSTS `idx_clusterobjectproperties_propkey` ON `openidm`.`clusterobjectproperties` (`propkey` ASC);
CREATE INDEX IF NOT EXiSTS `idx_clusterobjectproperties_propvalue` ON `openidm`.`clusterobjectproperties` (`propvalue` ASC);

CREATE  TABLE IF NOT EXISTS `openidm`.`uinotification` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `notificationType` VARCHAR(255) NOT NULL ,
  `createDate` VARCHAR(255) NOT NULL ,
  `message` TEXT NOT NULL ,
  `requester` VARCHAR(255) NULL ,
  `receiverId` VARCHAR(38) NOT NULL ,
  `requesterId` VARCHAR(38) NULL ,
  `notificationSubtype` VARCHAR(255) NULL ,
  PRIMARY KEY (`objectid`) );

CREATE  TABLE IF NOT EXISTS `openidm`.`updateobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx_updateobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  CONSTRAINT `fk_updateobjects_objecttypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION) ;

  CREATE INDEX IF NOT EXISTS `openidm`.`fk_updateobjects_objecttypes`
  ON `openidm`.`updateobjects` ( `objecttypes_id` ASC );


CREATE  TABLE IF NOT EXISTS `openidm`.`updateobjectproperties` (
  `updateobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) NULL ,
  `propvalue` VARCHAR(2000) NULL ,
  CONSTRAINT `fk_updateobjectproperties_updateobjects`
    FOREIGN KEY (`updateobjects_id` )
    REFERENCES `openidm`.`updateobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX  IF NOT EXISTS  `openidm`.`fk_updateobjectproperties_updateobjects` ON
`openidm`.`updateobjectproperties` (`updateobjects_id` ASC);

CREATE INDEX  IF NOT EXISTS  `openidm`.`idx_updateobjectproperties_propkey` ON `openidm`.`updateobjectproperties` (`propkey` ASC);
CREATE INDEX  IF NOT EXISTS  `openidm`.`idx_updateobjectproperties_propvalue` ON `openidm`.`updateobjectproperties` (`propvalue` ASC);


INSERT INTO `openidm`.`internaluser` (`objectid`, `rev`, `pwd`, `roles`)
SELECT 'openidm-admin', '0', 'openidm-admin', '[ { "_ref" : "repo/internal/role/openidm-admin" }, { "_ref" : "repo/internal/role/openidm-authorized" } ]'
WHERE NOT EXISTS (SELECT * FROM `openidm`.`internaluser` WHERE `objectid` = 'openidm-admin');

INSERT INTO `openidm`.`internaluser` (`objectid`, `rev`, `pwd`, `roles`)
SELECT 'anonymous', '0', 'anonymous', '[ { "_ref" : "repo/internal/role/openidm-reg" } ]'
WHERE NOT EXISTS (SELECT * FROM `openidm`.`internaluser` WHERE `objectid` = 'anonymous');

INSERT INTO `openidm`.`internalrole` (`objectid`, `rev`, `description`)
SELECT 'openidm-authorized', '0', 'Basic minimum user'
WHERE NOT EXISTS (SELECT * FROM `openidm`.`internalrole` WHERE `objectid` = 'openidm-authorized')
UNION
SELECT 'openidm-admin', '0', 'Administrative access'
WHERE NOT EXISTS (SELECT * FROM `openidm`.`internalrole` WHERE `objectid` = 'openidm-admin')
UNION
SELECT 'openidm-cert', '0', 'Authenticated via certificate'
WHERE NOT EXISTS (SELECT * FROM `openidm`.`internalrole` WHERE `objectid` = 'openidm-cert')
UNION
SELECT 'openidm-tasks-manager', '0', 'Allowed to reassign workflow tasks'
WHERE NOT EXISTS (SELECT * FROM `openidm`.`internalrole` WHERE `objectid` = 'openidm-tasks-manager')
UNION
SELECT 'openidm-reg', '0', 'Anonymous access'
WHERE NOT EXISTS (SELECT * FROM `openidm`.`internaluser` WHERE `objectid` = 'openidm-reg');
