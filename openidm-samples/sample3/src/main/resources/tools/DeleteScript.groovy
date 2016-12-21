/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openicf.connectors.hrdb

import groovy.sql.Sql
import org.forgerock.json.resource.BadRequestException
import org.forgerock.openicf.connectors.hrdb.HRDBConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.Uid

import java.sql.Connection

/**
 * Built-in accessible objects
 **/

// OperationType is DELETE for this script
def operation = operation as OperationType

// The configuration class created specifically for this connector
def configuration = configuration as HRDBConfiguration

// Default logging facility
def log = log as Log

// The Uid of the object to be deleted
def uid = uid as Uid

// The objectClass of the object to be deleted, e.g. ACCOUNT or GROUP
def objectClass = objectClass as ObjectClass

/**
 * Script action - Customizable
 *
 * Delete an object in the external source.  Connectors that do not support this should
 * throw an UnsupportedOperationException.
 *
 * This script has no return value but should throw an exception is something goes wrong
 **/

/* Log something to demonstrate this script executed */

def connection = connection as Connection
def ORG = new ObjectClass("organization")

log.info("Delete script, operation = " + operation.toString());
def sql = new Sql(connection);

if (uid == null) {
    throw new BadRequestException("Uid is null");
}

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        sql.execute("DELETE FROM users where id= ?", [uid.uidValue])
        break

    case ObjectClass.GROUP:
        sql.execute("DELETE FROM groups where id= ?", [uid.uidValue])
        break

    case ORG:
        sql.execute("DELETE FROM organizations where id= ?", [uid.uidValue])
        break

    default:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
}
