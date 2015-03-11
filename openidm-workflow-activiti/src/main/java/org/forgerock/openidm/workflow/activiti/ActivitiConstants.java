/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.workflow.activiti;

import org.forgerock.json.resource.Resource;
import org.forgerock.openidm.core.ServerConstants;

/**
 *
 */
public class ActivitiConstants {
    public static final String PROCESSDEFINITION_PATTERN = "/?processdefinition.*";
    public static final String PROCESSDEFINITION_ID_PATTERN = "/?processdefinition/.+$";
    public static final String PROCESSINSTANCE_PATTERN = "/?processinstance.*";
    public static final String PROCESSINSTANCE_ID_PATTERN = "/?processinstance/.+$";
    public static final String TASKINSTANCE_PATTERN = "/?taskinstance.*";
    public static final String TASKINSTANCE_ID_PATTERN = "/?taskinstance/.+$";
    public static final String TASKDEFINITION_PATTERN = "/?taskdefinition.*";
    public static final String TASKDEFINITION_ID_PATTERN = "/?taskdefinition/.+$";
    public static final String ID = Resource.FIELD_CONTENT_ID;
    public static final String REVISION = Resource.FIELD_CONTENT_REVISION;
    public static final String REQUEST_PARAMS = "params";
    public static final String REQUEST_BODY = "value";
    public static final String QUERY_ALL_IDS = ServerConstants.QUERY_ALL_IDS;
    public static final String QUERY_FILTERED = "filtered-query";
    public static final String QUERY_TASKDEF = "query-taskdefinition";
    public static final String VARIABLE_QUERY_PREFIX = "var-";
    public static final String OPENIDM_CONTEXT = "openidmcontext";
    public static final String ACTIVITI_PROCESSDEFINITIONID = "processDefinitionId";
    public static final String ACTIVITI_PROCESSDEFINITIONKEY = "processDefinitionKey";
    public static final String ACTIVITI_PROCESSDEFINITIONRESOURCENAME = "processDefinitionResourceName";
    public static final String ACTIVITI_PROCESSINSTANCEBUSINESSKEY = "processInstanceBusinessKey";
    public static final String ACTIVITI_PROCESSINSTANCEID = "processInstanceId";
    public static final String ACTIVITI_DIAGRAMRESOURCENAME = "processDiagramResourceName";
    public static final String ACTIVITI_FORMRESOURCEKEY = "formResourceKey";
    public static final String ACTIVITI_DEPLOYMENTID = "deploymentId";
    public static final String ACTIVITI_KEY = "key";
    public static final String ACTIVITI_STARTTIME = "startTime";
    public static final String ACTIVITI_ENDTIME = "endTime";
    public static final String ACTIVITI_STATUS = "status";
    public static final String ACTIVITI_BUSINESSKEY = "businessKey";
    public static final String ACTIVITI_DELETEREASON = "deleteReason";
    public static final String ACTIVITI_DURATIONINMILLIS = "durationInMillis";
    public static final String ACTIVITI_TASKNAME = "taskName";
    public static final String ACTIVITI_ASSIGNEE = "assignee";
    public static final String ACTIVITI_DESCRIPTION = "description";
    public static final String ACTIVITI_NAME = "name";
    public static final String ACTIVITI_OWNER = "owner";
    public static final String ACTIVITI_CREATETIME = "createTime";
    public static final String ACTIVITI_DUEDATE = "dueDate";
    public static final String ACTIVITI_EXECUTIONID = "executionId";
    public static final String ACTIVITI_CANDIDATEGROUP = "taskCandidateGroup";
    public static final String ACTIVITI_CANDIDATEUSER = "taskCandidateUser";
    public static final String ACTIVITI_STARTUSERID = "startUserId";
    public static final String ACTIVITI_SUPERPROCESSINSTANCEID = "superProcessInstanceId";
    public static final String ACTIVITI_TASKID = "taskId";
    public static final String ACTIVITI_PRIORITY = "priority";
    public static final String ACTIVITI_TASKDEFINITIONKEY = "taskDefinitionKey";
    public static final String ACTIVITI_VARIABLES = "variables";
    public static final String ACTIVITI_DELEGATE = "delegate";
    public static final String ACTIVITI_VERSION = "version";
    public static final String ACTIVITI_CATEGORY = "category";
    public static final String LIKE = "Like";
    public static final String FORMPROPERTIES = "formProperties";
    public static final String FORMPROPERTY_ID = "id";
    public static final String FORMPROPERTY_TYPE = "type";
    public static final String FORMPROPERTY_VALUE = "value";
    public static final String FORMPROPERTY_READABLE = "readable";
    public static final String FORMPROPERTY_REQUIRED = "required";
    public static final String FORMPROPERTY_WRITABLE = "writable";
    public static final String FORMPROPERTY_VARIABLENAME = "variableName";
    public static final String FORMPROPERTY_DEFAULTEXPRESSION = "defaultExpression";
    public static final String FORMPROPERTY_VARIABLEEXPRESSION = "variableExpression";
    public static final String ENUM_VALUES = "values";
    public static final String DATE_PATTERN = "datePattern";
    public static final String ACTIVITI_FORMGENERATIONTEMPLATE = "formGenerationTemplate";
}
