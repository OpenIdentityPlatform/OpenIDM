
-- -----------------------------------------------------
-- Table `openidm`.`managed_user`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`managed_user` (
    `objectid` VARCHAR(38) NOT NULL,
    `rev` VARCHAR(38) NOT NULL,
    `userName` VARCHAR(255),
    `password` VARCHAR(255),
    `accountStatus` VARCHAR(255),
    `postalCode` VARCHAR(255),
    `stateProvince` VARCHAR(255),
    `postalAddress` VARCHAR(255),
    `address2` VARCHAR(255),
    `country` VARCHAR(255),
    `city` VARCHAR(255),
    `givenName` VARCHAR(255),
    `sn` VARCHAR(255),
    `telephoneNumber` VARCHAR(255),
    `mail` VARCHAR(255),
    PRIMARY KEY (`objectid`),
    UNIQUE INDEX `idx_managed_user_userName` (`userName` ASC),
    INDEX `idx_managed_user_givenName` (`givenName` ASC),
    INDEX `idx_managed_user_sn` (`sn` ASC),
    INDEX `idx_managed_user_mail` (`mail` ASC),
    INDEX `idx_managed_user_accountStatus` (`accountStatus` ASC)
)
ENGINE = InnoDB;
