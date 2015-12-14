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
import org.forgerock.openicf.misc.scriptedcommon.MapFilterVisitor
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.filter.Filter

import java.sql.Connection

import static org.forgerock.json.JsonValue.field

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
def auditconfig = new ObjectClass("auditconfig")

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
                    attribute 'activitydate', dataCollector.activitydate
                    attribute 'transactionid', dataCollector.transactionid
                    attribute 'eventname', dataCollector.eventname
                    attribute 'trackingids', JsonValueUtil.fromJsonString(dataCollector.trackingids)?.getObject()
                    attribute 'server',
                            JsonValueUtil.fromEntries(
                                field("ip", dataCollector.server_ip),
                                field("port", dataCollector.server_port)
                            )?.getObject()
                    attribute 'client',
                            JsonValueUtil.fromEntries(
                                field("ip", dataCollector.client_ip),
                                field("port", dataCollector.client_port)
                            )?.getObject()
                    attribute 'userid', dataCollector.userid
                    attribute 'request',
                            JsonValueUtil.fromEntries(
                                field("protocol", dataCollector.request_protocol),
                                field("operation", dataCollector.request_operation),
                                field("detail", JsonValueUtil.fromJsonString(dataCollector.request_detail)?.getObject())
                            )?.getObject()
                    attribute 'http',
                            JsonValueUtil.fromEntries(
                                    field("request", JsonValueUtil.fromEntries(
                                            field("secure", JsonValueUtil.booleanFromString(dataCollector.http_request_secure)),
                                            field("method", dataCollector.http_request_method),
                                            field("path", dataCollector.http_request_path),
                                            field("queryParameters", JsonValueUtil.fromJsonString(dataCollector.http_request_queryparameters)?.getObject()),
                                            field("headers", JsonValueUtil.fromJsonString(dataCollector.http_request_headers)?.getObject()),
                                            field("cookies", JsonValueUtil.fromJsonString(dataCollector.http_request_cookies)?.getObject())
                                    )?.getObject()),
                                    field("response", JsonValueUtil.fromEntries(
                                            field("headers", JsonValueUtil.fromJsonString(dataCollector.http_response_headers)?.getObject())
                                    )?.getObject()),
                            )?.getObject()
                    attribute 'response',
                            JsonValueUtil.fromEntries(
                                field("status", dataCollector.response_status),
                                field("statusCode", dataCollector.response_statuscode),
                                field("elapsedTime", dataCollector.response_elapsedtime),
                                field("elaspesTimeUnits", dataCollector.response_elapsedtimeunits)
                            )?.getObject()
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
                    activitydate : row.activitydate,
                    transactionid : row.transactionid,
                    eventname : row.eventname,
                    userid : row.userid,
                    trackingids : row.trackingids,
                    server_ip : row.server_ip,
                    server_port : row.server_port,
                    client_ip : row.client_ip,
                    client_port : row.client_port,
                    request_protocol : row.request_protocol,
                    request_operation : row.request_operation,
                    request_detail : row.request_detail,
                    http_request_secure : row.http_request_secure,
                    http_request_method : row.http_request_method,
                    http_request_path : row.http_request_path,
                    http_request_queryparameters : row.http_request_queryparameters,
                    http_request_headers : row.http_request_headers,
                    http_request_cookies : row.http_request_cookies,
                    http_response_headers : row.http_response_headers,
                    response_status : row.response_status,
                    response_statuscode : row.response_statuscode,
                    response_elapsedtime : row.response_elapsedtime,
                    response_elapsedtimeunits : row.response_elapsedtimeunits,
                    roles : row.roles
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
                    attribute 'userid', dataCollector.userid
                    attribute 'eventname', dataCollector.eventname
                    attribute 'trackingids', JsonValueUtil.fromJsonString(dataCollector.trackingids)?.getObject()
                    attribute 'result', dataCollector.result
                    attribute 'principal', dataCollector.principals
                    attribute 'context',
                            JsonValueUtil.fromJsonString(dataCollector.context)?.getObject()
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
                    trackingids : row.trackingids,
                    result : row.result,
                    context : row.context,
                    entries : row.entries,
                    principal : row.principals
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
                    attribute 'transactionid', dataCollector.transactionid
                    attribute 'eventname', dataCollector.eventname
                    attribute 'userid', dataCollector.userid
                    attribute 'runas', dataCollector.runas
                    attribute 'operation', dataCollector.operation
                    attribute 'subjectbefore', JsonValueUtil.fromJsonString(dataCollector.subjectbefore)?.getObject()
                    attribute 'subjectafter', JsonValueUtil.fromJsonString(dataCollector.subjectafter)?.getObject()
                    attribute 'changedfields',
                            JsonValueUtil.fromJsonString(dataCollector.changedfields)?.getObject()
                    attribute 'passwordchanged', JsonValueUtil.booleanFromString(dataCollector.passwordchanged)
                    attribute 'subjectrev', dataCollector.subjectrev
                    attribute 'message', dataCollector.message
                    attribute 'activityobjectid', dataCollector.activityobjectid
                    attribute 'status', dataCollector.status
                    attribute 'trackingids', JsonValueUtil.fromJsonString(dataCollector.trackingids)?.getObject()
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
                    transactionid : row.transactionid,
                    eventname : row.eventname,
                    userid : row.userid,
                    runas : row.runas,
                    operation : row.operation,
                    subjectbefore : row.subjectbefore,
                    subjectafter : row.subjectafter,
                    changedfields : row.changedfields,
                    passwordchanged : row.passwordchanged,
                    subjectrev : row.subjectrev,
                    message : row.message,
                    activityobjectid : row.activityobjectid,
                    status : row.status,
                    trackingids : row.trackingids
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
                    attribute 'userid', dataCollector.userid
                    attribute 'trackingids', JsonValueUtil.fromJsonString(dataCollector.trackingids)?.getObject()
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
                    trackingids : row.trackingids,
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
                    attribute 'userid', dataCollector.userid
                    attribute 'trackingids', JsonValueUtil.fromJsonString(dataCollector.trackingids)?.getObject()
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
                    trackingids : row.trackingids,
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

    case auditconfig:
        def dataCollector = [ uid: "" ]

        def handleCollectedData = {
            if (dataCollector.uid != "") {
                handler {
                    uid dataCollector.uid
                    id dataCollector.id
                    attribute 'activitydate', dataCollector.activitydate
                    attribute 'transactionid', dataCollector.transactionid
                    attribute 'eventname', dataCollector.eventname
                    attribute 'userid', dataCollector.userid
                    attribute 'runas', dataCollector.runas
                    attribute 'operation', dataCollector.operation
                    attribute 'beforeObject', JsonValueUtil.fromJsonString(dataCollector.subjectbefore)?.getObject()
                    attribute 'afterObject', JsonValueUtil.fromJsonString(dataCollector.subjectafter)?.getObject()
                    attribute 'changedfields',
                            JsonValueUtil.fromJsonString(dataCollector.changedfields)?.getObject()
                    attribute 'rev', dataCollector.subjectrev
                    attribute 'configobjectid', dataCollector.activityobjectid
                    attribute 'trackingids', JsonValueUtil.fromJsonString(dataCollector.trackingids)?.getObject()
                }
            }
        }

        def statement = """
            SELECT * FROM auditconfig ${where} ${orderBy} ${limit}
        """

        sql.eachRow(statement, whereParams, { row ->
            if (dataCollector.uid != row.objectid) {
                // process each row of the resultset
                handleCollectedData();

                dataCollector = [
                        id : row.id as String,
                        uid : row.objectid,
                        activitydate : row.activitydate,
                        transactionid : row.transactionid,
                        eventname : row.eventname,
                        userid : row.userid,
                        runas : row.runas,
                        operation : row.operation,
                        subjectbefore : row.beforeObject,
                        subjectafter : row.afterObject,
                        changedfields : row.changedfields,
                        subjectrev : row.rev,
                        activityobjectid : row.configobjectid,
                        trackingids : row.trackingids
                ]
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