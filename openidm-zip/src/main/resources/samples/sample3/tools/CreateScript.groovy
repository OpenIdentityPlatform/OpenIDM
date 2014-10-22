/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
 * @author Gael Allioux <gael.allioux@forgerock.com>
 */


import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

import java.sql.Connection

def operation = operation as OperationType
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def configuration = configuration as ScriptedSQLConfiguration
def connection = connection as Connection
def uid = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def ORG = new ObjectClass("organization")



log.info("Entering " + operation + " Script");

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
