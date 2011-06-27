CREATE SCHEMA IF NOT EXISTS `openidm` DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;
USE `openidm`;

CREATE  TABLE IF NOT EXISTS `openidm`.`genericobjects` (
  `type` VARCHAR(217) NOT NULL ,
  `openidmid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL DEFAULT NULL ,
  PRIMARY KEY (`type`, `openidmid`) )
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `openidm`.`genericobjectproperties` (
  `genericobjects_type` VARCHAR(217) ,
  `genericobjects_openidmid` VARCHAR(38) ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) DEFAULT NULL ,
  `propvalue` TEXT DEFAULT NULL ,
  INDEX `fk_genericobjects` (`genericobjects_type`, `genericobjects_openidmid`) ,
  CONSTRAINT `fk_genericobjects`
    FOREIGN KEY (`genericobjects_type`, `genericobjects_openidmid`)
    REFERENCES `openidm`.`genericobjects` (`type`, `openidmid`)
    ON DELETE CASCADE)
ENGINE = InnoDB;

CREATE  TABLE IF NOT EXISTS `openidm`.`managedobjects` (
  `type` VARCHAR(217) NOT NULL ,
  `openidmid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL DEFAULT NULL ,
  PRIMARY KEY (`type`, `openidmid`) )
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `openidm`.`managedobjectproperties` (
  `managedobjects_type` VARCHAR(217) ,
  `managedobjects_openidmid` VARCHAR(38) ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) DEFAULT NULL ,
  `propvalue` TEXT DEFAULT NULL ,
  INDEX `fk_managedobjects` (`managedobjects_type`, `managedobjects_openidmid`) ,
  CONSTRAINT `fk_managedobjects`
    FOREIGN KEY (`managedobjects_type`, `managedobjects_openidmid`)
    REFERENCES `openidm`.`managedobjects` (`type`, `openidmid`)
    ON DELETE CASCADE)
ENGINE = InnoDB;

CREATE  TABLE IF NOT EXISTS `openidm`.`configobjects` (
  `type` VARCHAR(217) NOT NULL ,
  `openidmid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `fullobject` MEDIUMTEXT NULL DEFAULT NULL ,
  PRIMARY KEY (`type`, `openidmid`) )
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `openidm`.`configobjectproperties` (
  `configobjects_type` VARCHAR(217) ,
  `configobjects_openidmid` VARCHAR(38) ,
  `propkey` VARCHAR(255) NOT NULL ,
  `proptype` VARCHAR(32) DEFAULT NULL ,
  `propvalue` TEXT DEFAULT NULL ,
  INDEX `fk_configobjects` (`configobjects_type`, `configobjects_openidmid`) ,
  CONSTRAINT `fk_configobjects`
    FOREIGN KEY (`configobjects_type`, `configobjects_openidmid`)
    REFERENCES `openidm`.`configobjects` (`type`, `openidmid`)
    ON DELETE CASCADE)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `openidm`.`links` (
    `type` VARCHAR(217), 
    `openidmid` VARCHAR(38), 
    `rev` VARCHAR(38), 
    `sourceid` VARCHAR(38), 
    `targetid` VARCHAR(38), 
    `reconid` VARCHAR(36),
    PRIMARY KEY (`type`, `openidmid`) ) 
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `openidm`.`auditrecon` (
    `openidmid` VARCHAR(38), 
    `reconid` VARCHAR(36), 
    `reconciling` VARCHAR(12),
    `sourceobjectid` VARCHAR(255),
    `targetobjectid` VARCHAR(255),
    `timestamp` VARCHAR(26),
    `situation` VARCHAR(24),
    `action` VARCHAR(24), 
    `status` VARCHAR(7), 
    `message` TEXT ) 
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `openidm`.`auditactivity` (
    `openidmid` VARCHAR(38),  
    `timestamp` VARCHAR(26),
    `action` VARCHAR(24),
    `message` TEXT,
    `objectid` VARCHAR(255),
    `objectrev` VARCHAR(38),
    `rootactionid` VARCHAR(512),
    `parentactionid` VARCHAR(512),
    `requester` TEXT,
    `approver` TEXT,
    `before` MEDIUMTEXT,
    `after` MEDIUMTEXT,
    `status` VARCHAR(7) ) 
ENGINE = InnoDB;

