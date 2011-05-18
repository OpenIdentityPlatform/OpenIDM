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

import java.util.HashMap;
import java.util.Map;

import org.forgerock.openidm.context.ContextMap;
import org.forgerock.openidm.context.InvokeContext;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.sync.SynchronizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityLog {
    final static Logger logger = LoggerFactory.getLogger(ActivityLog.class);
    
    public static void log(ObjectSet router, Action action, String message, String objectId, 
            Map before, Map after, Status status) throws ObjectSetException { 
        // TODO: convert to flyweight?
        try {
            String rev = null;
            if (after != null) {
                rev = (String) after.get("_rev");
            } else if (before != null) {
                rev = (String) before.get("_rev");
            }
            
            // TODO: make configurable
            if (Action.READ.equals(action)) {
                before = null;
                after = null;
            } else if (Action.QUERY.equals(action)) {
                before = null;
                after = null;
            }
            
            Map activity = new HashMap();
            activity.put("timestamp", new java.util.Date()); // TODO: handle date properly for JSON
            activity.put("action", action);
            activity.put("message", message);
            activity.put("objectId", objectId);
            activity.put("rev", rev);
            activity.put("rootActionId", InvokeContext.getContext().getFirstNested(ContextMap.NESTED_ACTIVITY_IDS));
            activity.put("parentActionId", InvokeContext.getContext().getLastNested(ContextMap.NESTED_ACTIVITY_IDS));
            activity.put("requester", InvokeContext.getContext().getRequester());
            activity.put("approver", InvokeContext.getContext().getApprover());
            //activity.put("principal", principal);
            activity.put("before", before);
            //activity.put("diffApplied", null);
            activity.put("after", after); // how can we know for system objects?
            activity.put("status", status);
            router.create("audit/activity", activity);
        } catch (ObjectSetException ex) {
            logger.warn("Failed to write activity log {}", ex);
            throw ex;
            // TODO: should this stop the activity itself?
        }
    }
}