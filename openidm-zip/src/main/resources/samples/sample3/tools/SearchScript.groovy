/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * $Id$
 */
import groovy.sql.Sql;
import groovy.sql.DataSet;

// Parameters:
// The connector sends the following:
// connection: handler to the SQL connection
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
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


//Need to handle the __UID__ and __NAME__ in queries - this map has entries for each objectType, 
//and is used to translate fields that might exist in the query object from the ICF identifier
//back to the real property name.
def fieldMap = [
    "organization": [
        "__UID__": "name",
        "__NAME__": "name"
    ],
    "__ACCOUNT__": [
        "__UID__": "uid",
        "__NAME__": "uid"
    ],
    "__GROUP__": [
        "__UID__": "name",
        "__NAME__": "name"
    ]
]

log.info("Entering "+action+" Script");

def whereTemplates = [
    CONTAINS:'$left ${not ? "NOT " : ""}LIKE ?',
    ENDSWITH:'$left ${not ? "NOT " : ""}LIKE ?',
    STARTSWITH:'$left ${not ? "NOT " : ""}LIKE ?',
    EQUALS:'$left ${not ? "<>" : "="} ?',
    GREATERTHAN:'$left ${not ? "<=" : ">"} ?',
    GREATERTHANOREQUAL:'$left ${not ? "<" : ">="} ?',
    LESSTHAN:'$left ${not ? ">=" : "<"} ?',
    LESSTHANOREQUAL:'$left ${not ? ">" : "<="} ?'
];

def whereParams = []
def queryParser

// this closure function recurses through the (potentially complex) query object in order to build an equivalent SQL 'where' expression
queryParser = { queryObj ->

    if (queryObj.operation == "OR" || queryObj.operation == "AND") {
        return "(" + queryParser(queryObj.right) + " " + queryObj.operation + " " + queryParser(queryObj.left) + ")";
    } else {

        if (fieldMap[objectClass] && fieldMap[objectClass][queryObj.get("left")]) {
            queryObj.put("left",fieldMap[objectClass][queryObj.get("left")]);
        }

        def engine = new groovy.text.SimpleTemplateEngine()
        def wt = whereTemplates.get(queryObj.get("operation"))
        def binding = [left:queryObj.get("left"),not:queryObj.get("not")]
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


def sql = new Sql(connection);
def result = []
def where = "";

if (query != null){
    // We can use Groovy template engine to generate our custom SQL queries
    where = " WHERE " + queryParser(query)
    log.ok("Search WHERE clause is: "+ where)
}

switch ( objectClass ) {
    case "__ACCOUNT__":
    sql.eachRow("SELECT * FROM Users" + where, whereParams) {
        result.add([
            __UID__:it.uid, 
            __NAME__:it.uid, 
            uid:it.uid, 
            fullname:it.fullname,
            firstname:it.firstname,
            lastname:it.lastname,
            email:it.email,
            organization:it.organization
        ])
    }
    break

    case "__GROUP__":
    sql.eachRow("SELECT * FROM Groups" + where, whereParams) {
        result.add([
            __UID__:it.name, 
            __NAME__:it.name, 
            gid:it.gid,
            description:it.description
        ])
    }
    break

    case "organization":
    sql.eachRow("SELECT * FROM Organizations" + where, whereParams) {
        result.add([
            __UID__:it.name, 
            __NAME__:it.name, 
            description:it.description
        ])
    }
    break

    default:
    result;
}

return result;