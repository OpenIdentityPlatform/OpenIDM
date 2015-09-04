/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 ForgeRock AS. All rights reserved.
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
 */


import org.forgerock.openicf.common.protobuf.CommonObjectMessages
import org.identityconnectors.framework.common.objects.Uid

import static org.forgerock.json.JsonValue.*;

import groovy.sql.Sql;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;

import java.sql.Connection;

// Parameters:
// The connector sends us the following:
// connection : SQL connection
// log: a handler to the Log facility
// objectClass: a String describing the Object class (recon, activity, access)
// id: The entry identifier (OpenICF "Name" atribute. Most often matches the uid)
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
// password: password string, clear text
// options: a handler to the OperationOptions Map

log.info("Entering Create Script for {0} with attributes {1}", objectClass, attributes);

def sql = new Sql(connection as Connection);
//Create must return UID. Let's return the id as the UID for now.

def auditauthentication = new ObjectClass("auditauthentication");
def auditrecon = new ObjectClass("auditrecon");
def auditactivity = new ObjectClass("auditactivity");
def auditaccess = new ObjectClass("auditaccess");
def auditsync = new ObjectClass("auditsync");

//convert attributes to map
Map<String, Attribute> attributeMap = new HashMap<String, Attribute>();
for (Attribute attribute : attributes) {
    attributeMap.put(attribute.getName(), attribute);
}

