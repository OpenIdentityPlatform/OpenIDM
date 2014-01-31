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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2013-2014 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.util.Factory;
import org.forgerock.util.LazyMap;

public class ScriptUtil {
    // a ScriptName will have this request-binding if it wishes OpenIDM 2.1.x request bindings
    public static final String REQUEST_BINDING_2_1 = "OEPNIDM-2.1";

    public static Map<String, Object> getRequestMap(Request request, ServerContext context) {
        Map<String, Object> requestMap = new HashMap<String, Object>();
        JsonValue value = new JsonValue(null);
        String id = request.getResourceName();
        if (request instanceof ActionRequest) {
            value = ((ActionRequest)request).getContent();
            requestMap.put("params", ((ActionRequest)request).getAdditionalParameters());
            requestMap.put("method", "action");
            requestMap.put("action", ((ActionRequest)request).getAction());
        } else if (request instanceof CreateRequest) {
            CreateRequest createRequest = (CreateRequest)request;
            value = createRequest.getContent();
            requestMap.put("method", "create");
            if (createRequest.getNewResourceId() != null) {
                id = createRequest.getResourceName() + "/" + createRequest.getNewResourceId();
            }
        } else if (request instanceof ReadRequest) {
            requestMap.put("method", "read");
        } else if (request instanceof UpdateRequest) {
            value = ((UpdateRequest)request).getContent();
            requestMap.put("method", "update");
        } else if (request instanceof DeleteRequest) {
            requestMap.put("method", "delete");
        } else if (request instanceof PatchRequest) {
            requestMap.put("method", "patch");
            List<PatchOperation> ops = ((PatchRequest)request).getPatchOperations();
            JsonValue opsValue = new JsonValue(new ArrayList<Object>());
            for (PatchOperation op : ops) {
                opsValue.add(op.toJsonValue().getObject());
            }
            value = opsValue;
        } else if (request instanceof QueryRequest) {
            final QueryRequest queryRequest = (QueryRequest) request;
            final Map<String, String> params = queryRequest.getAdditionalParameters();
            params.put("_queryId", queryRequest.getQueryId());
            params.put("_queryExpression", queryRequest.getQueryExpression());
            params.put("_queryFilter", queryRequest.getQueryFilter() != null ? queryRequest.getQueryFilter().toString() : null);
            requestMap.put("params", params);
            requestMap.put("method", "query");
        } else {
            requestMap.put("method", null);
        }
        if (context.containsContext(SecurityContext.class)) {
            SecurityContext securityContext = context.asContext(SecurityContext.class);
            Map<String, Object> security = new HashMap<String, Object>();
            security.putAll(securityContext.getAuthorizationId());
            security.put("username", securityContext.getAuthenticationId());
            requestMap.put("security", security);
        }
        
        if (isFromHttp(context)) {
            HttpContext httpContext = context.asContext(HttpContext.class);
            requestMap.put("headers", httpContext.getHeaders());
            requestMap.put("fromHttp", "true");
            requestMap.put("params", httpContext.getParameters());
        } else {
            requestMap.put("fromHttp", "false");
        }
        requestMap.put("external", String.valueOf(ContextUtil.isExternal(context)));
        requestMap.put("type", request.getRequestType().toString());
        requestMap.put("value", value.getObject());
        requestMap.put("id", id);
        
        return requestMap;
    }

    private static boolean isFromHttp(ServerContext context) {
        Context c = context.getParent();
        if (c != null && c.getParent() != null && HttpContext.class.isAssignableFrom(c.getParent().getClass())) {
            return true;
        }
        return false;
    }

    public static  LazyMap<String, Object> getLazyContext(final ServerContext context) {
        return new LazyMap<String, Object>(new Factory<Map<String, Object>>() {
            @Override
            public Map<String, Object> newInstance() {
                if (null != context) {
                    return context.toJsonValue().required().asMap();
                }
                else {
                    return Collections.emptyMap();
                }
            }
        });
    }

}
