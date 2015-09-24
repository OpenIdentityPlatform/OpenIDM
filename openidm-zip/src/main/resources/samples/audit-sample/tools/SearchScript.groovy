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

import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;

import org.forgerock.openicf.misc.scriptedcommon.MapFilterVisitor

def connection = connection as Connection
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def filter = filter as Filter
def log = log as Log

def sql = new Sql(connection);
def where = " WHERE 1=1 ";
def whereParams = []

// Define audit specific object classes
def auditauthentication = new ObjectClass("auditauthentication")
def auditrecon = new ObjectClass("auditrecon")
def auditactivity = new ObjectClass("auditactivity")
def auditaccess = new ObjectClass("auditaccess")
def auditsync = new ObjectClass("auditsync")

// Use the specified _pagedResultsCookie to query based on
// activitydate and auto-incrememnt id
if (options.pagedResultsCookie != null) {
    def cookieProps = options.pagedResultsCookie.split(",");
    assert cookieProps.size() == 2

    where =  " WHERE activitydate >= ? AND id > ? "
    whereParams = [ cookieProps[0], cookieProps[1].toInteger()]
}

// Default to order by id
def orderBy = ["id ASC"]

if (options.sortKeys != null && options.sortKeys.size() > 0) {
    // Translate sortKeys to actual SQL columns
    def orderByFieldMap = [
        "__UID__" : "objectid",
        "__NAME__": "objectid",
        "timestamp" : "activitydate"
    ]
    
    orderBy.clear()
    options.sortKeys.each {
        def key = it.toString();
        def field = key.substring(1,key.size())
        if (orderByFieldMap[field]) {
            field = orderByFieldMap[field]
        }
        if (key.substring(0,1) == "+") {
            orderBy.add(field + " ASC")
        } else {
            orderBy.add(field + " DESC")
        }
    }
}
orderBy = " ORDER BY " + orderBy.join(",")

// Set LIMIT based on the specified _pageSize
def limit = ""
if (options.pageSize != null) {
    limit = " LIMIT " + options.pageSize.toString()

    // Set OFFSET based on the specified _pagedResultsOffset
    if (options.pagedResultsOffset != null) {
        limit += " OFFSET " + options.pagedResultsOffset.toString()
    }
}

// Keep track of lastActivitydate and lastId so we can
// use it for the next request when performing paging
def lastActivitydate
def lastId

if (filter != null) {

    def query = filter.accept(MapFilterVisitor.INSTANCE, null)
    //Need to handle the __UID__ and __NAME__ in queries
    def fieldMap = [
            "__UID__" : "objectid",
            "__NAME__": "objectid"
    ]

    def whereTemplates = [
            CONTAINS          : '$left ${not ? "NOT " : ""}LIKE ?',
            ENDSWITH          : '$left ${not ? "NOT " : ""}LIKE ?',
            STARTSWITH        : '$left ${not ? "NOT " : ""}LIKE ?',
            EQUALS            : '${not ? "NOT " : ""} $left <=> ?',
            GREATERTHAN       : '$left ${not ? "<=" : ">"} ?',
            GREATERTHANOREQUAL: '$left ${not ? "<" : ">="} ?',
            LESSTHAN          : '$left ${not ? ">=" : "<"} ?',
            LESSTHANOREQUAL   : '$left ${not ? ">" : "<="} ?'
    ];

    // This closure function recurses through the (potentially complex) query 
    // object in order to build an equivalent SQL 'where' expression
    def queryParser
    queryParser = { queryObj ->

        if (queryObj.operation == "OR" || queryObj.operation == "AND") {
            return "(" + queryParser(queryObj.right) + " " + queryObj.operation + " " + queryParser(queryObj.left) + ")";
        } else {

            if (fieldMap[queryObj.get("left")]) {
                queryObj.put("left", fieldMap[queryObj.get("left")]);
            }

            def engine = new groovy.text.SimpleTemplateEngine()
            def wt = whereTemplates.get(queryObj.get("operation"))
            def binding = [left: queryObj.get("left"), not: queryObj.get("not")]
            def template = engine.createTemplate(wt).make(binding)

            if (queryObj.get("operation") == "CONTAINS") {
                whereParams.push("%" + queryObj.get("right") + "%")
            } else if (queryObj.get("operation") == "ENDSWITH") {
                whereParams.push("%" + queryObj.get("right"))
            } else if (queryObj.get("operation") == "STARTSWITH") {
                whereParams.push(queryObj.get("right") + "%")
            } else {
                whereParams.push(queryObj.get("right"))
            }
            return template.toString()
        }
    }

    where = where + " AND "+ queryParser(query)
    log.ok("Search WHERE clause is: " + where)
}

