SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `openidm` DEFAULT CHARACTER SET utf8 COLLATE utf8_bin ;
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
  `propvalue` VARCHAR(2000) NULL ,
  INDEX `fk_genericobjectproperties_genericobjects` (`genericobjects_id` ASC) ,
  INDEX `idx_genericobjectproperties_prop` (`propkey` ASC, `propvalue`(255) ASC) ,
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
  `propvalue` VARCHAR(2000) NULL ,
  INDEX `fk_managedobjectproperties_managedobjects` (`managedobjects_id` ASC) ,
  INDEX `idx_managedobjectproperties_prop` (`propkey` ASC, `propvalue`(255) ASC) ,
  CONSTRAINT `fk_managedobjectproperties_managedobjects`
    FOREIGN KEY (`managedobjects_id` )
    REFERENCES `openidm`.`managedobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
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
  `proptype` VARCHAR(255) NULL ,
  `propvalue` VARCHAR(2000) NULL ,
  INDEX `fk_configobjectproperties_configobjects` (`configobjects_id` ASC) ,
  INDEX `idx_configobjectproperties_prop` (`propkey` ASC, `propvalue`(255) ASC) ,
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
  `linkqualifier` VARCHAR(255) NOT NULL ,
  `firstid` VARCHAR(255) NOT NULL ,
  `secondid` VARCHAR(255) NOT NULL ,
  UNIQUE INDEX `idx_links_first` (`linktype` ASC, `linkqualifier` ASC, `firstid` ASC) ,
  UNIQUE INDEX `idx_links_second` (`linktype` ASC, `linkqualifier` ASC, `secondid` ASC) ,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`security`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`security` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `storestring` LONGTEXT NOT NULL ,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`securitykeys`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`securitykeys` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `keypair` LONGTEXT NOT NULL ,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`auditauthentication`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `openidm`.`auditauthentication` (
  `objectid` VARCHAR(38) NOT NULL ,
  `transactionid` VARCHAR(56) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `userid` VARCHAR(255) NULL ,
  `eventname` VARCHAR(50) NULL ,
  `result` VARCHAR(255) NULL ,
  `principals` TEXT ,
  `context` TEXT ,
  `sessionid` VARCHAR(255) ,
  `entries` TEXT ,
  PRIMARY KEY (`objectid`)
)
  ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`auditrecon`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`auditrecon` (
  `objectid` VARCHAR(38) NOT NULL ,
  `transactionid` VARCHAR(56) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `eventname` VARCHAR(50) NULL ,
  `userid` VARCHAR(255) NULL ,
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
  PRIMARY KEY (`objectid`) ,
  INDEX `idx_auditrecon_reconid` (`reconid` ASC),
  INDEX `idx_auditrecon_targetobjectid` (`targetobjectid`(28) ASC),
  INDEX `idx_auditrecon_sourceobjectid` (`sourceobjectid`(28) ASC),
  INDEX `idx_auditrecon_activitydate` (`activitydate` ASC),
  INDEX `idx_auditrecon_mapping` (`mapping`(255) ASC),
  INDEX `idx_auditrecon_entrytype` (`entrytype` ASC),
  INDEX `idx_auditrecon_situation` (`situation` ASC),
  INDEX `idx_auditrecon_status` (`status` ASC) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`auditsync`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`auditsync` (
  `objectid` VARCHAR(38) NOT NULL ,
  `transactionid` VARCHAR(56) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `eventname` VARCHAR(50) NULL ,
  `userid` VARCHAR(255) NULL ,
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
  PRIMARY KEY (`objectid`)
)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`auditconfig`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`auditconfig` (
  `objectid` VARCHAR(38) NOT NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(56) NOT NULL ,
  `eventname` VARCHAR(255) NULL ,
  `userid` VARCHAR(255) NULL ,
  `runas` VARCHAR(255) NULL ,
  `resource_uri` VARCHAR(255) NULL ,
  `resource_protocol` VARCHAR(10) NULL ,
  `resource_method` VARCHAR(10) NULL ,
  `resource_detail` VARCHAR(255) NULL ,
  `before` MEDIUMTEXT NULL ,
  `after` MEDIUMTEXT NULL ,
  `changedfields` VARCHAR(255) NULL ,
  `rev` VARCHAR(255) NULL,
  PRIMARY KEY (`objectid`) ,
  INDEX `idx_auditconfig_transactionid` (`transactionid` ASC)
)
  ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `openidm`.`auditactivity`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`auditactivity` (
  `objectid` VARCHAR(38) NOT NULL ,
  `activity` VARCHAR(24) NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(56) NOT NULL ,
  `eventname` VARCHAR(255) NULL ,
  `userid` VARCHAR(255) NULL ,
  `runas` VARCHAR(255) NULL ,
  `resource_uri` VARCHAR(255) NULL ,
  `resource_protocol` VARCHAR(10) NULL ,
  `resource_method` VARCHAR(10) NULL ,
  `resource_detail` VARCHAR(255) NULL ,
  `subjectbefore` MEDIUMTEXT NULL ,
  `subjectafter` MEDIUMTEXT NULL ,
  `changedfields` VARCHAR(255) NULL ,
  `passwordchanged` VARCHAR(5) NULL ,
  `subjectrev` VARCHAR(255) NULL ,
  `message` TEXT NULL,
  `activityobjectid` VARCHAR(255) ,
  `status` VARCHAR(20) ,
  PRIMARY KEY (`objectid`) ,
  INDEX `idx_auditactivity_transactionid` (`transactionid` ASC)
)
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
  `activity` VARCHAR(24) NULL ,
  `activitydate` VARCHAR(29) NOT NULL COMMENT 'Date format: 2011-09-09T14:58:17.654+02:00' ,
  `transactionid` VARCHAR(56) NOT NULL ,
  `eventname` VARCHAR(255) ,
  `server_ip` VARCHAR(40) ,
  `server_port` VARCHAR(5) ,
  `client_host` VARCHAR(255) ,
  `client_ip` VARCHAR(40) ,
  `client_port` VARCHAR(5) ,
  `userid` VARCHAR(255) NULL ,
  `principal` TEXT NULL ,
  `roles` VARCHAR(1024) NULL ,
  `auth_component` VARCHAR(255) NULL ,
  `resource_uri` VARCHAR(255) NULL ,
  `resource_protocol` VARCHAR(10) NULL ,
  `resource_method` VARCHAR(14) NULL ,
  `resource_detail` VARCHAR(255) NULL ,
  `http_method` VARCHAR(10) NULL ,
  `http_path` VARCHAR(255) NULL ,
  `http_querystring` TEXT NULL ,
  `http_headers` TEXT ,
  `status` VARCHAR(20) NULL ,
  `elapsedtime` VARCHAR(13) NULL ,
  PRIMARY KEY (`objectid`),
  INDEX `idx_auditaccess_status` (`status` ASC),
  INDEX `idx_auditaccess_principal` (`principal`(28) ASC) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`schedulerobjects`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`schedulerobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx-schedulerobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  INDEX `fk_schedulerobjects_objectypes` (`objecttypes_id` ASC) ,
  CONSTRAINT `fk_schedulerobjects_objectypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`schedulerobjectproperties`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`schedulerobjectproperties` (
  `schedulerobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) NULL ,
  `propvalue` VARCHAR(2000) NULL ,
  INDEX `fk_schedulerobjectproperties_schedulerobjects` (`schedulerobjects_id` ASC) ,
  INDEX `idx_schedulerobjectproperties_prop` (`propkey` ASC, `propvalue`(255) ASC) ,
  CONSTRAINT `fk_schedulerobjectproperties_schedulerobjects`
    FOREIGN KEY (`schedulerobjects_id` )
    REFERENCES `openidm`.`schedulerobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`uinotification`
-- -----------------------------------------------------
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
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`clusterobjects`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`clusterobjects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(255) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx-clusterobjects_object` (`objecttypes_id` ASC, `objectid` ASC) ,
  INDEX `fk_clusterobjects_objectypes` (`objecttypes_id` ASC) ,
  CONSTRAINT `fk_clusterobjects_objectypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objecttypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`clusterobjectproperties`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`clusterobjectproperties` (
  `clusterobjects_id` BIGINT UNSIGNED NOT NULL ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) NULL ,
  `propvalue` VARCHAR(2000) NULL ,
  INDEX `idx_clusterobjectproperties_prop` (`propkey` ASC, `propvalue`(255) ASC) ,
  INDEX `fk_clusterobjectproperties_clusterobjects` (`clusterobjects_id` ASC) ,
  CONSTRAINT `fk_clusterobjectproperties_clusterobjects`
    FOREIGN KEY (`clusterobjects_id` )
    REFERENCES `openidm`.`clusterobjects` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;



