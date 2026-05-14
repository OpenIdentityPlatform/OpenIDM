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
import org.forgerock.openicf.connectors.hrdb.HRDBConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

import java.sql.Connection

/**
 * Built-in accessible objects
 **/

// OperationType is CREATE for this script
def operation = operation as OperationType

// The configuration class created specifically for this connector
def configuration = configuration as HRDBConfiguration

// Default logging facility
def log = log as Log

// Set of attributes describing the object to be created
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)

// The Uid of the object to be created, usually null indicating the Uid should be generated
def uid = id as String

// The objectClass of the object to be created, e.g. ACCOUNT or GROUP
def objectClass = objectClass as ObjectClass

/**
 * Script action - Customizable
 *
 * Create a new object in the external source.  Connectors that do not support this should
 * throw an UnsupportedOperationException.
 *
 * This script should return a Uid object that represents the ID of the newly created object
 **/

/* Log something to demonstrate this script executed */
log.info("Create script, operation = " + operation.toString());

def ORG = new ObjectClass("organization")
def connection = connection as Connection
def sql = new Sql(connection);

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        def retUid
        def generatedKeys = sql.executeInsert(
                "INSERT INTO users (uid,password,firstname,lastname,fullname,email,organization) values (?,sha1(?),?,?,?,?,?)",
                [
                        uid,
                        createAttributes.hasAttribute("password") ? createAttributes.findString("firstname") : "",
                        createAttributes.hasAttribute("firstname") ? createAttributes.findString("firstname") : "",
                        createAttributes.hasAttribute("lastname") ? createAttributes.findString("lastname") : "",
                        createAttributes.hasAttribute("fullname") ? createAttributes.findString("fullname") : "",
                        createAttributes.hasAttribute("email") ? createAttributes.findString("email") : "",
                        createAttributes.hasAttribute("organization") ? createAttributes.findString("organization") : ""
                ]
        )
        retUid = new Uid(generatedKeys[0][0] as String)

        createAttributes.findList("cars").each {
            sql.executeInsert(
                    "INSERT INTO car (users_id,year,make,model) VALUES (?,?,?,?)",
                    [
                            generatedKeys[0][0] as Integer,
                            it.year,
                            it.make,
                            it.model
                    ]
            )
        }

        return retUid
        break

    case ObjectClass.GROUP:
        def retUid
        def generatedKeys = sql.executeInsert(
                "INSERT INTO groups (name,gid,description) values (?,?,?)",
                [
                        uid,
                        createAttributes.hasAttribute("gid") ? createAttributes.findString("gid") : "",
                        createAttributes.hasAttribute("description") ? createAttributes.findString("description") : "",
                ]
        )
        retUid = new Uid(generatedKeys[0][0] as String)

        createAttributes.findList("users").each {
            sql.executeInsert(
                    "INSERT INTO groups_users (users_id,groups_id) SELECT id,? FROM users WHERE uid=?",
                    [
                            generatedKeys[0][0] as Integer,
                            it.uid
                    ]
            )
        }

        return retUid
        break

    case ORG:
        def generatedKeys = sql.executeInsert(
                "INSERT INTO organizations (name ,description) values (?,?)",
                [
                        uid,
                        createAttributes.hasAttribute("description") ? createAttributes.findString("description") : ""
                ]
        )
        return new Uid(generatedKeys[0][0] as String)

        break

    default:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
}
