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
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.audit.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

// JSON Resource
import org.forgerock.json.resource.JsonResourceContext;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceException;

// Deprecated
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;

public class ActivityLog {
    final static Logger logger = LoggerFactory.getLogger(ActivityLog.class);

    private final static boolean suspendException;
    private final static ObjectMapper mapper;

    /**
     * Creates a Jackson object mapper. By default, it
     * calls {@link org.codehaus.jackson.map.ObjectMapper#ObjectMapper()}.
     *
     */
    static {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        mapper = new ObjectMapper(jsonFactory);
        String config = IdentityServer.getInstance().getProperty(ActivityLog.class.getName().toLowerCase());
        suspendException = "suspend".equals(config);
    }


    public static String getRequester(JsonValue request) {
        String result = null;
        while (request != null && !request.isNull()) {
            JsonValue user = request.get("security").get("user");
            if (user.isString()) {
                result = user.asString();
                break;
            }
            request = request.get("parent");
        }
        return result;
    }

    public static void log(JsonResource router, JsonValue request, String message, String objectId,
                           JsonValue before, JsonValue after, Status status) throws JsonResourceException {
        if (request == null) {
            request = new JsonValue(null);
        }
        // TODO: convert to flyweight?
        try {
            Map<String, Object> activity = buildLog(request, message, objectId, before, after, status);
            JsonResourceAccessor accessor = new JsonResourceAccessor(router, JsonResourceContext.getParentContext(request));
            accessor.create("audit/activity", new JsonValue(activity));
        } catch (JsonResourceException ex) {
            logger.warn("Failed to write activity log {}", ex);
            if (!suspendException) {
                throw ex;
                // TODO: should this stop the activity itself?
            }
        }
    }


    public static void log(ObjectSet router, JsonValue request, String message, String objectId,
                           JsonValue before, JsonValue after, Status status) throws ObjectSetException {
        if (request == null) {
            request = new JsonValue(null);
        }
        // TODO: convert to flyweight?
        try {
            Map<String, Object> activity = buildLog(request, message, objectId, before, after, status);
            router.create("audit/activity", activity);
        } catch (ObjectSetException ex) {
            logger.warn("Failed to write activity log {}", ex);
            if (!suspendException) {
                throw ex;
                // TODO: should this stop the activity itself?
            }
        }
    }

    private static Map<String, Object> buildLog(JsonValue request, String message, String objectId, JsonValue before, JsonValue after, Status status) {
        String rev = null;
        if (after != null && after.get("_rev").isString()) {
            rev = after.get("_rev").asString();
        } else if (before != null && before.get("_rev").isString()) {
            rev = before.get("_rev").asString();
        }

        String method;
        try {
            method = request.get("method").asString();
        } catch (JsonValueException jve) {
            method = null;
        }
        // TODO: make configurable
        if (method != null && (method.equalsIgnoreCase("read") || method.equalsIgnoreCase("query"))) {
            before = null;
            after = null;
        }

        JsonValue root = JsonResourceContext.getRootContext(request);
        JsonValue parent = JsonResourceContext.getParentContext(request);

        Map<String, Object> activity = new HashMap<String, Object>();
        activity.put("timestamp", DateUtil.now());
        activity.put("action", request.get("method").getObject());
        activity.put("message", message);
        activity.put("objectId", objectId);
        activity.put("rev", rev);
        activity.put("activityId", request.get("uuid").getObject());
        activity.put("rootActionId", root.get("uuid").getObject());
        activity.put("parentActionId", parent.get("uuid").getObject());
        activity.put("requester", getRequester(request));
        try {
            activity.put("before", (before == null || before.isNull()) ? null : mapper.writeValueAsString(before.getObject()));
        } catch (IOException e) {
            activity.put("before", (before == null || before.isNull()) ? null : before.getObject().toString());
        }
        try {
            activity.put("after", (after == null || after.isNull()) ? null : mapper.writeValueAsString(after.getObject())); // how can we know for system objects?
        } catch (IOException e) {
            activity.put("after", (after == null || after.isNull()) ? null : after.getObject().toString()); // how can we know for system objects?
        }
        activity.put("status", status == null ? null : status.toString());
        return activity;
    }
}
