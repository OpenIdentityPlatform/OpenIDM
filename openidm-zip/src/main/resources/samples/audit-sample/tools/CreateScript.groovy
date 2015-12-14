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



import groovy.sql.Sql
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.ObjectClass

import java.sql.Connection

import static org.forgerock.json.JsonValue.json;

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
def auditconfig = new ObjectClass("auditconfig");

//convert attributes to map
Map<String, Attribute> attributeMap = new HashMap<String, Attribute>();
for (Attribute attribute : attributes) {
    attributeMap.put(attribute.getName(), attribute);
}

switch ( objectClass ) {
    case auditaccess:
        sql.execute("INSERT INTO auditaccess " +
                "(objectid, activitydate, eventname, transactionid, userid, trackingids, server_ip, server_port, " +
                "client_ip, client_port, request_protocol, request_operation, request_detail, " +
                "http_request_secure, http_request_method, http_request_path, http_request_queryparameters, " +
                "http_request_headers, http_request_cookies, http_response_headers, response_status, " +
                "response_statuscode, response_elapsedtime, response_elapsedtimeunits, roles" +
                ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                [
                    id,
                    attributeMap.get("activitydate")?.getValue()?.get(0),
                    attributeMap.get("eventname")?.getValue()?.get(0),
                    attributeMap.get("transactionid")?.getValue()?.get(0),
                    attributeMap.get("userid")?.getValue()?.get(0),
                    attributeMap.get("trackingids")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("trackingids")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("server")?.getValue()?.get(0)?.get("ip"),
                    attributeMap.get("server")?.getValue()?.get(0)?.get("port"),
                    attributeMap.get("client")?.getValue()?.get(0)?.get("ip"),
                    attributeMap.get("client")?.getValue()?.get(0)?.get("port"),
                    attributeMap.get("request")?.getValue()?.get(0)?.get("protocol"),
                    attributeMap.get("request")?.getValue()?.get(0)?.get("operation"),
                    attributeMap.get("request")?.getValue()?.get(0)?.get("detail") != null
                            ? json(attributeMap.get("request")?.getValue()?.get(0)?.get("detail")).toString()
                            : null,
                    attributeMap.get("http")?.getValue()?.get(0)?.get("request")?.get("secure"),
                    attributeMap.get("http")?.getValue()?.get(0)?.get("request")?.get("method"),
                    attributeMap.get("http")?.getValue()?.get(0)?.get("request")?.get("path"),
                    attributeMap.get("http")?.getValue()?.get(0)?.get("request")?.get("queryParameters") != null
                            ? json(attributeMap.get("http")?.getValue()?.get(0)?.get("request")?.get("queryParameters")).toString()
                            : null,
                    attributeMap.get("http")?.getValue()?.get(0)?.get("request")?.get("headers") != null
                            ? json(attributeMap.get("http")?.getValue()?.get(0)?.get("request")?.get("headers")).toString()
                            : null,
                    attributeMap.get("http")?.getValue()?.get(0)?.get("request")?.get("cookies") != null
                            ? json(attributeMap.get("http")?.getValue()?.get(0)?.get("request")?.get("cookies")).toString()
                            : null,
                    attributeMap.get("http")?.getValue()?.get(0)?.get("response")?.get("headers") != null
                            ? json(attributeMap.get("http")?.getValue()?.get(0)?.get("response")?.get("headers")).toString()
                            : null,
                    attributeMap.get("response")?.getValue()?.get(0)?.get("status"),
                    attributeMap.get("response")?.getValue()?.get(0)?.get("statusCode"),
                    attributeMap.get("response")?.getValue()?.get(0)?.get("elapsedTime"),
                    attributeMap.get("response")?.getValue()?.get(0)?.get('elapsedTimeUnits'),
                    attributeMap.get("roles")?.getValue() != null
                            ? json(attributeMap.get("roles")?.getValue()).toString()
                            : null,
                ]);
        break;

    case auditauthentication:
        sql.execute("INSERT INTO auditauthentication " +
                "(objectid, transactionid, activitydate, userid, eventname, result, principals, context, " +
                "entries, trackingids" +
                ") values (?,?,?,?,?,?,?,?,?,?)",
                [
                    id,
                    attributeMap.get("transactionid")?.getValue()?.get(0),
                    attributeMap.get("activitydate")?.getValue()?.get(0),
                    attributeMap.get("userid")?.getValue().get(0),
                    attributeMap.get("eventname")?.getValue()?.get(0),
                    attributeMap.get("result")?.getValue()?.get(0),
                    attributeMap.get("principal")?.getValue()?.get(0),
                    attributeMap.get("context")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("context")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("entries")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("entries")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("trackingids")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("trackingids")?.getValue()?.get(0)).toString()
                            : null
                ]);
        break;

    case auditactivity:
        sql.execute("INSERT INTO auditactivity (" +
                "objectid, activitydate, eventname, transactionid, userid, trackingids, runas, activityobjectid, " +
                "operation, subjectbefore, subjectafter, changedfields, " +
                "subjectrev, passwordchanged, message, status" +
                ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                [
                    id, // objectid
                    attributeMap.get("activitydate")?.getValue()?.get(0),
                    attributeMap.get("eventname")?.getValue()?.get(0),
                    attributeMap.get("transactionid")?.getValue()?.get(0),
                    attributeMap.get("userid")?.getValue()?.get(0),
                    attributeMap.get("trackingids")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("trackingids")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("runas")?.getValue()?.get(0),
                    attributeMap.get("activityobjectid")?.getValue()?.get(0),
                    attributeMap.get("operation")?.getValue()?.get(0),
                    attributeMap.get("subjectbefore")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("subjectbefore")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("subjectafter")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("subjectafter")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("changedfields")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("changedfields")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("subjectrev")?.getValue()?.get(0),
                    attributeMap.get("passwordchanged")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("passwordchanged")?.getValue()?.get(0)).toString()
                            : null,
                    attributeMap.get("message")?.getValue()?.get(0),
                    attributeMap.get("status")?.getValue()?.get(0)
                ]);
        break;

    case auditrecon:
        sql.execute("INSERT INTO auditrecon (" +
                "objectid, transactionid, activitydate, eventname, userid, trackingids, activity, exceptiondetail, linkqualifier, " +
                "mapping, message, messagedetail, situation, sourceobjectid, status, targetobjectid, reconciling, " +
                "ambiguoustargetobjectids, reconaction, entrytype, reconid" +
                ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                [
                    id, // objectid
                    attributeMap.get("transactionid")?.getValue()?.get(0),
                    attributeMap.get("activitydate")?.getValue()?.get(0),
                    attributeMap.get("eventname")?.getValue()?.get(0),
                    attributeMap.get("userid")?.getValue()?.get(0),
                    attributeMap.get("trackingids")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("trackingids")?.getValue()?.get(0)).toString()
                            : null,
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
                "objectid, transactionid, activitydate, eventname, userid, trackingids, activity, exceptiondetail, " +
                "linkqualifier, mapping, message, messagedetail, situation, sourceobjectid, status, targetobjectid" +
                ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                [
                    id,
                    attributeMap.get("transactionid")?.getValue()?.get(0),
                    attributeMap.get("activitydate")?.getValue()?.get(0),
                    attributeMap.get("eventname")?.getValue()?.get(0),
                    attributeMap.get("userid")?.getValue()?.get(0),
                    attributeMap.get("trackingids")?.getValue()?.get(0) != null
                            ? json(attributeMap.get("trackingids")?.getValue()?.get(0)).toString()
                            : null,
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

    case auditconfig:
        sql.execute("INSERT INTO auditconfig (" +
                "objectid, activitydate, eventname, transactionid, userid, trackingids, runas, configobjectid, " +
                "operation, beforeObject, afterObject, changedfields, " +
                "rev" +
                ") values (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                [
                        id, // objectid
                        attributeMap.get("activitydate")?.getValue()?.get(0),
                        attributeMap.get("eventname")?.getValue()?.get(0),
                        attributeMap.get("transactionid")?.getValue()?.get(0),
                        attributeMap.get("userid")?.getValue()?.get(0),
                        attributeMap.get("trackingids")?.getValue()?.get(0) != null
                                ? json(attributeMap.get("trackingids")?.getValue()?.get(0)).toString()
                                : null,
                        attributeMap.get("runas")?.getValue()?.get(0),
                        attributeMap.get("configobjectid")?.getValue()?.get(0),
                        attributeMap.get("operation")?.getValue()?.get(0),
                        attributeMap.get("beforeObject")?.getValue()?.get(0) != null
                                ? json(attributeMap.get("beforeObject")?.getValue()?.get(0)).toString()
                                : null,
                        attributeMap.get("afterObject")?.getValue()?.get(0) != null
                                ? json(attributeMap.get("afterObject")?.getValue()?.get(0)).toString()
                                : null,
                        attributeMap.get("changedfields")?.getValue()?.get(0) != null
                                ? json(attributeMap.get("changedfields")?.getValue()?.get(0)).toString()
                                : null,
                        attributeMap.get("rev")?.getValue()?.get(0)
                ]);
        break;

    default:
        log.warn("Didn't match objectClass " + objectClass);
}

return id;