switch ( objectClass ) {
    case auditaccess:
        sql.execute("INSERT INTO auditaccess " +
                "(objectid, activity, activitydate, transactionid, eventname, server_ip, server_port, client_host, " +
                "client_ip, client_port, userid, principal, roles, auth_component, resource_uri, resource_protocol, " +
                "resource_method, resource_detail, http_method, http_path, http_querystring, http_headers, status, " +
                "elapsedtime" +
                ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                [
                    id,
                    attributeMap.get("activity")?.getValue()?.get(0),
                    attributeMap.get("activitydate")?.getValue()?.get(0),
                    attributeMap.get("transactionid")?.getValue()?.get(0),
                    attributeMap.get("eventname")?.getValue()?.get(0),
                    attributeMap.get("server")?.getValue()?.get(0)?.get("ip"),
                    attributeMap.get("server")?.getValue()?.get(0)?.get("port"),
                    attributeMap.get("client")?.getValue()?.get(0)?.get("host"),
                    attributeMap.get("client")?.getValue()?.get(0)?.get("ip"),
                    attributeMap.get("client")?.getValue()?.get(0)?.get("port"),
                    attributeMap.get("authentication")?.getValue()?.get(0)?.get("id"),
                    attributeMap.get("authorizationid")?.getValue()?.get(0)?.get("id"),
                    attributeMap.get("authorizationid")?.getValue()?.get(0)?.get("roles") != null
                            ? json(attributeMap.get("authorizationid")?.getValue()?.get(0)?.get("roles")).toString()
                            : null,
                    attributeMap.get("authorizationid")?.getValue()?.get(0)?.get("component"),
                    attributeMap.get("resource")?.getValue()?.get(0)?.get("uri"),
                    attributeMap.get("resource")?.getValue()?.get(0)?.get("protocol"),
                    attributeMap.get("resource")?.getValue()?.get(0)?.get("method"),
                    attributeMap.get("resource")?.getValue()?.get(0)?.get("detail"),
                    attributeMap.get("http")?.getValue()?.get(0)?.get("method"),
                    attributeMap.get("http")?.getValue()?.get(0)?.get("path"),
                    attributeMap.get("http")?.getValue()?.get(0)?.get("querystring"),
                    attributeMap.get("http")?.getValue()?.get(0)?.get('headers') != null
                            ? json(attributeMap.get("http")?.getValue()?.get(0)?.get('headers')).toString()
                            : null,
                    attributeMap.get("response")?.getValue()?.get(0)?.get("status"),
                    attributeMap.get("response")?.getValue()?.get(0)?.get("elapsedTime"),
                ]);
        break;

    case auditauthentication:
        sql.execute("INSERT INTO auditauthentication " +
                "(objectid, transactionid, activitydate, userid, eventname, result, principals, context, sessionid, " +
                "entries" +
                ") values (?,?,?,?,?,?,?,?,?,?)",
                [
                    id,
                    attributeMap.get("transactionid")?.getValue()?.get(0),
                    attributeMap.get("activitydate")?.getValue()?.get(0),
                    attributeMap.get("authentication")?.getValue()?.get(0)?.get("id"),
                    attributeMap.get("eventname")?.getValue()?.get(0),
                    attributeMap.get("result")?.getValue()?.get(0),
                    attributeMap.get("principal")?.getValue()?.get(0),
                    attributeMap.get("context")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("context")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("sessionid")?.getValue()?.get(0),
                    attributeMap.get("entries")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("entries")?.getValue()?.get(0)).toString()
                            : null
                ]);
        break;

    case auditactivity:
        sql.execute("INSERT INTO auditactivity (" +
                "objectid, activitydate, activity, transactionid, eventname, userid, runas, resource_uri, " +
                "resource_protocol, resource_method, resource_detail, subjectbefore, subjectafter, changedfields, " +
                "passwordchanged, subjectrev, message, activityobjectid, status" +
                ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                [
                    id, // objectid
                    attributeMap.get("activitydate")?.getValue()?.get(0),
                    attributeMap.get("activity")?.getValue()?.get(0),
                    attributeMap.get("transactionid")?.getValue()?.get(0),
                    attributeMap.get("eventname")?.getValue()?.get(0),
                    attributeMap.get("authentication")?.getValue()?.get(0)?.get("id"),
                    attributeMap.get("runas")?.getValue()?.get(0),
                    attributeMap.get("resourceOperation")?.getValue()?.get(0)?.get("uri"),
                    attributeMap.get("resourceOperation")?.getValue()?.get(0)?.get("protocol"),
                    attributeMap.get("resourceOperation")?.getValue()?.get(0)?.get("operation")?.get("method"),
                    attributeMap.get("resourceOperation")?.getValue()?.get(0)?.get("operation")?.get("detail"),
                    attributeMap.get("subjectbefore")?.getValue()?.get(0),
                    attributeMap.get("subjectafter")?.getValue()?.get(0),
                    attributeMap.get("changedfields")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("changedfields")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("passwordchanged")?.getValue()?.get(0)?.toString(),
                    attributeMap.get("subjectrev")?.getValue()?.get(0),
                    attributeMap.get("message")?.getValue()?.get(0),
                    attributeMap.get("activityobjectid")?.getValue()?.get(0),
                    attributeMap.get("status")?.getValue()?.get(0)
                ]);
        break;

    case auditrecon:
        sql.execute("INSERT INTO auditrecon (" +
                "objectid, transactionid, activitydate, eventname, userid, activity, exceptiondetail, linkqualifier, " +
                "mapping, message, messagedetail, situation, sourceobjectid, status, targetobjectid, reconciling, " +
                "ambiguoustargetobjectids, reconaction, entrytype, reconid" +
                ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                [
                    id, // objectid
                    attributeMap.get("transactionid")?.getValue()?.get(0),
                    attributeMap.get("activitydate")?.getValue()?.get(0),
                    attributeMap.get("eventname")?.getValue()?.get(0),
                    attributeMap.get("authentication")?.getValue()?.get(0)?.get("id"),
                    attributeMap.get("activity")?.getValue()?.get(0),
                    attributeMap.get("exceptiondetail")?.getValue()?.get(0),
                    attributeMap.get("linkqualifier")?.getValue()?.get(0),
                    attributeMap.get("mapping")?.getValue()?.get(0),
                    attributeMap.get("message")?.getValue()?.get(0),
                    attributeMap.get("messagedetail")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("messagedetail")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("situation")?.getValue()?.get(0),
                    attributeMap.get("sourceobjectid")?.getValue()?.get(0),
                    attributeMap.get("status")?.getValue()?.get(0),
                    attributeMap.get("targetobjectid")?.getValue()?.get(0),
                    attributeMap.get("reconciling")?.getValue()?.get(0),
                    attributeMap.get("ambiguoustargetobjectids")?.getValue()?.get(0),
                    attributeMap.get("reconaction")?.getValue()?.get(0),
                    attributeMap.get("entrytype")?.getValue()?.get(0),
                    attributeMap.get("reconid")?.getValue()?.get(0)
                ]);
        break;

    case auditsync:
        sql.execute("INSERT INTO auditsync (" +
                "objectid, transactionid, activitydate, eventname, userid, activity, exceptiondetail, " +
                "linkqualifier, mapping, message, messagedetail, situation, sourceobjectid, status, targetobjectid" +
                ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                [
                    id,
                    attributeMap.get("transactionid")?.getValue()?.get(0),
                    attributeMap.get("activitydate")?.getValue()?.get(0),
                    attributeMap.get("eventname")?.getValue()?.get(0),
                    attributeMap.get("authentication")?.getValue()?.get(0)?.get("id"),
                    attributeMap.get("activity")?.getValue()?.get(0),
                    attributeMap.get("exceptiondetail")?.getValue()?.get(0),
                    attributeMap.get("linkqualifier")?.getValue()?.get(0),
                    attributeMap.get("mapping")?.getValue()?.get(0),
                    attributeMap.get("message")?.getValue()?.get(0),
                    attributeMap.get("messagedetail")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("messagedetail")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("situation")?.getValue()?.get(0),
                    attributeMap.get("sourceobjectid")?.getValue()?.get(0),
                    attributeMap.get("status")?.getValue()?.get(0),
                    attributeMap.get("targetobjectid")?.getValue()?.get(0)
                ]);
        break;

    default:
        log.warn("Didn't match objectClass " + objectClass);
}

return id;
