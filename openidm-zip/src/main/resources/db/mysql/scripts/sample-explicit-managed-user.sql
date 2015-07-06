
-- -----------------------------------------------------
-- Table `openidm`.`managed_user`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `openidm`.`managed_user` (
    `objectid` VARCHAR(38) NOT NULL,
    `rev` VARCHAR(38) NOT NULL,
    `userName` VARCHAR(255),
    `password` VARCHAR(255),
    `accountStatus` VARCHAR(255),
    `roles` VARCHAR(255),
    `authzRoles` VARCHAR(255),
    `lastPasswordSet` VARCHAR(255),
    `postalCode` VARCHAR(255),
    `stateProvince` VARCHAR(255),
    `passwordAttempts` VARCHAR(255),
    `lastPasswordAttempt` VARCHAR(255),
    `postalAddress` VARCHAR(255),
    `address2` VARCHAR(255),
    `country` VARCHAR(255),
    `city` VARCHAR(255),
    `givenName` VARCHAR(255),
    `sn` VARCHAR(255),
    `telephoneNumber` VARCHAR(255),
    `mail` VARCHAR(255),
    `siteImage` VARCHAR(255),
    `passPhrase` VARCHAR(255),
    `securityAnswer` VARCHAR(255),
    `securityQuestion` VARCHAR(255),
    `securityAnswerAttempts` VARCHAR(255),
    `lastSecurityAnswerAttempt` VARCHAR(255),
    `total` VARCHAR(255),
    `version` VARCHAR(255),
    PRIMARY KEY (`objectid`),
    UNIQUE INDEX `idx_managed_user_userName` (`userName` ASC),
    INDEX `idx_managed_user_givenName` (`givenName` ASC),
    INDEX `idx_managed_user_sn` (`sn` ASC),
    INDEX `idx_managed_user_mail` (`mail` ASC),
    INDEX `idx_managed_user_accountStatus` (`accountStatus` ASC),
    INDEX `idx_managed_user_roles` (`roles` ASC)
)
ENGINE = InnoDB;
