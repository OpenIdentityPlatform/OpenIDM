delimiter //

create procedure `openidm`.`byFieldValue` (t_schema varchar(255), t_name varchar(255), field varchar(255), val varchar(255), acceptable_fields varchar(255))
begin
    set @value = val;
    select find_in_set(field, acceptable_fields) into @ok;
    IF @ok != 0 THEN
        set @query = concat('select * from ', t_schema, '.', t_name ,' where ', field, ' = ?');
        prepare stmt from @query;
        execute stmt using @value;
    END IF;
end //


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
