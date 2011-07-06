SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `openidm` DEFAULT CHARACTER SET utf8 ;
USE `openidm` ;

-- -----------------------------------------------------
-- Table `openidm`.`objectypes`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`objectypes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `objectype` VARCHAR(255) NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `idx_objecttypes_objecttype` (`objectype` ASC) )
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
    REFERENCES `openidm`.`objectypes` (`id` )
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
  PRIMARY KEY (`genericobjects_id`, `propkey`) ,
  INDEX `fk_genericobjectproperties_genericobjects` (`genericobjects_id` ASC) ,
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
    REFERENCES `openidm`.`objectypes` (`id` )
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
  PRIMARY KEY (`managedobjects_id`, `propkey`) ,
  INDEX `fk_managedobjectproperties_managedobjects` (`managedobjects_id` ASC) ,
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
    REFERENCES `openidm`.`objectypes` (`id` )
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
  PRIMARY KEY (`configobjects_id`, `propkey`) ,
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
  `objecttypes_id` BIGINT UNSIGNED NOT NULL ,
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `sourceid` VARCHAR(255) NOT NULL ,
  `targetid` VARCHAR(255) NOT NULL ,
  `reconid` VARCHAR(36) NULL ,
  UNIQUE INDEX `idx_links_source` (`objecttypes_id` ASC, `objectid` ASC, `sourceid` ASC) ,
  INDEX `idx_links_target` (`objecttypes_id` ASC, `objectid` ASC, `targetid` ASC) ,
  PRIMARY KEY (`objecttypes_id`, `objectid`) ,
  INDEX `fk_links_objecttypes` (`objecttypes_id` ASC) ,
  CONSTRAINT `fk_links_objecttypes`
    FOREIGN KEY (`objecttypes_id` )
    REFERENCES `openidm`.`objectypes` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `openidm`.`auditrecon`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`auditrecon` (
  `objectid` VARCHAR(38) NOT NULL ,
  `reconid` VARCHAR(36) NULL ,
  `reconciling` VARCHAR(12) NULL ,
  `sourceobjectid` VARCHAR(511) NULL ,
  `targetobjectid` VARCHAR(511) NULL ,
  `activitydate` VARCHAR(26) NULL ,
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
  `activitydate` VARCHAR(26) NULL ,
  `activity` VARCHAR(24) NULL ,
  `message` TEXT NULL ,
  `subjectid` VARCHAR(511) NULL ,
  `subjectrev` VARCHAR(38) NULL ,
  `rootactionid` VARCHAR(512) NULL ,
  `parentactionid` VARCHAR(512) NULL ,
  `requester` TEXT NULL ,
  `approver` TEXT NULL ,
  `subjectbefore` MEDIUMTEXT NULL ,
  `subjectafter` MEDIUMTEXT NULL ,
  `status` VARCHAR(7) NULL ,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