log.info("Search: ObjectClass {0}, where {1}", objectClass, where);
def resultCount = 0
switch ( objectClass ) {
    case auditaccess:
        def dataCollector = [ uid: "" ]

        def handleCollectedData = {
            if (dataCollector.uid != "") {
                handler {
                    uid dataCollector.uid
                    id dataCollector.id
                    attribute 'activity', dataCollector.activity
                    attribute 'activitydate', dataCollector.activitydate
                    attribute 'transactionid', dataCollector.transactionid
                    attribute 'eventname', dataCollector.eventname
                    attribute 'server',
                            JsonValueUtil.fromEntries(
                                field("ip", dataCollector.server_ip),
                                field("port", dataCollector.server_port)
                            ).getObject()
                    attribute 'client',
                            JsonValueUtil.fromEntries(
                                field("host", dataCollector.client_host),
                                field("ip", dataCollector.client_ip),
                                field("port", dataCollector.client_port)
                            ).getObject()
                    attribute 'authentication',
                            JsonValueUtil.fromEntries(
                                field("id", dataCollector.userid)
                            ).getObject()
                    attribute 'authorization',
                            JsonValueUtil.fromEntries(
                                field("id", dataCollector.principal),
                                field("roles",
                                        JsonValueUtil.fromJsonString(dataCollector.roles)?.getObject()),
                                field("component", dataCollector.auth_component)
                            ).getObject()
                    attribute 'resource',
                            JsonValueUtil.fromEntries(
                                field("uri", dataCollector.resource_uri),
                                field("protocol", dataCollector.resource_protocol),
                                field("method", dataCollector.resource_method),
                                field("detail", dataCollector.resource_detail)
                            ).getObject()
                    attribute 'http',
                            JsonValueUtil.fromEntries(
                                field("method", dataCollector.http_method),
                                field("path", dataCollector.http_path),
                                field("querystring", dataCollector.http_querystring),
                                field("headers",
                                        JsonValueUtil.fromJsonString(dataCollector.http_headers)?.getObject())
                            ).getObject()
                    attribute 'response',
                            JsonValueUtil.fromEntries(
                                field("status", dataCollector.status),
                                field("elapsedTime", dataCollector.elapsedtime)
                            ).getObject()
                }
            }
        }

        def statement = """
            SELECT * FROM auditaccess ${where} ${orderBy} ${limit}
        """

        sql.eachRow(statement, whereParams, { row ->
            if (dataCollector.uid != row.objectid) {
                // process each row of the resultset
                handleCollectedData();

                dataCollector = [
                    id : row.id as String,
                    uid : row.objectid,
                    activity : row.activity,
                    activitydate : row.activitydate,
                    transactionid : row.transactionid,
                    eventname : row.eventname,
                    server_ip : row.server_ip,
                    server_port : row.server_port,
                    client_host : row.client_host,
                    client_ip : row.client_ip,
                    client_port : row.client_port,
                    userid : row.userid,
                    principal : row.principal,
                    roles : row.roles,
                    auth_component : row.auth_component,
                    resource_uri : row.resource_uri,
                    protocol : row.resource_protocol,
                    resource_method : row.resource_method,
                    resource_detail : row.resource_detail,
                    http_method : row.http_method,
                    http_path : row.http_path,
                    http_querystring : row.http_querystring,
                    http_headers : row.http_headers,
                    status : row.status,
                    elapsedtime : row.elapsedtime
                ]
            }

            lastActivitydate = row.activitydate
            lastId = row.id
            resultCount++
        });

        handleCollectedData();

        break

    case auditauthentication:
        def dataCollector = [ uid: "" ]

        def handleCollectedData = {
            if (dataCollector.uid != "") {
                handler {
                    uid dataCollector.uid
                    id dataCollector.id
                    attribute 'transactionid', dataCollector.transactionid
                    attribute 'activitydate', dataCollector.activitydate
                    attribute 'authentication',
                            JsonValueUtil.fromEntries(
                                field("id", dataCollector.userid)
                            ).getObject()
                    attribute 'eventname', dataCollector.eventname
                    attribute 'result', dataCollector.result
                    attribute 'principal', dataCollector.principals
                    attribute 'context',
                            JsonValueUtil.fromJsonString(dataCollector.context)?.getObject()
                    attribute 'sessionid', dataCollector.sessionid
                    attribute 'entries',
                            JsonValueUtil.fromJsonString(dataCollector.entries)?.getObject()
                }
            }
        }

        def statement = """
            SELECT * FROM auditauthentication ${where} ${orderBy} ${limit}
        """

        sql.eachRow(statement, whereParams, { row ->
            if (dataCollector.uid != row.objectid) {
                // process each row of the resultset
                handleCollectedData();

                dataCollector = [
                    id : row.id as String,
                    uid : row.objectid,
                    transactionid : row.transactionid,
                    activitydate : row.activitydate,
                    userid : row.userid,
                    eventname : row.eventname,
                    result : row.result,
                    context : row.context,
                    sessionid : row.sessionid,
                    entries : row.entries
                ]
            }

            lastActivitydate = row.activitydate
            lastId = row.id
            resultCount++
        });

        handleCollectedData();

        break

    case auditactivity:
        def dataCollector = [ uid: "" ]

        def handleCollectedData = {
            if (dataCollector.uid != "") {
                handler {
                    uid dataCollector.uid
                    id dataCollector.id
                    attribute 'activitydate', dataCollector.activitydate
                    attribute 'activity', dataCollector.activity
                    attribute 'transactionid', dataCollector.transactionid
                    attribute 'eventname', dataCollector.eventname
                    attribute 'authentication',
                            JsonValueUtil.fromEntries(
                                field("id", dataCollector.userid)
                            ).getObject()
                    attribute 'runas', dataCollector.runas
                    attribute 'resourceOperation',
                            JsonValueUtil.fromEntries(
                                field("uri", dataCollector.resource_uri),
                                field("protocol", dataCollector.resource_protocol),
                                field("operation", JsonValueUtil.fromEntries(
                                    field("method", dataCollector.resource_method),
                                    field("detail", dataCollector.resource_detail)
                                ).getObject())
                            ).getObject()
                    attribute 'subjectbefore', dataCollector.subjectbefore
                    attribute 'subjectafter', dataCollector.subjectafter
                    attribute 'changedfields',
                            JsonValueUtil.fromJsonString(dataCollector.changedfields)?.getObject()
                    attribute 'passwordchanged', JsonValueUtil.booleanFromString(dataCollector.passwordchanged)
                    attribute 'subjectrev', dataCollector.subjectrev
                    attribute 'message', dataCollector.message
                    attribute 'activityobjectid', dataCollector.activityobjectid
                    attribute 'status', dataCollector.status
                }
            }
        }

        def statement = """
            SELECT * FROM auditactivity ${where} ${orderBy} ${limit}
        """

        sql.eachRow(statement, whereParams, { row ->
            if (dataCollector.uid != row.objectid) {
                // process each row of the resultset
                handleCollectedData();

                dataCollector = [
                    id : row.id as String,
                    uid : row.objectid,
                    activitydate : row.activitydate,
                    activity : row.activity,
                    transactionid : row.transactionid,
                    eventname : row.eventname,
                    userid : row.userid,
                    runas : row.runas,
                    resource_uri : row.resource_uri,
                    resource_protocol : row.resource_protocol,
                    resource_method : row.resource_method,
                    resource_detail : row.resource_detail,
                    subjectbefore : row.subjectbefore,
                    subjectafter : row.subjectafter,
                    changedfields : row.changedfields,
                    passwordchanged : row.passwordchanged,
                    subjectrev : row.subjectrev,
                    message : row.message,
                    activityobjectid : row.activityobjectid,
                    status : row.status
                ]
            }

            lastActivitydate = row.activitydate
            lastId = row.id
            resultCount++
        });

        handleCollectedData();

        break

    case auditrecon:
        def dataCollector = [ uid: "" ]

        def handleCollectedData = {
            if (dataCollector.uid != "") {
                handler {
                    uid dataCollector.uid
                    id dataCollector.id
                    attribute 'transactionid', dataCollector.transactionid
                    attribute 'activitydate', dataCollector.activitydate
                    attribute 'eventname', dataCollector.eventname
                    attribute 'authentication',
                            JsonValueUtil.fromEntries(
                                field("id", dataCollector.userid)
                            ).getObject()
                    attribute 'activity', dataCollector.activity
                    attribute 'exceptiondetail', dataCollector.exceptiondetail
                    attribute 'linkqualifier', dataCollector.linkqualifier
                    attribute 'mapping', dataCollector.mapping
                    attribute 'message', dataCollector.message
                    attribute 'messagedetail',
                            JsonValueUtil.fromJsonString(dataCollector.messagedetail)?.getObject()
                    attribute 'situation', dataCollector.situation
                    attribute 'sourceobjectid', dataCollector.sourceobjectid
                    attribute 'status', dataCollector.status
                    attribute 'targetobjectid', dataCollector.targetobjectid
                    attribute 'reconciling', dataCollector.reconciling
                    attribute 'ambiguoustargetobjectids', dataCollector.ambiguoustargetobjectids
                    attribute 'reconaction', dataCollector.reconaction
                    attribute 'entrytype', dataCollector.entrytype
                    attribute 'reconid', dataCollector.reconid
                }
            }
        }

        def statement = """
            SELECT * FROM auditrecon ${where} ${orderBy} ${limit}
        """

        sql.eachRow(statement, whereParams, { row ->
            if (dataCollector.uid != row.objectid) {
                // process each row of the resultset
                handleCollectedData();

                dataCollector = [
                    id : row.id as String,
                    uid : row.objectid,
                    transactionid : row.transactionid,
                    activitydate : row.activitydate,
                    eventname : row.eventname,
                    userid : row.userid,
                    activity : row.activity,
                    exceptiondetail : row.exceptiondetail,
                    linkqualifier : row.linkqualifier,
                    mapping : row.mapping,
                    message : row.message,
                    messagedetail : row.messagedetail,
                    situation : row.situation,
                    sourceobjectid : row.sourceobjectid,
                    status : row.status,
                    targetobjectid : row.targetobjectid,
                    reconciling : row.reconciling,
                    ambiguoustargetobjectids : row.ambiguoustargetobjectids,
                    reconaction : row.reconaction,
                    entrytype : row.entrytype,
                    reconid : row.reconid
                ]
            }

            lastActivitydate = row.activitydate
            lastId = row.id
            resultCount++
        });

        handleCollectedData();
        break

    case auditsync:
        def dataCollector = [ uid: "" ]

        def handleCollectedData = {
            if (dataCollector.uid != "") {
                handler {
                    uid dataCollector.uid
                    id dataCollector.id
                    attribute 'transactionid', dataCollector.transactionid
                    attribute 'activitydate', dataCollector.activitydate
                    attribute 'eventname', dataCollector.eventname
                    attribute 'authentication',
                            JsonValueUtil.fromEntries(
                                field("id", dataCollector.userid)
                            ).getObject()
                    attribute 'activity', dataCollector.activity
                    attribute 'exceptiondetail', dataCollector.exceptiondetail
                    attribute 'linkqualifier', dataCollector.linkqualifier
                    attribute 'mapping', dataCollector.mapping
                    attribute 'message', dataCollector.message
                    attribute 'messagedetail',
                            JsonValueUtil.fromJsonString(dataCollector.messagedetail)?.getObject()
                    attribute 'situation', dataCollector.situation
                    attribute 'sourceobjectid', dataCollector.sourceobjectid
                    attribute 'status', dataCollector.status
                    attribute 'targetobjectid', dataCollector.targetobjectid
                }
            }
        }

        def statement = """
            SELECT * FROM auditsync ${where} ${orderBy} ${limit}
        """

        sql.eachRow(statement, whereParams, { row ->
            if (dataCollector.uid != row.objectid) {
                // process each row of the resultset
                handleCollectedData();

                dataCollector = [
                    id : row.id as String,
                    uid : row.objectid,
                    transactionid : row.transactionid,
                    activitydate : row.activitydate,
                    eventname : row.eventname,
                    userid : row.userid,
                    activity : row.activity,
                    exceptiondetail : row.exceptiondetail,
                    linkqualifier : row.linkqualifier,
                    mapping : row.mapping,
                    message : row.message,
                    messagedetail : row.messagedetail,
                    situation : row.situation,
                    sourceobjectid : row.sourceobjectid,
                    status : row.status,
                    targetobjectid : row.targetobjectid                ]
            }

            lastActivitydate = row.activitydate
            lastId = row.id
            resultCount++
        });

        handleCollectedData();
        break

    default:
        log.warn("Didn't match objectClass " + objectClass);
}

// If _pageSize has not been specified or if a custom _sortyKey has been
// specified return the default SearchResult object.  We do not support
// paging with arbitrary sort keys.
if (options.sortKeys || limit.toString().isEmpty() || resultCount < options.pageSize) {
    return new SearchResult();
}

return new SearchResult(lastActivitydate.toString() + "," + lastId.toString(), -1);