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
package org.forgerock.openidm.workflow.activiti.impl;

import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonTransformer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.openidm.util.DateUtil;
import org.joda.time.DateTime;

/**
 * Utility class for Activiti workflow integration
 */
public class ActivitiUtil {
    /**
     * Fetch and remove process key from the request
     * @param request Request to be processed
     * @return process key
     */
    public static String removeKeyFromRequest(CreateRequest request) {
          return (String) (request.getContent().isNull()
                  ? null
                  : request.getContent().expect(Map.class).asMap().remove("_key"));
    }
    
    /**
     * Fetch and remove process business key from the request
     * @param request Request to be processed
     * @return process business key
     */
    public static String removeBusinessKeyFromRequest(CreateRequest request) {
          return (String) (request.getContent().isNull()
                  ? null
                  : request.getContent().expect(Map.class).asMap().remove("_businessKey"));
    }
    
    /**
     * Fetch and remove Activiti workflow processDefinitionId if present
     * @param request Request to be processed
     * @return processDefinitionId
     */
    public static String removeProcessDefinitionIdFromRequest(CreateRequest request) {
        return (String) (request.getContent().isNull()
                ? null
                : request.getContent().expect(Map.class).asMap().remove("_processDefinitionId"));
    }
    
    /**
     * Fetch the body of the request
     * @param request Request to be processed
     * @return request body
     */
    public static Map<String, Object> getRequestBodyFromRequest(CreateRequest request) {
        if (!request.getContent().isNull()) {
            JsonValue val = request.getContent();
            val.getTransformers().add(new DatePropertyTransformer());
            val.applyTransformers();
            val = val.copy();
            return new HashMap<String, Object>(val.expect(Map.class).asMap());
        } else {
            return new HashMap<String, Object>(1);
        }
    }
    
    /**
     * Fetch query parameters from the request
     * @param request Request to be processed
     * @param paramName parameter to be fetched
     * @return 
     */
    public static String getParamFromRequest(QueryRequest request, String paramName) {
        return request.getAdditionalParameters().get(paramName);
    }
    
    private static class DatePropertyTransformer implements JsonTransformer {
        @Override
        public void transform(JsonValue value) throws JsonException {
            if (null != value && value.isString()) {
                DateTime d = DateUtil.getDateUtil().parseIfDate(value.asString());
                if (d != null){
                    value.setObject(d.toDate());
                }
            }
        }
    }
    /**
     * Process the query parameters if they are workflow/task specific
     * (prefixed: var-...)
     *
     * @param request incoming request
     * @return map of the workflow/task parameters
     */
    public static Map<String, String> fetchVarParams(QueryRequest request) {
        Map<String, String> wfParams = new HashMap<String, String>();
        Iterator<Entry<String, String>> itAll = request.getAdditionalParameters().entrySet().iterator();
        while (itAll.hasNext()) {
            Map.Entry<String, String> e = itAll.next();
            if ((e.getKey().startsWith(ActivitiConstants.VARIABLE_QUERY_PREFIX))) {
                wfParams.put(e.getKey().substring(4), e.getValue());
            }
        }
        return wfParams;
    }
    
}
