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
import com.fasterxml.jackson.annotation.JsonProperty;;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;

/**
 *
 */
@JsonIgnoreProperties({"persistentState"})
public class HistoricProcessInstanceMixIn {

    @JsonProperty(ActivitiConstants.ACTIVITI_BUSINESSKEY)
    protected String businessKey;
    @JsonProperty(ActivitiConstants.ACTIVITI_DELETEREASON)
    protected String deleteReason;
    @JsonProperty(ActivitiConstants.ID)
    protected String id;
    @JsonProperty(ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID)
    protected String processDefinitionId;
    @JsonProperty(ActivitiConstants.ACTIVITI_STARTUSERID)
    protected String startUserId;
    @JsonProperty(ActivitiConstants.ACTIVITI_DURATIONINMILLIS)
    protected Long durationInMillis;
    @JsonProperty(ActivitiConstants.ACTIVITI_STARTTIME)
    @JsonSerialize(using = DateSerializer.class)
    protected Date startTime;
    @JsonProperty(ActivitiConstants.ACTIVITI_ENDTIME)
    @JsonSerialize(using = DateSerializer.class)
    protected Date endTime;
    @JsonProperty(ActivitiConstants.ACTIVITI_SUPERPROCESSINSTANCEID)
    protected String superProcessInstanceId;
}
