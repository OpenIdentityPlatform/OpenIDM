CREATE  TABLE IF NOT EXISTS `openidm`.`clusteredrecontargetids` (
  `objectid` VARCHAR(38) NOT NULL ,
  `rev` VARCHAR(38) NOT NULL ,
  `reconid` VARCHAR(255) NOT NULL ,
  `targetid` VARCHAR(255) NOT NULL ,
  INDEX `idx_clusteredrecontargetids_reconid` (`reconid` ASC) ,
  INDEX `idx_clusteredrecontargetids_reconid_targetid` (`reconid` ASC, `targetid` ASC) ,
  PRIMARY KEY (`objectid`) )
ENGINE = InnoDB;
