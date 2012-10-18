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
import java.util.Map;
import org.forgerock.json.fluent.JsonValue;

/**
 * Utility class for Activiti workflow integration
 * @author orsolyamebold
 */
public class ActivitiUtil {
    /**
     * Fetch and remove OpenIDM action from the request
     * @param request Request to be processed
     * @return requested action
     */
    public static String removeKeyFromRequest(JsonValue request) {
          return (String) (request.get("value").isNull() ? null : request.get("value").expect(Map.class).asMap().remove("key"));
    }
    
    /**
     * Fetch and remove Activiti workflow processDefinitionId if present
     * @param request Request to be processed
     * @return processDefinitionId
     */
    public static String removeProcessDefinitionIdFromRequest(JsonValue request) {
        return (String) (request.get("value").isNull() ? null : request.get("value").expect(Map.class).asMap().remove("processDefinitionId"));
    }
    
    /**
     * Fetch the body of the request
     * @param request Request to be processed
     * @return request body
     */
    public static Map<String, Object> getRequestBodyFromRequest(JsonValue request) {
        return request.get("value").isNull() ? 
                new HashMap(1) : new HashMap<String, Object>(request.get("value").expect(Map.class).asMap());
    }
    
    /**
     * 
     * @param request incoming request
     * @return 
     */
    public static String getIdFromRequest(JsonValue request) {
        String[] id = request.get("id").asString().split("/");
        return id[id.length-1];
    }
    
    public static String getQueryIdFromRequest(JsonValue request) {
        return request.get("params").get("_query-id").asString();
    }
}
