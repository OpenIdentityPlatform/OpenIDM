/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Version 1.0
 * Author ForgeRock
 */
package org.forgerock.openicf.connectors.hrdb

import groovy.sql.Sql
import org.forgerock.openicf.connectors.hrdb.HRDBConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.common.security.GuardedString
import org.identityconnectors.common.security.SecurityUtil
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException

import java.sql.Connection

/**
 * Built-in accessible objects
 **/

// OperationType is AUTHENTICATION for this script
def operation = operation as OperationType

// The configuration class created specifically for this connector
def configuration = configuration as HRDBConfiguration

// Default logging facility
def log = log as Log

/**
 * Script action - Customizable
 *
 * Must either return an int or String convertible to a Uid object or throw an exception
 **/

def authId = null;

/**
 * Params not generated custom connector tool
 **/

// username used in the sql query
def username = username as String

// password used in the sql query
def password = password as GuardedString;

// connection used for connecting to the SQL repo
def connection = connection as Connection

log.info("Entering " + operation + " Script");

// create connection to SQL
def sql = new Sql(connection);

// do select with provided, username/password
sql.eachRow("SELECT id FROM users WHERE uid = ? AND password = sha1(?)", [username, SecurityUtil.decrypt(password)]) {
    authId = String.valueOf(it.id)
}

// check if user was returned..would imply authenticated
if (authId == null) {
    throw new InvalidPasswordException("Authentication Failed")
}

return authId