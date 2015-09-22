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

import static org.forgerock.json.JsonValue.*;

import groovy.sql.Sql

import java.sql.Connection;

import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;

def sql = new Sql(connection as Connection);
def where = "";
def filter = filter as Filter;

def auditauthentication = new ObjectClass("auditauthentication");
def auditrecon = new ObjectClass("auditrecon");
def auditactivity = new ObjectClass("auditactivity");
def auditaccess = new ObjectClass("auditaccess");
def auditsync = new ObjectClass("auditsync");

if (filter instanceof EqualsFilter && ((EqualsFilter) filter).getAttribute().is(Uid.NAME)) {
    def id = AttributeUtil.getStringValue(((EqualsFilter) filter).getAttribute());
    where = " WHERE objectid = '${id}'";
}

log.info("Search: ObjectClass {0}, where {1}", objectClass, where);

switch ( objectClass ) {
    case auditaccess:
        sql.eachRow("SELECT * FROM auditaccess" + where,
                { row ->
                    handler {
                        uid row.objectid
                        id row.objectid // Name is required by OpenICF connector
                        attribute 'activity', row.activity
                        attribute 'activitydate', row.activitydate
                        attribute 'transactionid', row.transactionid
                        attribute 'eventname', row.eventname
                        attribute 'server',
                                JsonValueUtil.fromEntries(
                                    field("ip", row.server_ip),
                                    field("port", row.server_port)
                                ).getObject()
                        attribute 'client',
                                JsonValueUtil.fromEntries(
                                    field("host", row.client_host),
                                    field("ip", row.client_ip),
                                    field("port", row.client_port)
                                ).getObject()
                        attribute 'authentication',
                                JsonValueUtil.fromEntries(
                                    field("id", row.userid)
                                ).getObject()
                        attribute 'authorization',
                                JsonValueUtil.fromEntries(
                                    field("id", row.principal),
                                    field("roles",
                                            JsonValueUtil.fromJsonString(row.roles)?.getObject()),
                                    field("component", row.auth_component)
                                ).getObject()
                        attribute 'resource',
                                JsonValueUtil.fromEntries(
                                    field("uri", row.resource_uri),
                                    field("protocol", row.resource_protocol),
                                    field("method", row.resource_method),
                                    field("detail", row.resource_detail)
                                ).getObject()
                        attribute 'http',
                                JsonValueUtil.fromEntries(
                                    field("method", row.http_method),
                                    field("path", row.http_path),
                                    field("querystring", row.http_querystring),
                                    field("headers",
                                            JsonValueUtil.fromJsonString(row.http_headers)?.getObject())
                                ).getObject()
                        attribute 'response',
                                JsonValueUtil.fromEntries(
                                    field("status", row.status),
                                    field("elapsedTime", row.elapsedtime)
                                ).getObject()
                    }
                }
        );
        break

    case auditauthentication:
        sql.eachRow("SELECT * FROM auditauthentication" + where,
                { row ->
                    handler {
                        uid row.objectid
                        id row.objectid
                        attribute 'transactionid', row.transactionid
                        attribute 'activitydate', row.activitydate
                        attribute 'authentication',
                                JsonValueUtil.fromEntries(
                                    field("id", row.userid)
                                ).getObject()
                        attribute 'eventname', row.eventname
                        attribute 'result', row.result
                        attribute 'principal', row.principals
                        attribute 'context',
                                JsonValueUtil.fromJsonString(row.context)?.getObject()
                        attribute 'sessionid', row.sessionid
                        attribute 'entries',
                                JsonValueUtil.fromJsonString(row.entries)?.getObject()
                    }
                }
        );
        break;

    case auditactivity:
        sql.eachRow("SELECT * FROM auditactivity" + where,
                { row ->
                    handler {
                        uid row.objectid
                        id row.objectid // Name is required by OpenICF connector
                        attribute 'activitydate', row.activitydate
                        attribute 'activity', row.activity
                        attribute 'transactionid', row.transactionid
                        attribute 'eventname', row.eventname
                        attribute 'authentication',
                                JsonValueUtil.fromEntries(
                                    field("id", row.userid)
                                ).getObject()
                        attribute 'runas', row.runas
                        attribute 'resourceOperation',
                                JsonValueUtil.fromEntries(
                                    field("uri", row.resource_uri),
                                    field("protocol", row.resource_protocol),
                                    field("operation", JsonValueUtil.fromEntries(
                                        field("method", row.resource_method),
                                        field("detail", row.resource_detail)
                                    ).getObject())
                                ).getObject()
                        attribute 'subjectbefore', row.subjectbefore
                        attribute 'subjectafter', row.subjectafter
                        attribute 'changedfields',
                                JsonValueUtil.fromJsonString(row.changedfields)?.getObject()
                        attribute 'passwordchanged', JsonValueUtil.booleanFromString(row.passwordchanged)
                        attribute 'subjectrev', row.subjectrev
                        attribute 'message', row.message
                        attribute 'activityobjectid', row.activityobjectid
                        attribute 'status', row.status
                    }
                }
        );
        break

    case auditrecon:
        sql.eachRow("SELECT * FROM auditrecon" + where,
                { row ->
                    handler {
                        uid row.objectid
                        id row.objectid // Name is required by OpenICF connector
                        attribute 'transactionid', row.transactionid
                        attribute 'activitydate', row.activitydate
                        attribute 'eventname', row.eventname
                        attribute 'authentication',
                                JsonValueUtil.fromEntries(
                                    field("id", row.userid)
                                ).getObject()
                        attribute 'activity', row.activity
                        attribute 'exceptiondetail', row.exceptiondetail
                        attribute 'linkqualifier', row.linkqualifier
                        attribute 'mapping', row.mapping
                        attribute 'message', row.message
                        attribute 'messagedetail',
                                JsonValueUtil.fromJsonString(row.messagedetail)?.getObject()
                        attribute 'situation', row.situation
                        attribute 'sourceobjectid', row.sourceobjectid
                        attribute 'status', row.status
                        attribute 'targetobjectid', row.targetobjectid
                        attribute 'reconciling', row.reconciling
                        attribute 'ambiguoustargetobjectids', row.ambiguoustargetobjectids
                        attribute 'reconaction', row.reconaction
                        attribute 'entrytype', row.entrytype
                        attribute 'reconid', row.reconid
                    }
                }
        );
        break

    case auditsync:
        sql.eachRow("SELECT * FROM auditsync" + where,
                { row ->
                    handler {
                        uid row.objectid
                        id row.objectid
                        attribute 'transactionid', row.transactionid
                        attribute 'activitydate', row.activitydate
                        attribute 'eventname', row.eventname
                        attribute 'authentication',
                                JsonValueUtil.fromEntries(
                                    field("id", row.userid)
                                ).getObject()
                        attribute 'activity', row.activity
                        attribute 'exceptiondetail', row.exceptiondetail
                        attribute 'linkqualifier', row.linkqualifier
                        attribute 'mapping', row.mapping
                        attribute 'message', row.message
                        attribute 'messagedetail',
                                JsonValueUtil.fromJsonString(row.messagedetail)?.getObject()
                        attribute 'situation', row.situation
                        attribute 'sourceobjectid', row.sourceobjectid
                        attribute 'status', row.status
                        attribute 'targetobjectid', row.targetobjectid
                    }
                }
        );
        break;

    default:
        log.warn("Didn't match objectClass " + objectClass);
}

return new SearchResult();
