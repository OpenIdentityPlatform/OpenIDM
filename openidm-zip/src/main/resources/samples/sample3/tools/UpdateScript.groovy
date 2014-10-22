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
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

import java.sql.Connection

def operation = operation as OperationType
def updateAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def configuration = configuration as ScriptedSQLConfiguration
def connection = connection as Connection
def id = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = uid as Uid
def ORG = new ObjectClass("organization")

log.info("Entering " + operation + " Script");
def sql = new Sql(connection);

switch (operation) {
    case OperationType.UPDATE:
        switch (objectClass) {
            case ObjectClass.ACCOUNT:
                sql.executeUpdate("""
                        UPDATE 
                            users
                        SET 
                            fullname = ?,
                            firstname = ?,
                            lastname = ?,
                            email = ?,
                            organization = ?,
                            password = coalesce(sha1(?), password),
                            timestamp = now()
                        WHERE 
                            id = ?
                        """,
                        [
                                updateAttributes.findString("fullname"),
                                updateAttributes.findString("firstname"),
                                updateAttributes.findString("lastname"),
                                updateAttributes.findString("email"),
                                updateAttributes.findString("organization"),
                                updateAttributes.findString("password"),
                                uid.uidValue
                        ]
                );
                sql.executeUpdate("DELETE FROM car WHERE users_id=?",
                    [
                            uid.uidValue
                    ]
                );
                updateAttributes.findMap("cars").each {
                    sql.executeInsert(
                            "INSERT INTO car (users_id,year,make,model) VALUES (?,?,?,?)",
                            [
                                    uid.uidValue,
                                    it.year,
                                    it.make,
                                    it.model
                            ]
                    )
                };

                break

            case ObjectClass.GROUP:
                sql.executeUpdate("""
                        UPDATE 
                            groups
                        SET
                            description = ?,
                            name = ?,
                            gid = ?,
                            timestamp = now()
                        WHERE 
                            id = ?
                        """,
                        [
                                updateAttributes.findString("description"),
                                updateAttributes.findString("name"),
                                updateAttributes.findString("gid"),
                                uid.uidValue
                        ]
                );
                sql.executeUpdate("DELETE FROM groups_users WHERE groups_id=?",
                        [
                                uid.uidValue
                        ]
                );
                updateAttributes.findMap("users").each {
                    sql.executeInsert(
                            "INSERT INTO groups_users (users_id,groups_id) SELECT id,? FROM users WHERE uid=?",
                            [
                                    it.uid,
                                    uid.uidValue
                            ]
                    )
                }

                break

            case ORG:
                sql.executeUpdate("""
                        UPDATE 
                            Organizations
                        SET 
                            description = ?,
                            timestamp = now()
                        WHERE 
                            id = ?
                        """,
                        [
                                updateAttributes.findString("description"),
                                uid.uidValue
                        ]
                );
                break

            default:
                uid
        }
        return uid
    case OperationType.ADD_ATTRIBUTE_VALUES:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
    case OperationType.REMOVE_ATTRIBUTE_VALUES:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
    default:
        throw new ConnectorException("UpdateScript can not handle operation:" + operation.name())
}