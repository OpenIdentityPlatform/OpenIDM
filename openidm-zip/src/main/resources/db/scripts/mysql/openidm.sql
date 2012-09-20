SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `openidm` DEFAULT CHARACTER SET utf8 ;
USE `openidm` ;

-- -----------------------------------------------------
-- Table `openidm`.`objecttypes`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`objecttypes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttype` VARCHAR(255) NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx_objecttypes_objecttype` (`objecttype` ASC) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`genericobjects`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`genericobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  INDEX `fk_genericobjects_objecttypes` (`objecttypes_id` ASC) ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx_genericobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  CONSTRAINT `fk_genericobjects_objecttypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`genericobjectproperties`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`genericobjectproperties` (
  `genericobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) NULL ,
  `propvalue` TEXT NULL ,
  INDEX `fk_genericobjectproperties_genericobjects` (`genericobjects_id` ASC) ,
  INDEX `idx_genericobjectproperties_prop` (`propkey` ASC, `propvalue`(16) ASC) ,
  CONSTRAINT `fk_genericobjectproperties_genericobjects`
    FOREIGN KEY (`genericobjects_id` )
    REFERENCES `openidm`.`genericobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`managedobjects`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`managedobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx-managedobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  INDEX `fk_managedobjects_objectypes` (`objecttypes_id` ASC) ,
  CONSTRAINT `fk_managedobjects_objectypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`managedobjectproperties`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`managedobjectproperties` (
  `managedobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) NULL ,
  `propvalue` TEXT NULL ,
  INDEX `fk_managedobjectproperties_managedobjects` (`managedobjects_id` ASC) ,
  INDEX `idx_managedobjectproperties_prop` (`propkey` ASC, `propvalue`(16) ASC) ,
  CONSTRAINT `fk_managedobjectproperties_managedobjects`
    FOREIGN KEY (`managedobjects_id` )
    REFERENCES `openidm`.`managedobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`managednotification`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`managednotification` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `notificationType` VARCHAR(255) NOT NULL ,
  `requestDate` VARCHAR(255) NOT NULL ,
  `message` TEXT,
  `requester` VARCHAR(255),
  `userName` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `openidm`.`manageduserapplicationlnk`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`manageduserapplicationlnk` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `state` VARCHAR(255) NOT NULL ,
  `applicationId` VARCHAR(255) NOT NULL ,
  `userName` VARCHAR(255) NOT NULL,
  `lastTimeUsed` VARCHAR(255),
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `openidm`.`managedapplication`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`managedapplication` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `description` TEXT,
  `iconPath` VARCHAR(255) NOT NULL,
  `isDefault` BOOLEAN,
  `needsApproval` BOOLEAN,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `openidm`.`manageduserapplicationstate`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`manageduserapplicationstate` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `description` TEXT,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `openidm`.`configobjects`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`configobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `fk_configobjects_objecttypes` (`objecttypes_id` ASC) ,
  UNIQUE INDEX `idx_configobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  CONSTRAINT `fk_configobjects_objecttypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`configobjectproperties`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`configobjectproperties` (
  `configobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) NULL ,
  `propvalue` TEXT NULL ,
  INDEX `fk_configobjectproperties_configobjects` (`configobjects_id` ASC) ,
  INDEX `idx_configobjectproperties_prop` (`propkey` ASC, `propvalue`(16) ASC) ,
  CONSTRAINT `fk_configobjectproperties_configobjects`
    FOREIGN KEY (`configobjects_id` )
    REFERENCES `openidm`.`configobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`links`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`links` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `linktype` VARCHAR(255) NOT NULL ,
  `firstid` VARCHAR(255) NOT NULL ,
  `secondid` VARCHAR(255) NOT NULL ,
  UNIQUE INDEX `idx_links_first` (`linktype` ASC, `firstid` ASC) ,
  UNIQUE INDEX `idx_links_second` (`linktype` ASC, `secondid` ASC) ,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`auditrecon`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`auditrecon` (
  `objectid` VARCHAR(38) NOT NULL ,
  `entrytype` VARCHAR(7) NULL ,
  `rootactionid` VARCHAR(511) NULL ,
  `reconid` VARCHAR(36) NULL ,
  `reconciling` VARCHAR(12) NULL ,
  `sourceobjectid` VARCHAR(511) NULL ,
  `targetobjectid` VARCHAR(511) NULL ,
  `ambiguoustargetobjectids` MEDIUMTEXT NULL ,
  `activitydate` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `situation` VARCHAR(24) NULL ,
  `activity` VARCHAR(24) NULL ,
  `status` VARCHAR(7) NULL ,
  `message` TEXT NULL ,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`auditactivity`
-- -----------------------------------------------------
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
  `passwordchanged` VARCHAR(5) NULL ,
  PRIMARY KEY (`objectid`) ,
  INDEX `idx_auditactivity_rootactionid` (`rootactionid` ASC) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`internaluser`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`internaluser` (
  `objectid` VARCHAR(254) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `pwd` VARCHAR(510) NULL ,
  `roles` VARCHAR(1024) NULL ,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`auditaccess`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`auditaccess` (
  `objectid` VARCHAR(38) NOT NULL ,
  `activitydate` VARCHAR(29) NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `activity` VARCHAR(24) NULL ,
  `ip` VARCHAR(40) NULL ,
  `principal` TEXT NULL ,
  `roles` VARCHAR(1024) NULL ,
  `status` VARCHAR(7) NULL ,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- -----------------------------------------------------
-- Data for table `openidm`.`managedapplication`
-- -----------------------------------------------------
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('F3535924-EF9C-48D0-BFC6-59DE7B190A1B', 0, 'Salesforce', 'Salesforce', 'images/dashboard/salesforce.png', false, true);
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('B65FA6A2-D23D-49CD-BEA0-CE98E275A8CD', 0, 'Gmail', 'Gmail', 'images/dashboard/gmail.png', true, false);
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('2156ACE6-1FFF-46B3-B680-EA16520F55A0', 0, 'GooglePlus', 'Google+', 'images/dashboard/googleplus.png', false, false);
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('2F3B1623-2738-47C5-8050-9523BC0525A6', 0, 'Liveplan', 'Liveplan', 'images/dashboard/liveplan.png', false, false);
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('0F3B1623-2738-47C5-8050-9523BC0525A6', 0, 'Calendar', 'Calendar', 'images/dashboard/calendar.png', true, false);
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('1F3B1623-2738-47C5-8050-9523BC0525A6', 0, 'Concur', 'Concur', 'images/dashboard/concur.png', true, false);
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('3F3B1623-2738-47C5-8050-9523BC0525A6', 0, 'Docs', 'Docs', 'images/dashboard/docs.png', false, false);
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('4F3B1623-2738-47C5-8050-9523BC0525A6', 0, 'Drive', 'Drive', 'images/dashboard/drive.png', false, false);
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('5F3B1623-2738-47C5-8050-9523BC0525A6', 0, 'FusionTables', 'FusionTables', 'images/dashboard/fusion_tables.png', false, false);
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('6F3B1623-2738-47C5-8050-9523BC0525A6', 0, 'Vault', 'Vault', 'images/dashboard/vault.png', false, false);
insert into managedapplication (objectid,rev,name,description,iconPath,isDefault,needsApproval) values ('7F3B1623-2738-47C5-8050-9523BC0525A6', 0, 'Webex', 'Webex', 'images/dashboard/webex.png', true, false);


-- -----------------------------------------------------
-- Data for table `openidm`.`manageduserapplicationstate`
-- -----------------------------------------------------
insert into manageduserapplicationstate (objectid,rev,name,description) values ('B65FA6A2-D43D-49CD-BEA0-CE98E275A8CD', 0, 'pending', 'Request is pending for approval');
insert into manageduserapplicationstate (objectid,rev,name,description) values ('B65FA6A2-D43D-49CB-BEA0-CE98E275A8CD', 0, 'approved', 'Request has been approved');
insert into manageduserapplicationstate (objectid,rev,name,description) values ('B65FA6A2-D43D-49CD-BEA0-CE9CE275A8CD', 0, 'rejected', 'Request has been rejected');

-- -----------------------------------------------------
-- Data for table `openidm`.`internaluser`
-- -----------------------------------------------------
START TRANSACTION;
USE `openidm`;
INSERT INTO `openidm`.`internaluser` (`objectid`, `rev`, `pwd`, `roles`) VALUES ('openidm-admin', '0', '{\"$crypto\":{\"value\":{\"iv\":\"fIevcJYS4TMxClqcK7covg==\",\"data\":\"Tu9o/S+j+rhOIgdp9uYc5Q==\",\"cipher\":\"AES/CBC/PKCS5Padding\",\"key\":\"openidm-sym-default\"},\"type\":\"x-simple-encryption\"}}', 'openidm-admin,openidm-authorized');
INSERT INTO `openidm`.`internaluser` (`objectid`, `rev`, `pwd`, `roles`) VALUES ('anonymous', '0', 'anonymous', 'openidm-reg');

COMMIT;

-- -------------------------------------------
-- openidm database user
-- ------------------------------------------
GRANT ALL PRIVILEGES on openidm.* TO openidm IDENTIFIED BY 'openidm';
GRANT ALL PRIVILEGES on openidm.* TO openidm@'%' IDENTIFIED BY 'openidm';
GRANT ALL PRIVILEGES on openidm.* TO openidm@localhost IDENTIFIED BY 'openidm';
