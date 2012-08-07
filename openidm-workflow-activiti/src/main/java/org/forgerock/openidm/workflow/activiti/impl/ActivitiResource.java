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
package org.forgerock.openidm.workflow.activiti.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;

/**
 * Implementation of the Activiti Engine Resource
 * 
 * @author orsolyamebold
 */
public class ActivitiResource implements JsonResource {

    private ProcessEngine processEngine;

    public ActivitiResource(ProcessEngine engine) {
        this.processEngine = engine;
    }

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    public JsonValue action(JsonValue params) throws JsonResourceException {
        JsonValue result = null;
        String action = ActivitiUtil.getActionFromRequest(params);
        //POST openidm/workflow/activiti?_action=TestWorkFlow will trigger the process
        String processDefinitionId = ActivitiUtil.getProcessDefinitionIdFromRequest(params);
        Map<String, Object> variables = ActivitiUtil.getProcessVariablesFromRequest(params);

        //TODO consider to put only the parent into the params. parent/security may contain confidential access token
        //variables.put("openidm-context", new HashMap(params.get("parent").asMap()));
        ProcessInstance instance;
        if (processDefinitionId == null) {
            instance = processEngine.getRuntimeService().startProcessInstanceByKey(action, variables);
        } else {
            instance = processEngine.getRuntimeService().startProcessInstanceById(processDefinitionId, variables);
        }
        if (instance != null) {
            result = new JsonValue(new HashMap<String, Object>());
            result.put("status", instance.isEnded() ? "ended" : "suspended");
            result.put("processInstanceId", instance.getProcessInstanceId());
            result.put("businessKey", instance.getBusinessKey());
            result.put("processDefinitionId", instance.getProcessDefinitionId());
            result.put("id", instance.getId());
        }

        return result;
    }

    /**
     * Query the available workflow definitions
     * @return workflow definitions
     * @throws JsonResourceException 
     */
    public JsonValue read() throws JsonResourceException {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        List<ProcessDefinition> definitionList = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        if (definitionList != null && definitionList.size() > 0) {
            for (ProcessDefinition processDefinition : definitionList) {
                Map<String, Object> processMap = new HashMap<String, Object>();
                processMap.put("key", processDefinition.getKey());
                processMap.put("name", processDefinition.getName());
                result.put(processDefinition.getId(), processMap);
            }
        }
        return result;
    }

    @Override
    public JsonValue handle(JsonValue jv) throws JsonResourceException {
        try {
            switch (jv.get("method").required().asEnum(SimpleJsonResource.Method.class)) {
                case create:
                    return null;
                case read:
                    return read();
                case update:
                    return null;
                case delete:
                    return null;
                case patch:
                    return null;
                case query:
                    return null;
                case action:
                    return action(jv);
                default:
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
            }
        } catch (JsonValueException jve) {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, jve);
        }
    }
}