delimiter //

create procedure `openidm`.`getAllFromTable` (t_schema varchar(255), t_name varchar(255), order_by varchar(255), order_dir varchar(255), num_rows bigint, skip bigint, acceptable_order_by varchar(512))
begin
    set @num_rows = num_rows;
    set @skip = skip;
    select find_in_set(order_by, acceptable_order_by) into @order_by_ok;
    select find_in_set(order_dir, 'asc,desc') into @order_dir_ok;
    IF @order_by_ok != 0 && @order_dir_ok != 0 THEN
        set @query = concat('select * from ', t_schema, '.', t_name ,' order by ', order_by ,' ', order_dir ,' limit ? offset ?');
        prepare stmt from @query;
        execute stmt using @num_rows, @skip;
    END IF;
end //

delimiter ;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- -----------------------------------------------------
-- Data for table `openidm`.`internaluser`
-- -----------------------------------------------------
START TRANSACTION;
USE `openidm`;
INSERT INTO `openidm`.`internaluser` (`objectid`, `rev`, `pwd`, `roles`) VALUES ('openidm-admin', '0', 'openidm-admin', '["openidm-admin","openidm-authorized"]');
INSERT INTO `openidm`.`internaluser` (`objectid`, `rev`, `pwd`, `roles`) VALUES ('anonymous', '0', 'anonymous', '["openidm-reg"]');

COMMIT;

-- -------------------------------------------
-- openidm database user
-- ------------------------------------------
GRANT ALL PRIVILEGES on openidm.* TO openidm IDENTIFIED BY 'openidm';
GRANT ALL PRIVILEGES on openidm.* TO openidm@'%' IDENTIFIED BY 'openidm';
GRANT ALL PRIVILEGES on openidm.* TO openidm@localhost IDENTIFIED BY 'openidm';
