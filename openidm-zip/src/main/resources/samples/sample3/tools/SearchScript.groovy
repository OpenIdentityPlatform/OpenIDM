/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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
 * Version 1.0
 * Author ForgeRock
 */
package org.forgerock.openicf.connectors.hrdb

import groovy.sql.Sql
import org.forgerock.openicf.connectors.hrdb.HRDBConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Uid
import org.forgerock.openicf.misc.scriptedcommon.MapFilterVisitor
import org.identityconnectors.framework.common.objects.AttributeBuilder
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.filter.Filter
import org.identityconnectors.framework.common.objects.ResultsHandler

import java.sql.Connection

/**
 * Built-in accessible objects
 **/

// OperationType is SEARCH for this script
def operation = operation as OperationType

// The configuration class created specifically for this connector
def configuration = configuration as HRDBConfiguration

// Default logging facility
def log = log as Log

// The objectClass of the object to be searched, e.g. ACCOUNT or GROUP
def objectClass = objectClass as ObjectClass

// The search filter for this operation
def filter = filter as Filter

// Additional options for this operation
def options = options as OperationOptions


def connection = connection as Connection
def ORG = new ObjectClass("organization")


log.info("Entering " + operation + " Script");

def sql = new Sql(connection);
def where = " WHERE 1=1 ";
def whereParams = []

// Set where and whereParams if they have been passed in the request for paging
if (options.pagedResultsCookie != null) {
    def cookieProps = options.pagedResultsCookie.split(",");
    assert cookieProps.size() == 2
    // The timestamp and id are for this example only.
    // The user can use their own properties to sort on.
    // For paging it is important that the properties that you use must identify
    // a distinct set of pages for each iteration of the
    // pagedResultsCookie, which can be decided by last record of the previous set.
    where =  " WHERE timestamp >= ? AND id > ? "
    whereParams = [ cookieProps[0], cookieProps[1].toInteger()]
}

// Determine what properties will be used to sort the query
def orderBy = []
if (options.sortKeys != null && options.sortKeys.size() > 0) {
    options.sortKeys.each {
        def key = it.toString();
        if (key.substring(0,1) == "+") {
            orderBy.add(key.substring(1,key.size()) + " ASC")
        } else {
            orderBy.add(key.substring(1,key.size()) + " DESC")
        }
    }
    orderBy = " ORDER BY " + orderBy.join(",")
} else {
    orderBy = ""
}

def limit = ""
if (options.pageSize != null) {
    limit = " LIMIT " + options.pageSize.toString()
}

// keep track of lastTimestamp and lastId so we can
// use it for the next request to do paging
def lastTimestamp
def lastId

if (filter != null) {

    def query = filter.accept(MapFilterVisitor.INSTANCE, null)
    //Need to handle the __UID__ and __NAME__ in queries - this map has entries for each objectType,
    //and is used to translate fields that might exist in the query object from the ICF identifier
    //back to the real property name.
    def fieldMap = [
            "organization": [
                    "__UID__" : "id",
                    "__NAME__": "name"
            ],
            "__ACCOUNT__" : [
                    "__UID__" : "id",
                    "__NAME__": "uid"
            ],
            "__GROUP__"   : [
                    "__UID__" : "id",
                    "__NAME__": "name"
            ]
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

    // this closure function recurses through the (potentially complex) query object in order to build an equivalent SQL 'where' expression
    def queryParser
    queryParser = { queryObj ->

        if (queryObj.operation == "OR" || queryObj.operation == "AND") {
            return "(" + queryParser(queryObj.right) + " " + queryObj.operation + " " + queryParser(queryObj.left) + ")";
        } else {

            if (fieldMap[objectClass.objectClassValue] && fieldMap[objectClass.objectClassValue][queryObj.get("left")]) {
                queryObj.put("left", fieldMap[objectClass.objectClassValue][queryObj.get("left")]);
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
def resultCount = 0
switch (objectClass) {
    case ObjectClass.ACCOUNT:
        def dataCollector = [ uid: "", cars: [] ]

        def handleCollectedData = {
            if (dataCollector.uid != "") {
                handler {
                    uid dataCollector.id
                    id dataCollector.uid
                    attribute 'uid', dataCollector.uid
                    attribute 'fullname', dataCollector.fullname
                    attribute 'firstname', dataCollector.firstname
                    attribute 'lastname', dataCollector.lastname
                    attribute 'email', dataCollector.email
                    attribute 'organization', dataCollector.organization
                    attributes AttributeBuilder.build('cars', dataCollector.cars)
                }

            }
        }

        def statement = """
            SELECT
            u.id,
            u.uid,
            u.fullname,
            u.firstname,
            u.lastname,
            u.email,
            u.organization,
            u.timestamp,
            c.year,
            c.make,
            c.model
            FROM
            users u
            LEFT OUTER JOIN
            car c
            ON c.users_id = u.id
            ${where}
            ${orderBy}
            ${limit}
        """

        sql.eachRow(statement, whereParams, { row ->
            if (dataCollector.uid != row.uid) {
                // new user row, process what we've collected

                handleCollectedData();

                dataCollector = [
                        id : row.id as String,
                        uid : row.uid,
                        fullname: row.fullname,
                        firstname: row.firstname,
                        lastname: row.lastname,
                        email: row.email,
                        organization: row.organization,
                        cars : [ ]
                ]
            }

            if (row.year) {
                dataCollector.cars.add([
                        year: row.year,
                        make: row.make,
                        model: row.model
                ])
            }

            lastTimestamp = row.timestamp
            lastId = row.id
            resultCount++
        });

        handleCollectedData();

        break

    case ObjectClass.GROUP:

        def dataCollector = [ gid: "", users: [] ]

        def handleCollectedData = {
            if (dataCollector.gid != "") {
                handler {
                    uid dataCollector.id
                    id dataCollector.gid
                    attribute 'gid', dataCollector.gid
                    attribute 'name', dataCollector.name
                    attribute 'description', dataCollector.description
                    attributes AttributeBuilder.build('users', dataCollector.users)
                }

            }
        }

        sql.eachRow("""
        SELECT
            g.id,
            g.gid,
            g.name,
            g.description,
            g.timestamp,
            u.users_id
        FROM
            groups g
        LEFT OUTER JOIN
            groups_users u
        ON
            u.groups_id = g.id
        ${where}
        ${orderBy}
        ${limit}
        """, whereParams, { row ->

            if (dataCollector.gid != row.gid) {
                // new user row, process what we've collected

                handleCollectedData();

                dataCollector = [
                        id : row.id as String,
                        gid : row.gid,
                        name: row.name,
                        description: row.description,
                        users : [ ]
                ]
            }

            if (row.users_id) {
                dataCollector.users.add([
                        uid: row.users_id
                ])
            }

            lastTimestamp = row.timestamp
            lastId = row.id
            resultCount++
        });

        handleCollectedData();

        break

    case ORG:
        sql.eachRow("""
        SELECT
                id,
                uid,
                description,
                timestamp
        FROM
                organizations
        ${where}
        ${orderBy}
        ${limit}
        """, whereParams, { row ->
            handler {
                id row.name
                uid row.id as String
                attribute 'description', row.description
            }
            lastTimestamp = row.timestamp
            lastId = row.id
            resultCount++
        });
        break

    default:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
}

// If paging is not wanted just return the default SearchResult object
if (orderBy.toString().isEmpty() || limit.toString().isEmpty() || resultCount < options.pageSize) {
    return new SearchResult();
}

return new SearchResult(lastTimestamp.toString() + "," + lastId.toString(), -1);
