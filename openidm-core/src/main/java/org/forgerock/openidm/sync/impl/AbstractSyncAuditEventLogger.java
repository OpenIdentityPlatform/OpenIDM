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
 * Portions copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.openidm.audit.util.Status;

/**
 * Base Template for sync related audit events.
 */
public abstract class AbstractSyncAuditEventLogger<T extends AbstractSyncAuditEventBuilder<T>> {

    private final ObjectMapping.SyncOperation syncOperation;

    private Context context;
    private Exception exception;
    private String message;
    private JsonValue messageDetail;
    private String linkQualifier;
    private String mapping;
    private String sourceObjectId;
    private Status status;
    private String targetObjectId;

    /**
     * Base constructor for sync and recon audit event logs. Context is required to contain a SecurityContext.
     *
     * @param syncOperation the operation that holds the action and
     * @param mapping
     * @param context
     */
    public AbstractSyncAuditEventLogger(ObjectMapping.SyncOperation syncOperation, String mapping,
            Context context) {

        this.syncOperation = syncOperation;
        this.mapping = mapping;
        this.context = context;

        // The context must hold a Security context.
        if (!context.containsContext(SecurityContext.class)) {
            throw new IllegalArgumentException(
                    "Context is required to have a SecurityContext in order to log an audit event.");
        }
    }

    /**
     * Simple setter.
     *
     * @param exception
     */
    public void setException(Exception exception) {
        this.exception = exception;
    }

    /**
     * Returns the message that was previously set.
     *
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * Simple setter.
     *
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Simple setter.
     *
     * @param messageDetail
     */
    public void setMessageDetail(JsonValue messageDetail) {
        this.messageDetail = messageDetail;
    }

    /**
     * Simple setter.
     *
     * @param linkQualifier
     */
    public void setLinkQualifier(String linkQualifier) {
        this.linkQualifier = linkQualifier;
    }

    /**
     * Simple setter.
     *
     * @param mapping
     */
    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    /**
     * Simple setter.
     *
     * @param sourceObjectId
     */
    public void setSourceObjectId(String sourceObjectId) {
        this.sourceObjectId = sourceObjectId;
    }

    /**
     * Simple setter.
     *
     * @param status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Simpler setter.
     *
     * @param targetObjectId
     */
    public void setTargetObjectId(String targetObjectId) {
        this.targetObjectId = targetObjectId;
    }

    /**
     * Implementors should return the REST path to the audit log end points.
     *
     * @return the REST path to audit.
     */
    protected abstract String getAuditPath();

    /**
     * Implementors should return the event name for this audit log event.
     *
     * @return the event name
     */
    protected abstract String getEventName();

    /**
     * Implementors should return the proper AbstractSyncAuditEventBuilder for this logger.
     *
     * @return The builder to be used to make the log audit event.
     */
    protected abstract T getEventBuilder();

    /**
     * Implementors should utilize the builder and apply the custom fields.
     *
     * @param builder The builder to customize.
     * @return
     */
    protected abstract T applyCustomFields(T builder);

    /**
     * Calls buildAuditEvent() and invokes the request to the audit path.
     *
     * @param connectionFactory
     * @throws ResourceException
     */
    public final void log(ConnectionFactory connectionFactory) throws ResourceException {

        try {
            T eventBuilder = getEventBuilder()
                    .transactionIdFromRootContext(context)
                    .timestamp(System.currentTimeMillis())
                    .eventName(getEventName())
                    .authenticationFromSecurityContext(context)
                    .action(null != syncOperation ? syncOperation.action : null)
                    .exception(exception)
                    .linkQualifier(linkQualifier)
                    .mapping(mapping)
                    .message(message)
                    .messageDetail(messageDetail)
                    .situation(null != syncOperation ? syncOperation.situation : null)
                    .sourceObjectId(sourceObjectId)
                    .status(status)
                    .targetObjectId(targetObjectId);

            AuditEvent auditEvent = applyCustomFields(eventBuilder).toEvent();

            connectionFactory.getConnection().create(context,
                    Requests.newCreateRequest(getAuditPath(), auditEvent.getValue()));
        } catch (ResourceException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}
