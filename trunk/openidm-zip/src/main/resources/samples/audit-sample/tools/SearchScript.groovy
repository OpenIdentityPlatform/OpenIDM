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
import org.identityconnectors.framework.common.objects.SearchResult;

import java.sql.Connection;

import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;

def sql = new Sql(connection as Connection);
def result = [];
def where = "";
def filter = filter as Filter;

def auditrecon = new ObjectClass("auditrecon");
def auditactivity = new ObjectClass("auditactivity");
def auditaccess = new ObjectClass("auditaccess");
def auditsync = new ObjectClass("auditsync");

if (filter instanceof EqualsFilter && ((EqualsFilter) filter).getAttribute().is(Uid.NAME)) {
    //This is a Read

    def id = AttributeUtil.getStringValue(((EqualsFilter) filter).getAttribute());
    where = " WHERE objectid = '${id}'";
}

switch ( objectClass ) {
    case auditrecon:
        sql.eachRow("SELECT * FROM auditrecon" + where,
            { row ->
                handler {
                    uid row.objectid
                    id row.objectid // Name is required by OpenICF connector
                    attribute 'entrytype', row.entrytype
                    attribute 'rootactionid', row.rootactionid
                    attribute 'activity', row.activity
                    attribute 'message', row.message
                    attribute 'reconciling', row.reconciling
                    attribute 'reconid', row.reconid
                    attribute 'situation', row.situation
                    attribute 'sourceobjectid', row.sourceobjectid
                    attribute 'status:', row.status
                    attribute 'targetobjectid', row.targetobjectid
                    attribute 'ambiguoustargetobjectids', row.ambiguoustargetobjectids
                    attribute 'activitydate', row.activitydate
                    attribute 'actionid', row.actionid
                    attribute 'exceptiondetail', row.exceptiondetail
                    attribute 'linkqualifier', row.linkqualifier
                    attribute 'mapping', row.mapping
                    attribute 'messagedetail', row.messagedetail
                }
            }
        );
    break

    case auditactivity:
        sql.eachRow("SELECT * FROM auditactivity" + where,
            { row ->
                handler {
                    uid row.objectid
                    id row.objectid // Name is required by OpenICF connector
                    attribute 'activityid', row.activityid
                    attribute 'activitydate', row.activitydate
                    attribute 'activity', row.activity
                    attribute 'message', row.message
                    attribute 'subjectid', row.subjectid
                    attribute 'subjectrev', row.subjectrev
                    attribute 'rootactionid', row.rootactionid
                    attribute 'parentactionid', row.parentactionid
                    attribute 'requester', row.requester
                    attribute 'subjectbefore', row.subjectbefore
                    attribute 'subjectafter', row.subjectafter
                    attribute 'status', row.status
                    attribute 'changedfields', row.changedfields
                    attribute 'passwordchanged', row.passwordchanged
                }
            }
        );
    break

    case auditaccess:
        sql.eachRow("SELECT * FROM auditaccess" + where,
            { row ->
                handler {
                    uid row.objectid
                    id row.objectid // Name is required by OpenICF connector
                    attribute 'activity', row.activity
                    attribute 'ip', row.ip
                    attribute 'principal', row.principal
                    attribute 'roles', row.roles?.tokenize(',')
                    attribute 'status', row.status
                    attribute 'activitydate', row.activitydate
                    attribute 'userid', row.userid
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
                    attribute 'objectid', row.objectid
                    attribute 'rootactionid', row.rootactionid
                    attribute 'sourceobjectid', row.sourceobjectid
                    attribute 'targetobjectid:', row.targetobjectid
                    attribute 'activitydate', row.activitydate
                    attribute 'activity', row.activity
                    attribute 'situation', row.situation
                    attribute 'status', row.status
                    attribute 'message', row.message
                    attribute 'actionid', row.actionid
                    attribute 'exceptiondetail', row.exceptiondetail
                    attribute 'linkqualifier', row.linkqualifier
                    attribute 'mapping', row.mapping
                    attribute 'messagedetail', row.messagedetail
                }
            }
        );
        break;

    default:
    log.warn("Didn't match objectClass " + objectClass);
}

return new SearchResult();