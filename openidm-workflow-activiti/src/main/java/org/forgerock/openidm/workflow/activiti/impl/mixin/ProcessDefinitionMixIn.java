/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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
package org.forgerock.openidm.workflow.activiti.impl.mixin;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;

/**
 *
 */
@JsonIgnoreProperties({"processDefinition", "identityLinks", "activities", "initial", "initialActivityStack", "persistentState"})
public abstract class ProcessDefinitionMixIn {

    @JsonProperty(ActivitiConstants.ACTIVITI_CATEGORY)
    protected String category;
    @JsonProperty(ActivitiConstants.ACTIVITI_DEPLOYMENTID)
    protected String deploymentId;
    @JsonProperty(ActivitiConstants.ACTIVITI_DESCRIPTION)
    protected String description;
    @JsonProperty(ActivitiConstants.ACTIVITI_DIAGRAMRESOURCENAME)
    protected String diagramResourceName;
    @JsonProperty(ActivitiConstants.ID)
    protected String id;
    @JsonProperty(ActivitiConstants.REVISION)
    protected int revision;
    @JsonProperty(ActivitiConstants.ACTIVITI_KEY)
    protected String key;
    @JsonProperty(ActivitiConstants.ACTIVITI_NAME)
    protected String name;
}
