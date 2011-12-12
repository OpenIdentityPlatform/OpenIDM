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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.audit.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.openidm.sync.SynchronizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

// JSON Resource
import org.forgerock.json.resource.JsonResourceContext;

// Deprecated
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;

public class ActivityLog {
    final static Logger logger = LoggerFactory.getLogger(ActivityLog.class);
    
    // TODO: replace with proper formatter
    final static SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static String getRequester(JsonValue request) {
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

    public static void log(ObjectSet router, JsonValue request, String message, String objectId, 
     JsonValue before, JsonValue after, Status status) throws ObjectSetException {
        if (request == null) {
            request = new JsonValue(null);
        }
        // TODO: convert to flyweight?
        try {
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

            Map<String,Object> activity = new HashMap<String,Object>();
            activity.put("timestamp", isoFormatter.format(new Date())); 
            activity.put("action", request.get("method").getObject());
            activity.put("message", message);
            activity.put("objectId", objectId);
            activity.put("rev", rev);
// TODO: Change schema to support the id of the request itself, commented-out below:
//            activity.put("activityId", request.get("uuid").getObject());
            activity.put("rootActionId", root.get("uuid").getObject());
            activity.put("parentActionId", parent.get("uuid").getObject());
            activity.put("requester", getRequester(request));
            activity.put("before", before == null ? null : before.getObject());
            activity.put("after", after == null ? null : after.getObject()); // how can we know for system objects?
            activity.put("status", status == null ? null : status.toString());
            router.create("audit/activity", activity);
        } catch (ObjectSetException ex) {
            logger.warn("Failed to write activity log {}", ex);
            throw ex;
            // TODO: should this stop the activity itself?
        }
    }
}
