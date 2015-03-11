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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an audit activity log message using the router.
 */
public class RouterActivityLogger implements ActivityLogger {

    /**
     * Setup logging for the {@link ActivityLogger}.
     */
    final static Logger logger = LoggerFactory.getLogger(RouterActivityLogger.class);

    private final ConnectionFactory connectionFactory;
    private final boolean suspendException;
    private final boolean logFullObjects;
    private final DateUtil dateUtil;

    /**
     * Creates an AuditLogger to create activity messages on the router.
     *
     * @param connectionFactory The {@link ConnectionFactory} to use.
     */
    public RouterActivityLogger(ConnectionFactory connectionFactory) {
        this(connectionFactory,
                "suspend".equals(IdentityServer.getInstance().getProperty(ActivityLogger.class.getName().toLowerCase())));
    }

    /**
     * Creates an AuditLogger to create activity messages on the router.
     *
     * @param connectionFactory The {@link ConnectionFactory} to use.
     * @param suspendException whether to throw Exceptions on failure to log or not
     */
    public RouterActivityLogger(ConnectionFactory connectionFactory, boolean suspendException) {
        this.connectionFactory = connectionFactory;
        this.suspendException = suspendException;
        this.logFullObjects = Boolean.valueOf(IdentityServer.getInstance().getProperty("openidm.audit.logFullObjects", "false"));
        // TODO Allow for configured dateUtil
        dateUtil = DateUtil.getDateUtil("UTC");
    }

    private String getRequester(Context context) {
        return context.containsContext(SecurityContext.class)
            ? context.asContext(SecurityContext.class).getAuthenticationId()
            : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void log(ServerContext context, RequestType requestType, String message, String objectId,
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
            // TODO: should this stop the activity itself?
            if (suspendException) {
                // log on exception if we're suspending the exception-propagation
                logger.warn("Failed to write activity log {}", ex);
            } else {
                // let the caller handle/log the exception
                throw ex;
            }
        }
    }

    private Map<String, Object> buildLog(Context context, RequestType requestType, String message,
            String objectId, JsonValue before, JsonValue after, Status status) {
        String rev = null;
        if (after != null && after.get(Resource.FIELD_CONTENT_REVISION).isString()) {
            rev = after.get(Resource.FIELD_CONTENT_REVISION).asString();
        } else if (before != null && before.get(Resource.FIELD_CONTENT_REVISION).isString()) {
            rev = before.get(Resource.FIELD_CONTENT_REVISION).asString();
        }

        String method = requestType.name().toLowerCase();

        if (method != null
                && !logFullObjects
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
        activity.put(BEFORE, before == null ? null : before.getObject());
        activity.put(AFTER, after == null ? null : after.getObject());
        activity.put(STATUS, status == null ? null : status.toString());

        return activity;
    }
}
