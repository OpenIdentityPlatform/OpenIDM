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
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions

import java.sql.Connection

def operation = operation as OperationType
def configuration = configuration as ScriptedSQLConfiguration
def connection = connection as Connection
def log = log as Log
def objectClass = objectClass as ObjectClass
def ORG = new ObjectClass("organization")



log.info("Entering " + operation + " Script");
def sql = new Sql(connection);


switch (operation) {
    case OperationType.SYNC:
        def options = options as OperationOptions
        def token = token as Object

        def tstamp = null
        if (token != null) {
            tstamp = new java.sql.Timestamp(token)
        } else {
            def today = new Date()
            tstamp = new java.sql.Timestamp(today.time)
        }
        switch (objectClass) {
            case ObjectClass.ACCOUNT:
                sql.eachRow("select * from users where timestamp > ${tstamp}", { row ->
                    def cararray = []
                    def userid = row.id as Integer
                    sql.eachRow("SELECT * FROM car WHERE users_id = ${userid}", { car ->
                        if (car.year) {
                            cararray.add([
                                    year: car.year,
                                    make: car.make,
                                    model: car.model
                            ])
                        }
                    });
                    handler({
                        syncToken row.timestamp.getTime()
                        CREATE_OR_UPDATE()
                        object {
                            id row.uid
                            uid row.id as String
                            attribute 'uid', row.uid
                            attribute 'fullname', row.fullname
                            attribute 'firstname', row.firstname
                            attribute 'lastname', row.lastname
                            attribute 'email', row.email
                            attribute 'cars', cararray
                            attribute 'organization', row.organization
                        }
                    })
                })
                break

            case ObjectClass.GROUP:
                sql.eachRow("SELECT * FROM groups where timestamp > ${tstamp}", { row ->
                    def groupid = row.id as Integer
                    def userlist = []
                    sql.eachRow("SELECT users_id FROM groups_users WHERE groups_id = ${groupid}", { user ->
                        userlist.add([
                                uid: user.users_id
                        ])
                    })
                    handler({
                        syncToken row.timestamp.getTime()
                        CREATE_OR_UPDATE()
                        object {
                            id row.id as String
                            uid row.name
                            delegate.objectClass(ObjectClass.GROUP)
                            attribute 'gid', row.gid
                            attribute 'description', row.description
                            attribute 'users', userlist
                        }
                    })
                });
                break

            case ORG:
                sql.eachRow("SELECT * FROM organizations where timestamp > ${tstamp}", { row ->
                    handler({
                        syncToken row.timestamp.getTime()
                        CREATE_OR_UPDATE()
                        object {
                            id row.id as String
                            uid row.name
                            delegate.objectClass(ORG)
                            attribute 'description', row.description
                        }
                    })
                });
                break

            default:
                log.error("Sync script: objectClass " + objectClass + " is not handled by the Sync script")
                throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                        objectClass.objectClassValue + " is not supported.")
        }

        break;
    case OperationType.GET_LATEST_SYNC_TOKEN:

        switch (objectClass) {
            case ObjectClass.ACCOUNT:
                row = sql.firstRow("select max(timestamp) as timestamp from users")
                break;

            case ObjectClass.GROUP:
                row = sql.firstRow("select max(timestamp) as timestamp from groups")
                break;
            case ORG:
                row = sql.firstRow("select max(timestamp) as timestamp from organizations")
                break;

            default:
                throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                        objectClass.objectClassValue + " is not supported.")
        }

        log.ok("Get Latest Sync Token script: last token is: " + row["timestamp"])
        // We don't want to return the java.sql.Timestamp, it is not a supported data type
        // Get the 'long' version
        return row["timestamp"].getTime();

        break;
    default:
        throw new ConnectorException("SyncScript can not handle operation:" + operation.name())
}
