/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2015 ForgeRock AS.
 */
package org.forgerock.openidm.workflow.activiti.impl.mixin;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;

/**
 *
 */
@JsonIgnoreProperties({"variableInstances", "execution", "taskDefinition", "candidates", "identityLinks", "variables",
    "variablesLocal", "variableNames", "variableNamesLocal", "assignee", "persistentState", "processInstance"})
public class TaskEntityMixIn {

    @JsonProperty(ActivitiConstants.ID)
    protected String id;
    @JsonProperty(ActivitiConstants.REVISION)
    protected int revision;
    @JsonProperty(ActivitiConstants.ACTIVITI_NAME)
    protected String name;
    @JsonProperty(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID)
    protected String processDefinitionId;
    @JsonProperty(ActivitiConstants.ACTIVITI_PROCESSINSTANCEID)
    protected String processInstanceId;
    @JsonProperty(ActivitiConstants.ACTIVITI_OWNER)
    protected String owner;
    @JsonProperty(ActivitiConstants.ACTIVITI_DESCRIPTION)
    protected String description;
    @JsonProperty(ActivitiConstants.ACTIVITI_CREATETIME)
    @JsonSerialize(using = DateSerializer.class)
    protected Date createTime;
    @JsonProperty(ActivitiConstants.ACTIVITI_DUEDATE)
    protected Date dueDate;
    @JsonProperty(ActivitiConstants.ACTIVITI_EXECUTIONID)
    protected String executionId;
    @JsonProperty(ActivitiConstants.ACTIVITI_PRIORITY)
    protected int priority;
    @JsonProperty(ActivitiConstants.ACTIVITI_TASKDEFINITIONKEY)
    protected String taskDefinitionKey;
}
