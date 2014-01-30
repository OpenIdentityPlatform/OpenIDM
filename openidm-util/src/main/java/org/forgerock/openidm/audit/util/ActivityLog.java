/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityLog {

    /**
     * Setup logging for the {@link ActivityLog}.
     */
    final static Logger logger = LoggerFactory.getLogger(ActivityLog.class);

    private final static boolean suspendException;
    private static DateUtil dateUtil;

    public static final String TIMESTAMP = "timestamp";
    public static final String ACTION = "action";
    public static final String MESSAGE = "message";
    public static final String OBJECT_ID = "objectId";
    public static final String REVISION = "rev";
    public static final String ACTIVITY_ID = "activityId";
    public static final String ROOT_ACTION_ID = "rootActionId";
    public static final String PARENT_ACTION_ID = "parentActionId";
    public static final String REQUESTER = "requester";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String STATUS = "status";
    public static final String CHANGED_FIELDS = "changedFields";
    public static final String PASSWORD_CHANGED = "passwordChanged";

    /**
     * Creates a Jackson object mapper. By default, it calls
     * {@link org.codehaus.jackson.map.ObjectMapper#ObjectMapper()}.
     * 
     */
    static {
        String config = IdentityServer.getInstance().getProperty(ActivityLog.class.getName().toLowerCase());
        suspendException = "suspend".equals(config);
        // TODO Allow for configured dateUtil
        dateUtil = DateUtil.getDateUtil("UTC");
    }

    public static String getRequester(Context context) {
        SecurityContext securityContext = context.asContext(SecurityContext.class);
        return securityContext != null
                ? securityContext.getAuthenticationId()
                : null;
    }

    public static void log(ConnectionFactory connectionFactory, ServerContext context, RequestType requestType, String message, String objectId,
                           JsonValue before, JsonValue after, Status status) throws ResourceException {
        if (requestType == null) {
            throw new NullPointerException("Request can not be null when audit.");
        }
        // TODO: convert to flyweight?
        try {
            Map<String, Object> activity = buildLog(context, requestType, message, objectId, before, after, status);
            connectionFactory.getConnection().create(new ServerContext(context),
                    Requests.newCreateRequest("audit/activity", new JsonValue(activity)));
        } catch (ResourceException ex) {
            logger.warn("Failed to write activity log {}", ex);
            if (!suspendException) {
                throw ex;
                // TODO: should this stop the activity itself?
            }
        }
    }

    private static Map<String, Object> buildLog(Context context, RequestType requestType, String message,
            String objectId, JsonValue before, JsonValue after, Status status) {
        String rev = null;
        if (after != null && after.get(Resource.FIELD_CONTENT_REVISION).isString()) {
            rev = after.get(Resource.FIELD_CONTENT_REVISION).asString();
        } else if (before != null && before.get(Resource.FIELD_CONTENT_REVISION).isString()) {
            rev = before.get(Resource.FIELD_CONTENT_REVISION).asString();
        }

        String method = requestType.name();

        // TODO: make configurable
        if (method != null
                && (RequestType.READ.equals(requestType) || RequestType.QUERY.equals(requestType))) {
            before = null;
            after = null;
        }

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
        activity.put(REQUESTER, getRequester(context));
        activity.put(BEFORE, JsonUtil.jsonIsNull(before) ? null : before.getWrappedObject());
        activity.put(AFTER, JsonUtil.jsonIsNull(after) ? null : after.getWrappedObject());
        activity.put(STATUS, status == null ? null : status.toString());

        return activity;
    }
}
