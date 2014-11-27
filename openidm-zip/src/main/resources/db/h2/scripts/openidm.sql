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
  `propvalue` TEXT NULL ,
  CONSTRAINT `fk_genericobjectproperties_genericobjects`
    FOREIGN KEY (`genericobjects_id` )
    REFERENCES `openidm`.`genericobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);


CREATE INDEX  IF NOT EXISTS  `openidm`.`fk_genericobjectproperties_genericobjects` ON
`openidm`.`genericobjectproperties` (`genericobjects_id` ASC);

CREATE INDEX  IF NOT EXISTS  `openidm`.`idx_genericobjectproperties_prop` ON `openidm`.`genericobjectproperties` (`propkey` ASC, `propvalue` ASC);


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
  `propvalue` TEXT NULL ,
  CONSTRAINT `fk_managedobjectproperties_managedobjects`
    FOREIGN KEY (`managedobjects_id` )
    REFERENCES `openidm`.`managedobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);
    
CREATE INDEX IF NOT EXISTS `openidm`.`idx_managedobjectproperties_prop` ON `openidm`.`managedobjectproperties` (`propkey` ASC, `propvalue` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`fk_managedobjectproperties_managedobjects` ON `openidm`.`managedobjectproperties` (`managedobjects_id` ASC);


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
  `propvalue` TEXT NULL ,
  CONSTRAINT `fk_configobjectproperties_configobjects`
    FOREIGN KEY (`configobjects_id` )
    REFERENCES `openidm`.`configobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `openidm`.`fk_configobjectproperties_configobjects` ON `openidm`.`configobjectproperties`(`configobjects_id` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_configobjectproperties_prop` ON `openidm`.`configobjectproperties`(`propkey` ASC, `propvalue` ASC);


CREATE  TABLE IF NOT EXISTS `openidm`.`links` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `linktype` VARCHAR(255) NOT NULL ,
  `firstid` VARCHAR(255) NOT NULL ,
  `secondid` VARCHAR(255) NOT NULL ,
  UNIQUE INDEX `idx_links_first` (`linktype` ASC, `firstid` ASC) ,
  UNIQUE INDEX `idx_links_second` (`linktype` ASC, `secondid` ASC) ,
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
  
   
CREATE TABLE IF NOT EXISTS `openidm`.`auditrecon` (
  `objectid` VARCHAR(38) NOT NULL ,
  `entrytype` VARCHAR(7) NULL ,
  `rootactionid` VARCHAR(511) NULL ,
  `reconid` VARCHAR(36) NULL ,
  `reconaction` VARCHAR(36) NULL ,
  `reconciling` VARCHAR(12) NULL ,
  `sourceobjectid` VARCHAR(511) NULL ,
  `targetobjectid` VARCHAR(511) NULL ,
  `ambiguoustargetobjectids` MEDIUMTEXT NULL ,
  `activitydate` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `situation` VARCHAR(24) NULL ,
  `activity` VARCHAR(24) NULL ,
  `status` VARCHAR(7) NULL ,
  `message` TEXT NULL ,
  `actionid` VARCHAR(255) NULL ,
  `exceptiondetail` TEXT NULL ,
  `mapping` VARCHAR(511) NULL ,
  `messagedetail` MEDIUMTEXT NULL,
  PRIMARY KEY (`objectid`) );


CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_reconid` ON `openidm`.`auditrecon`(`reconid` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_targetobjectid` ON `openidm`.`auditrecon`(`targetobjectid` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_sourceobjectid` ON `openidm`.`auditrecon`(`sourceobjectid` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_activitydate` ON `openidm`.`auditrecon`(`activitydate` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_entrytype` ON `openidm`.`auditrecon`(`entrytype` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_situation` ON `openidm`.`auditrecon`(`situation` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditrecon_status` ON `openidm`.`auditrecon`(`status` ASC);
  
   
CREATE TABLE IF NOT EXISTS `openidm`.`auditsync` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rootactionid` VARCHAR(511) NULL ,
  `sourceobjectid` VARCHAR(511) NULL ,
  `targetobjectid` VARCHAR(511) NULL ,
  `activitydate` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `situation` VARCHAR(24) NULL ,
  `activity` VARCHAR(24) NULL ,
  `status` VARCHAR(7) NULL ,
  `message` TEXT NULL ,
  `actionid` VARCHAR(255) NULL ,
  `exceptiondetail` TEXT NULL ,
  `mapping` VARCHAR(511) NULL ,
  `messagedetail` MEDIUMTEXT NULL,
  PRIMARY KEY (`objectid`) );
  
  
CREATE  TABLE IF NOT EXISTS `openidm`.`auditactivity` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rootactionid` VARCHAR(511) NULL ,
  `parentactionid` VARCHAR(511) NULL ,
  `activityid` VARCHAR(511) NULL ,
  `activitydate` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `activity` VARCHAR(24) NULL ,
  `message` TEXT NULL ,
  `subjectid` VARCHAR(511) NULL ,
  `subjectrev` VARCHAR(38) NULL ,
  `requester` TEXT NULL ,
  `approver` TEXT NULL ,
  `subjectbefore` MEDIUMTEXT NULL ,
  `subjectafter` MEDIUMTEXT NULL ,
  `status` VARCHAR(7) NULL ,
  `changedfields` VARCHAR(255) NULL ,
  `passwordchanged` VARCHAR(5) NULL,
  PRIMARY KEY (`objectid`));
  
CREATE INDEX IF NOT EXISTS `openidm`.`idx_auditactivity_rootactionid` ON  `openidm`.`auditactivity`(`rootactionid` ASC);    
  
CREATE  TABLE IF NOT EXISTS `openidm`.`internaluser` (
  `objectid` VARCHAR(254) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `pwd` VARCHAR(510) NULL ,
  `roles` VARCHAR(1024) NULL ,
  PRIMARY KEY (`objectid`) );
  
  
CREATE  TABLE IF NOT EXISTS `openidm`.`auditaccess` (
  `objectid` VARCHAR(38) NOT NULL ,
  `activitydate` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `activity` VARCHAR(24) NULL ,
  `ip` VARCHAR(40) NULL ,
  `principal` TEXT NULL ,
  `roles` VARCHAR(1024) NULL ,
  `status` VARCHAR(7) NULL ,
  `userid` VARCHAR(24) NULL ,
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
  `propvalue` TEXT NULL ,
  CONSTRAINT `fk_schedulerobjectproperties_schedulerobjects`
    FOREIGN KEY (`schedulerobjects_id` )
    REFERENCES `openidm`.`schedulerobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `openidm`.`fk_schedulerobjectproperties_schedulerobjects` ON `openidm`.`schedulerobjectproperties`(`schedulerobjects_id` ASC);
CREATE INDEX IF NOT EXISTS `openidm`.`idx_schedulerobjectproperties_prop` ON `openidm`.`schedulerobjectproperties`(`propkey` ASC, `propvalue` ASC);

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
  `propvalue` TEXT NULL ,
  CONSTRAINT `fk_clusterobjectproperties_clusterobjects`
    FOREIGN KEY (`clusterobjects_id` )
    REFERENCES `openidm`.`clusterobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION);

CREATE INDEX IF NOT EXISTS `fk_clusterobjectproperties_clusterobjects` ON `openidm`.`clusterobjectproperties`  (`clusterobjects_id` ASC);
CREATE INDEX IF NOT EXiSTS `idx_clusterobjectproperties_prop` ON `openidm`.`clusterobjectproperties` (`propkey` ASC, `propvalue` ASC);

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

INSERT INTO `openidm`.`internaluser` (`objectid`, `rev`, `pwd`, `roles`) 
SELECT 'openidm-admin', '0', 'openidm-admin', 'openidm-admin,openidm-authorized'
WHERE NOT EXISTS (SELECT * FROM `openidm`.`internaluser` WHERE `objectid` = 'openidm-admin');

INSERT INTO `openidm`.`internaluser` (`objectid`, `rev`, `pwd`, `roles`) 
SELECT 'anonymous', '0', 'anonymous', 'openidm-reg'
WHERE NOT EXISTS (SELECT * FROM `openidm`.`internaluser` WHERE `objectid` = 'anonymous');

    
