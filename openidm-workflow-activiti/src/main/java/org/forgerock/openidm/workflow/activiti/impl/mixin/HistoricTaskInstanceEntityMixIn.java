/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openidm.workflow.activiti.impl.mixin;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;

import java.util.Date;

@JsonIgnoreProperties({"persistentState"})
public class HistoricTaskInstanceEntityMixIn {


    @JsonProperty(ActivitiConstants.ACTIVITI_NAME)
    protected String name;

    @JsonProperty(ActivitiConstants.ACTIVITI_DESCRIPTION)
    protected String description;

    @JsonProperty(ActivitiConstants.ACTIVITI_OWNER)
    protected String owner;

    @JsonProperty(ActivitiConstants.ACTIVITI_ASSIGNEE)
    protected String assignee;

    @JsonProperty(ActivitiConstants.ACTIVITI_TASKDEFINITIONKEY)
    protected String taskDefinitionKey;

    @JsonProperty(ActivitiConstants.ACTIVITI_PRIORITY)
    protected String priority;

    @JsonProperty(ActivitiConstants.ACTIVITI_DUEDATE)
    @JsonSerialize(using = DateSerializer.class)
    protected String dueDate;

    @JsonProperty(ActivitiConstants.ACTIVITI_CLAIMTIME)
    @JsonSerialize(using = DateSerializer.class)
    protected String claimTime;

    @JsonProperty(ActivitiConstants.ID)
    protected String id;

    @JsonProperty(ActivitiConstants.ACTIVITI_PROCESSINSTANCEID)
    protected String processId;

    @JsonProperty(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID)
    protected String processDefinitionID;

    @org.codehaus.jackson.annotate.JsonProperty(ActivitiConstants.ACTIVITI_STARTTIME)
    @JsonSerialize(using = DateSerializer.class)
    protected Date startTime;

    @org.codehaus.jackson.annotate.JsonProperty(ActivitiConstants.ACTIVITI_ENDTIME)
    @JsonSerialize(using = DateSerializer.class)
    protected Date endTime;

    @JsonProperty(ActivitiConstants.ACTIVITI_DURATIONINMILLIS)
    protected Long durationInMillis;

    @JsonProperty(ActivitiConstants.ACTIVITI_DELETEREASON)
    protected String deleteReason;

}
