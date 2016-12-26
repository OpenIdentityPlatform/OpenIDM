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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.audit.impl;

import static org.forgerock.json.resource.Requests.copyOfQueryRequest;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.query.QueryFilter.equalTo;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.audit.Audit;
import org.forgerock.audit.AuditingContext;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.query.FieldTransformerQueryFilterVisitor;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit event handler that logs to a router target.
 */
public class RouterAuditEventHandler extends AuditEventHandlerBase {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(RouterAuditEventHandler.class);
    static final String EVENT_ID = "eventId";

    /** router target resource path */
    private final ResourcePath resourcePath;

    /** the DependencyProvider to provide access to the ConnectionFactory */
    private final ConnectionFactory connectionFactory;

    private static final FieldTransformerQueryFilterVisitor<JsonPointer> objectIdVisitor = new FieldTransformerQueryFilterVisitor<>(new org.forgerock.guava.common.base.Function<JsonPointer, JsonPointer>() {
        private final JsonPointer idPointer = new JsonPointer(ResourceResponse.FIELD_CONTENT_ID);
        private final JsonPointer eventIdPointer = new JsonPointer(RouterAuditEventHandler.EVENT_ID);

        @Override
        public JsonPointer apply(JsonPointer field) {
            return (idPointer.equals(field)) ? eventIdPointer : field;
        }
    });


    @Inject
    public RouterAuditEventHandler(
            final RouterAuditEventHandlerConfiguration configuration,
            final EventTopicsMetaData eventTopicsMetaData,
            @Audit final ConnectionFactory connectionFactory) {
        super(configuration.getName(), eventTopicsMetaData, configuration.getTopics(), configuration.isEnabled());
        this.resourcePath = ResourcePath.valueOf(configuration.getResourcePath());
        this.connectionFactory = connectionFactory;
        logger.info("Audit logging to: {}", resourcePath.toString());
    }

    @Override
    public void startup() throws ResourceException {
        // do nothing
    }

    @Override
    public void shutdown() throws ResourceException {
        // do nothing
    }

    @Override
    public Promise<ResourceResponse, ResourceException> publishEvent(final Context context, final String auditEventTopic,
            final JsonValue auditEventContent) {
        try {
            JsonValue routerAuditContent = auditEventContent.copy();
            final String auditEventId = auditEventContent.get(ResourceResponse.FIELD_CONTENT_ID).asString();
            routerAuditContent.put(EVENT_ID, auditEventId);

            return connectionFactory.getConnection()
                    .createAsync(new AuditingContext(context),
                            newCreateRequest(resourcePath.concat(auditEventTopic), null, routerAuditContent))
                    .then(new Function<ResourceResponse, ResourceResponse, ResourceException>() {
                                @Override
                                public ResourceResponse apply(ResourceResponse response) throws ResourceException {
                                    return newResourceResponse(auditEventId, null, auditEventContent);
                                }
                            }
                    );

        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readEvent(final Context context, final String auditEventTopic,
            final String auditEventId) {
        QueryRequest queryRequest = newQueryRequest(auditEventTopic)
                .setQueryFilter(equalTo(new JsonPointer(EVENT_ID), auditEventId))
                .setPageSize(1);

        final List<ResourceResponse> responses = new ArrayList<>();
        return queryEvents(context, auditEventTopic, queryRequest, new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resource) {
                responses.add(resource);
                return true;
            }
        }).then(new Function<QueryResponse, ResourceResponse, ResourceException>() {
            @Override
            public ResourceResponse apply(QueryResponse value) throws ResourceException {
                if (responses.isEmpty()) {
                    throw new NotFoundException("Audit Event with objectId '" + auditEventId + "' was not found.");
                }
                return newResourceResponse(responses.get(0).getId(),
                        responses.get(0).getRevision(),
                        responses.get(0).getContent());
            }
        });
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryEvents(final Context context, final String auditEventTopic,
            final QueryRequest queryRequest, final QueryResourceHandler queryResourceHandler) {
        try {
            final QueryRequest newRequest = copyOfQueryRequest(queryRequest);
            newRequest.setResourcePath(resourcePath.concat(queryRequest.getResourcePathObject()));

            // Convert the possible expression to a filter, otherwise extract the current filter, if there is one.
            QueryFilter<JsonPointer> queryFilter = (queryRequest.getQueryExpression() != null)
                    ? QueryFilters.parse(queryRequest.getQueryExpression())
                    : queryRequest.getQueryFilter();

            if (null != queryFilter) {
                // Convert any references to "_id" to "objectId"
                newRequest.setQueryFilter(queryFilter.accept(objectIdVisitor, null));
                newRequest.setQueryExpression(null);
            }

            return newResultPromise(
                    connectionFactory.getConnection().query(new AuditingContext(context), newRequest,
                            new QueryResourceHandler() {
                                @Override
                                public boolean handleResource(ResourceResponse resourceResponse) {
                                    return queryResourceHandler.handleResource(adaptResponse(resourceResponse));
                                }
                            }));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /**
     * Replaces the route's response _id field with that of the eventId field; the id of the audit event.
     *
     * @param resourceResponse the response to adapt the content.
     * @return the response with the modified content.
     */
    private ResourceResponse adaptResponse(ResourceResponse resourceResponse) {
        JsonValue routerContent = resourceResponse.getContent().copy();
        String id = routerContent.get(EVENT_ID).asString();
        if (null != id) {
            routerContent.put(FIELD_CONTENT_ID, id);
            routerContent.remove(EVENT_ID);
            return newResourceResponse(id, resourceResponse.getRevision(), routerContent);
        } else {
            return resourceResponse;
        }
    }

}
