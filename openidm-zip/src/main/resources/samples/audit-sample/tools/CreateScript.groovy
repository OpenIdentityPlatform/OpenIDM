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
// The connector sends us the following:
// connection : SQL connection
// action: String correponding to the action ("CREATE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (recon, activity, access)
// id: The entry identifier (OpenICF "Name" atribute. (most often matches the uid)
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
// password: password string, clear text
// options: a handler to the OperationOptions Map

// log.info("Entering {0} Script for {1} with attributes {2}", action, objectClass, attributes);

def sql = new Sql(connection);
//Create must return UID. Let's return the name for now.

switch ( objectClass ) {
    case "auditrecon":
    sql.execute("INSERT INTO auditrecon (objectid,entrytype,rootactionid,activity,message,reconciling,reconid,situation,sourceobjectid,status,targetobjectid,ambiguoustargetobjectids,activitydate,actionid,exceptiondetail,mapping,messagedetail) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        [
            id, // objectid
            attributes.get("entrytype")?.get(0),
            attributes.get("rootactionid")?.get(0),
            attributes.get("activity")?.get(0),
            attributes.get("message")?.get(0),
            attributes.get("reconciling")?.get(0),
            attributes.get("reconid")?.get(0),
            attributes.get("situation")?.get(0),
            attributes.get("sourceobjectid")?.get(0),
            attributes.get("status")?.get(0),
            attributes.get("targetobjectid")?.get(0),
            attributes.get("ambiguoustargetobjectids")?.get(0),
            attributes.get("activitydate")?.get(0),
            attributes.get("actionid")?.get(0),
            attributes.get("exceptiondetail")?.get(0),
            attributes.get("mapping")?.get(0),
            attributes.get("messagedetail")?.get(0)
        ]);
    sql.commit();
    break

    case "auditactivity":
    sql.execute("INSERT INTO auditactivity (objectid,activityid,activitydate,activity,message,subjectid,subjectrev,rootactionid,parentactionid,requester,subjectbefore,subjectafter,status,changedfields,passwordchanged) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        [
            id, // objectid
            attributes.get("activityid")?.get(0),
            attributes.get("activitydate")?.get(0),
            attributes.get("activity")?.get(0),
            attributes.get("message")?.get(0),
            attributes.get("subjectid")?.get(0),
            attributes.get("subjectrev")?.get(0),
            attributes.get("rootactionid")?.get(0),
            attributes.get("parentactionid")?.get(0),
            attributes.get("requester")?.get(0),
            attributes.get("subjectbefore")?.get(0),
            attributes.get("subjectafter")?.get(0),
            attributes.get("status")?.get(0),
            attributes.get("changedfields")?.get(0),
            attributes.get("passwordchanged")?.get(0)
        ]);
    sql.commit();
    break

    case "auditaccess":

    sql.execute("INSERT INTO auditaccess (objectid,activity,ip,principal,roles,status,activitydate,userid) values (?,?,?,?,?,?,?,?)",
        [
            id,
            attributes.get("activity")?.get(0),
            attributes.get("ip")?.get(0),
            attributes.get("principal")?.get(0),
            attributes.get("roles")?.join(','),
            attributes.get("status")?.get(0),
            attributes.get("activitydate")?.get(0),
            attributes.get("userid")?.get(0)
        ]);
    sql.commit();
    break

    default:
    log.warn("Didn't match objectClass " + objectClass);
}

return id;
