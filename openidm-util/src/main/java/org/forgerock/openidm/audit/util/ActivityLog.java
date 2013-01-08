/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.audit.util;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ActivityLog {
    final static Logger logger = LoggerFactory.getLogger(ActivityLog.class);

    private final static boolean suspendException;
    private static DateUtil dateUtil;

    public final static String TIMESTAMP = "timestamp";
    public final static String ACTION = "action";
    public final static String MESSAGE = "message";
    public final static String OBJECT_ID = "objectId";
    public final static String REVISION = "rev";
    public final static String ACTIVITY_ID = "activityId";
    public final static String ROOT_ACTION_ID = "rootActionId";
    public final static String PARENT_ACTION_ID = "parentActionid";
    public final static String REQUESTER = "requester";
    public final static String BEFORE = "before";
    public final static String AFTER = "after";
    public final static String STATUS = "status";
    public final static String CHANGED_FIELDS = "changedFields";
    public final static String PASSWORD_CHANGED = "passwordChanged";

    /**
     * Creates a Jackson object mapper. By default, it calls
     * {@link org.codehaus.jackson.map.ObjectMapper#ObjectMapper()}.
     * 
     */
    static {
        String config =
                IdentityServer.getInstance().getProperty(ActivityLog.class.getName().toLowerCase());
        suspendException = "suspend".equals(config);
        // TODO Allow for configured dateUtil
        dateUtil = DateUtil.getDateUtil("UTC");
    }

    public static String getRequester(JsonValue request) {
        String result = null;
        while (request != null && !request.isNull()) {
            JsonValue user = request.get("security").get("username");
            if (user.isString()) {
                result = user.asString();
                break;
            }
            request = request.get("parent");
        }
        return result;
    }

    public static void log(ServerContext router, Request request, String message,
            String objectId, JsonValue before, JsonValue after, Status status)
            throws ResourceException {
        if (request == null) {
            throw new NullPointerException("Request can not be null when audit.");
        }
        // TODO: convert to flyweight?
        try {
            Map<String, Object> activity =
                    buildLog(router, request, message, objectId, before, after, status);
            // TODO: UPGRADE
            router.getConnection().create(new ServerContext(router),
                    Requests.newCreateRequest("audit/activity", new JsonValue(activity)));
        } catch (ResourceException ex) {
            logger.warn("Failed to write activity log {}", ex);
            if (!suspendException) {
                throw ex;
                // TODO: should this stop the activity itself?
            }
        }
    }

    private static Map<String, Object> buildLog(Context context, Request request, String message,
            String objectId, JsonValue before, JsonValue after, Status status) {
        String rev = null;
        if (after != null && after.get(ServerConstants.OBJECT_PROPERTY_REV).isString()) {
            rev = after.get(ServerConstants.OBJECT_PROPERTY_REV).asString();
        } else if (before != null && before.get(ServerConstants.OBJECT_PROPERTY_REV).isString()) {
            rev = before.get(ServerConstants.OBJECT_PROPERTY_REV).asString();
        }

        String method;
        if (request instanceof CreateRequest) {
            method = "create";
        } else if (request instanceof ReadRequest) {
            method = "read";
        } else if (request instanceof UpdateRequest) {
            method = "update";
        } else if (request instanceof QueryRequest) {
            method = "query";
        } else if (request instanceof PatchRequest) {
            method = "patch";
        } else if (request instanceof DeleteRequest) {
            method = "delete";
        } else if (request instanceof ActionRequest) {
            method = "action";
        } else {
            method = "unknown";
        }

        // TODO: make configurable
        if (method != null && (method.equalsIgnoreCase("read") || method.equalsIgnoreCase("query"))) {
            before = null;
            after = null;
        }

        // TODO: UPGRADE (Get the parent and the root context id)
        Context root = context.asContext(RootContext.class);
        Context parent = context.getParent();

        Map<String, Object> activity = new HashMap<String, Object>();
        activity.put(TIMESTAMP, dateUtil.now());
        activity.put(ACTION, method);
        activity.put(MESSAGE, message);
        activity.put(OBJECT_ID, objectId);
        activity.put(REVISION, rev);
        activity.put(ACTIVITY_ID, context.getId());
        activity.put(ROOT_ACTION_ID, root.getId());
        activity.put(PARENT_ACTION_ID, parent.getId());
        // TODO: UPGRADE (Get the Requester)
        //activity.put(REQUESTER, getRequester(request));
        activity.put(BEFORE, JsonUtil.jsonIsNull(before) ? null : before.getWrappedObject());
        activity.put(AFTER, JsonUtil.jsonIsNull(after) ? null : after.getWrappedObject());
        activity.put(STATUS, status == null ? null : status.toString());

        return activity;
    }
}
