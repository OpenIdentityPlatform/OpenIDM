/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All rights reserved.
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
import groovy.sql.Sql;
import groovy.sql.DataSet;

// Parameters:
// The connector sends the following:
// connection: handler to the SQL connection
// objectClass: a String describing the Object class (access, activity, recon)
// action: a string describing the action ("SEARCH" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map
// query: a handler to the Query Map
//
// The Query map describes the filter used.
//
// query = [ operation: "CONTAINS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "ENDSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "STARTSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "EQUALS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = null : then we assume we fetch everything
//
// AND and OR filter just embed a left/right couple of queries.
// query = [ operation: "AND", left: query1, right: query2 ]
// query = [ operation: "OR", left: query1, right: query2 ]
//
// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

log.debug("Entering {0} Script for {1} with qeury {2} and options {3}",
        action, objectClass, query, options);

def sql = new Sql(connection);
def result = []
def where = "";

if (query != null) {
    // Need to handle the __UID__ in queries
    // all audit tables have id as 'objectid'
    if (query.get("left").equalsIgnoreCase("__UID__"))
        query.put("left", "objectid");

    // We can use Groovy template engine to generate our custom SQL queries
    def engine = new groovy.text.SimpleTemplateEngine();

    def whereTemplates = [
        CONTAINS:' WHERE $left ${not ? "NOT " : ""}LIKE "%$right%"',
        ENDSWITH:' WHERE $left ${not ? "NOT " : ""}LIKE "%$right"',
        STARTSWITH:' WHERE $left ${not ? "NOT " : ""}LIKE "$right%"',
        EQUALS:' WHERE $left ${not ? "<>" : "="} "$right"',
        GREATERTHAN:' WHERE $left ${not ? "<=" : ">"} "$right"',
        GREATERTHANOREQUAL:' WHERE $left ${not ? "<" : ">="} "$right"',
        LESSTHAN:' WHERE $left ${not ? ">=" : "<"} "$right"',
        LESSTHANOREQUAL:' WHERE $left ${not ? ">" : "<="} "$right"'
    ]

    def wt = whereTemplates.get(query.get("operation"));
    def binding = [left:query.get("left"),right:query.get("right"),not:query.get("not")];
    def template = engine.createTemplate(wt).make(binding);
    where = template.toString();
    log.debug("Search WHERE clause is: "+ where)
}

switch ( objectClass ) {
    case "recon":
    sql.eachRow("SELECT * FROM auditrecon" + where,
            { result.add(
                [
                __UID__:it.objectid,
                __NAME__:it.objectid, // Name is required by OpenICF connector
                entrytype:it.entrytype,
                rootactionid:it.rootactionid,
                activity:it.activity,
                message:it.message,
                reconciling:it.reconciling,
                reconid:it.reconid,
                situation:it.situation,
                sourceobjectid:it.sourceobjectid,
                status:it.status,
                targetobjectid:it.targetobjectid,
                ambiguoustargetobjectids:it.ambiguoustargetobjectids,
                activitydate:it.activitydate,
                actionid:it.actionid,
                exceptiondetail:it.exceptiondetail,
                mapping:it.mapping,
                messagedetail:it.messagedetail
                ] )
            } );
    break

    case "activity":
    sql.eachRow("SELECT * FROM auditactivity" + where,
            { result.add(
                [
                __UID__:it.objectid,
                __NAME__:it.objectid, // Name is required by OpenICF connector
                activityid:it.activityid,
                activitydate:it.activitydate,
                activity:it.activity,
                message:it.message,
                subjectid:it.subjectid,
                subjectrev:it.subjectrev,
                rootactionid:it.rootactionid,
                parentactionid:it.parentactionid,
                requester:it.requester,
                subjectbefore:it.subjectbefore,
                subjectafter:it.subjectafter,
                status:it.status,
                changedfields:it.changedfields,
                passwordchanged:it.passwordchanged
                ] )
            } );
    break

    case "access":
    sql.eachRow("SELECT * FROM auditaccess" + where, 
            { result.add(
                [
                __UID__:it.objectid,,
                __NAME__:it.objectid, // Name is required by OpenICF connector
                activity:it.activity,
                ip:it.ip,
                principal:it.principal,
                roles:it.roles?.tokenize(','),
                status:it.status,
                activitydate:it.activitydate,
                userid:it.userid
                ] )
            } );
    break

    default:
    log.warn("Didn't match objectClass " + objectClass);
}

log.debug("Returning {0}", result);

return result;
