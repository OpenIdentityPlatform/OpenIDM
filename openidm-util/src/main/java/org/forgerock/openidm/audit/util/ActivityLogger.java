/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;

/**
 * Create an audit activity log for a request result.
 *
 */
public interface ActivityLogger {

    /** the activity log timestamp field */
    public static final String TIMESTAMP = "timestamp";
    /** the activity log action field */
    public static final String ACTION = "action";
    /** the activity log message field */
    public static final String MESSAGE = "message";
    /** the activity log object id field */
    public static final String OBJECT_ID = "objectId";
    /** the activity log revision field */
    public static final String REVISION = "rev";
    /** the activity log activity id field */
    public static final String ACTIVITY_ID = "activityId";
    /** the activity log root action id field */
    public static final String ROOT_ACTION_ID = "rootActionId";
    /** the activity log parent action id field */
    public static final String PARENT_ACTION_ID = "parentActionId";
    /** the activity log requester field */
    public static final String REQUESTER = "requester";
    /** the activity log object before field */
    public static final String BEFORE = "before";
    /** the activity log object after field */
    public static final String AFTER = "after";
    /** the activity log status field */
    public static final String STATUS = "status";
    /** the activity log changed fields field */
    public static final String CHANGED_FIELDS = "changedFields";
    /** the activity log password changed field */
    public static final String PASSWORD_CHANGED = "passwordChanged";

    /**
     * Write an activity audit log.
     *
     * @param context the context of the request to log
     * @param requestType the type of request to log
     * @param message
     * @param objectId the resourceId being operated on
     * @param before the object value "before" the request
     * @param after the object value "after" the request
     * @param status the status of the operation
     * @throws ResourceException on failure to write to audit log unless suppressed.
     */
    void log(ServerContext context, RequestType requestType, String message, String objectId,
             JsonValue before, JsonValue after, Status status) throws ResourceException;
}
