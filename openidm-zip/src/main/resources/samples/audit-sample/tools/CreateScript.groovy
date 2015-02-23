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
import groovy.sql.Sql;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;

import java.sql.Connection;

// Parameters:
// The connector sends us the following:
// connection : SQL connection
// action: String correponding to the action ("CREATE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (recon, activity, access)
// id: The entry identifier (OpenICF "Name" atribute. Most often matches the uid)
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
// password: password string, clear text
// options: a handler to the OperationOptions Map

// log.info("Entering {0} Script for {1} with attributes {2}", action, objectClass, attributes);

def sql = new Sql(connection as Connection);
//Create must return UID. Let's return the name for now.

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
    case auditrecon:
    sql.execute("INSERT INTO auditrecon (objectid,entrytype,rootactionid,activity,message,reconciling,reconid,situation,sourceobjectid,status,targetobjectid,ambiguoustargetobjectids,activitydate,actionid,exceptiondetail,linkqualifier,mapping,messagedetail) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        [
            id, // objectid
            attributeMap.get("entrytype")?.getValue()?.get(0),
            attributeMap.get("rootactionid")?.getValue()?.get(0),
            attributeMap.get("activity")?.getValue()?.get(0),
            attributeMap.get("message")?.getValue()?.get(0),
            attributeMap.get("reconciling")?.getValue()?.get(0),
            attributeMap.get("reconid")?.getValue()?.get(0),
            attributeMap.get("situation")?.getValue()?.get(0),
            attributeMap.get("sourceobjectid")?.getValue()?.get(0),
            attributeMap.get("status")?.getValue()?.get(0),
            attributeMap.get("targetobjectid")?.getValue()?.get(0),
            attributeMap.get("ambiguoustargetobjectids")?.getValue()?.get(0),
            attributeMap.get("activitydate")?.getValue()?.get(0),
            attributeMap.get("actionid")?.getValue()?.get(0),
            attributeMap.get("exceptiondetail")?.getValue()?.get(0),
            attributeMap.get("linkqualifier")?.getValue()?.get(0),    
            attributeMap.get("mapping")?.getValue()?.get(0),
            attributeMap.get("messagedetail")?.getValue()?.get(0)
        ]);
    break

    case auditactivity:
    sql.execute("INSERT INTO auditactivity (objectid,activityid,activitydate,activity,message,subjectid,subjectrev,rootactionid,parentactionid,requester,subjectbefore,subjectafter,status,changedfields,passwordchanged) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        [
            id, // objectid
            attributeMap.get("activityid")?.getValue()?.get(0),
            attributeMap.get("activitydate")?.getValue()?.get(0),
            attributeMap.get("activity")?.getValue()?.get(0),
            attributeMap.get("message")?.getValue()?.get(0),
            attributeMap.get("subjectid")?.getValue()?.get(0),
            attributeMap.get("subjectrev")?.getValue()?.get(0),
            attributeMap.get("rootactionid")?.getValue()?.get(0),
            attributeMap.get("parentactionid")?.getValue()?.get(0),
            attributeMap.get("requester")?.getValue()?.get(0),
            attributeMap.get("subjectbefore")?.getValue()?.get(0),
            attributeMap.get("subjectafter")?.getValue()?.get(0),
            attributeMap.get("status")?.getValue()?.get(0),
            attributeMap.get("changedfields")?.getValue()?.get(0),
            attributeMap.get("passwordchanged")?.getValue()?.get(0)
        ]);
    break

    case auditaccess:

    sql.execute("INSERT INTO auditaccess (objectid,activity,ip,principal,roles,status,activitydate,userid) values (?,?,?,?,?,?,?,?)",
        [
            id,
            attributeMap.get("activity")?.getValue()?.get(0),
            attributeMap.get("ip")?.getValue()?.get(0),
            attributeMap.get("principal")?.getValue()?.get(0),
            attributeMap.get("roles")?.getValue()?.join(','),
            attributeMap.get("status")?.getValue()?.get(0),
            attributeMap.get("activitydate")?.getValue()?.get(0),
            attributeMap.get("userid")?.getValue()?.get(0)
        ]);
    break

    case auditsync:

        sql.execute("INSERT INTO auditsync (objectid,rootactionid,sourceobjectid,targetobjectid,activitydate,activity,situation,status,message,actionid,exceptiondetail,linkqualifier,mapping,messagedetail) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                [
                        id,
                        attributeMap.get("rootactionid")?.getValue()?.get(0),
                        attributeMap.get("sourceobjectid")?.getValue()?.get(0),
                        attributeMap.get("targetobjectid")?.getValue()?.get(0),
                        attributeMap.get("activitydate")?.getValue()?.get(0),
                        attributeMap.get("activity")?.getValue()?.get(0),
                        attributeMap.get("situation")?.getValue()?.get(0),
                        attributeMap.get("status")?.getValue()?.get(0),
                        attributeMap.get("message")?.getValue()?.get(0),
                        attributeMap.get("actionid")?.getValue()?.get(0),
                        attributeMap.get("exceptiondetail")?.getValue()?.get(0),
                        attributeMap.get("linkqualifier")?.getValue()?.get(0),
                        attributeMap.get("mapping")?.getValue()?.get(0),
                        attributeMap.get("messagedetail")?.getValue()?.get(0)
                ]);
    break

    default:
    log.warn("Didn't match objectClass " + objectClass);
}

return id;
