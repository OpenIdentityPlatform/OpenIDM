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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.config.manage;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.audit.events.ConfigAuditEventBuilder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilizing the ConfigAuditEventBuilder this class will send log events of config changes to the commons
 * audit module.
 *
 * @see ConfigAuditEventBuilder
 */
public class ConfigAuditEventLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigAuditEventLogger.class);

    public static final String CONFIG_AUDIT_EVENT_NAME = "CONFIG";
    public static final String AUDIT_CONFIG_REST_PATH = "audit/config";
    public static final String ROOT_ID_PATH = "/" + ResourceResponse.FIELD_CONTENT_ID;

    /**
     * Calls buildAuditEvent() and invokes the request to the audit path.
     *
     * @param connectionFactory factory used to make crest call to audit service.
     */
    public final Promise<ResourceResponse, ResourceException> log(ConfigAuditState configAuditState, Request request,
            Context context, ConnectionFactory connectionFactory) {
        try {
            // Build the event utilizing the config builder.
            final AuditEvent auditEvent = ConfigAuditEventBuilder.configEvent()
                    .operationFromCrestRequest(request)
                    .userId(getUserId(context))
                    .runAs(getUserId(context))
                    .transactionIdFromContext(context)
                    .revision(configAuditState.getRevision())
                    .timestamp(System.currentTimeMillis())
                    .objectId(configAuditState.getId())
                    .eventName(CONFIG_AUDIT_EVENT_NAME)
                    .before(configAuditState.getBefore())
                    .after(configAuditState.getAfter())
                    .changedFields(getChangedFields(
                            configAuditState.getBefore(),
                            configAuditState.getAfter(),
                            request.getRequestType()))
                    .toEvent();

            return connectionFactory.getConnection().create(context,
                    Requests.newCreateRequest(AUDIT_CONFIG_REST_PATH, auditEvent.getValue())).asPromise();
        } catch (ResourceException e) {
            LOGGER.error("had trouble logging audit event for config changes.", e);
            return e.asPromise();
        } catch (Exception e) {
            LOGGER.error("had trouble logging audit event for config changes.", e);
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    private String[] getChangedFields(JsonValue before, JsonValue after, RequestType requestType) {
        if (RequestType.READ.equals(requestType) || RequestType.QUERY.equals(requestType)) {
            // if reading or query there are no changed fields
            return new String[0];
        }

        if (null == before && null == after) {
            return new String[0];
        }
        // Default to empty json when they are null.
        if (after == null) {
            after = json(object());
        }
        if (before == null) {
            before = json(object());
        }
        // Now find the changed fields.
        List<String> changedFields = new ArrayList<>();
        for (JsonValue change : JsonPatch.diff(before, after)) {
            String diff = change.get(JsonPatch.PATH_PTR).asString();
            // Config objects require id only for repo storage and not on the filesystem.
            // As far as reporting changes to a config, it is not relevant to be reported as changes.
            if (!ROOT_ID_PATH.equals(diff)) {
                changedFields.add(diff);
            }
        }
        return changedFields.toArray(new String[changedFields.size()]);
    }

    private String getUserId(final Context context) {
        return context.containsContext(SecurityContext.class)
                ? context.asContext(SecurityContext.class).getAuthenticationId()
                : null;
    }

}
