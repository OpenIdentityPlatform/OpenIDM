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
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openidm.repo.opendj.impl;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.query.QueryFilter.and;
import static org.forgerock.util.query.QueryFilter.equalTo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.forgerock.guava.common.collect.ObjectArrays;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.query.FieldTransformerQueryFilterVisitor;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type handler for generic objects that simply places all properties in a {@code fullobject} JSON field.
 */
public class GenericDJTypeHandler extends ExplicitDJTypeHandler {
    private static final Logger logger = LoggerFactory.getLogger(GenericDJTypeHandler.class);

    private static final String OBJECT_TYPE = "objecttype";

    /**
     * Non-generic properties. Currently only containing _id and _rev.
     * Can be used in the future to facilitate hybrid objects (both explicit and properties)
     */
    private final Set<String> explicitProperties;

    /**
     * QueryFilterVisitor that prefixes generic attributes with /fullobject to match DJ schema.
     */
    final FieldTransformerQueryFilterVisitor<Void, JsonPointer> transformer =
            new FieldTransformerQueryFilterVisitor<Void, JsonPointer>() {
        @Override
        protected JsonPointer transform(Void param, JsonPointer ptr) {
            if (ptr.isEmpty() || ptr.get(0).equals(OBJECT_TYPE) || explicitProperties.contains(ptr.get(0))) {
                return ptr;
            } else {
                // generic field, prepend
                return new JsonPointer(ObjectArrays.concat("fullobject", ptr.toArray()));
            }
        }
    };

    final Function<ResourceResponse, ResourceResponse, ResourceException> transformOutput =
            new Function<ResourceResponse, ResourceResponse, ResourceException>() {
                @Override
                public ResourceResponse apply(ResourceResponse r) {
                    return Responses.newResourceResponse(r.getId(), r.getRevision(), outputTransformer(r.getContent()));
                }
            };


    /**
     * Create a new generic DJ type handler.
     *
     * This handler simply places all object properties within the {@code fullobject} JSON field.
     *
     * @param repoResource The path to this resource on {@code repoHandler}
     * @param repoHandler The request handler provided by rest2ldap for repo access
     * @param config Configuration specific to this type handler
     * @param queries Configured queries for this resource
     * @param commands Configured commands for this resource
     *
     * @see ExplicitDJTypeHandler#ExplicitDJTypeHandler(ResourcePath, RequestHandler, JsonValue, JsonValue, JsonValue)
     */
    GenericDJTypeHandler(final ResourcePath repoResource, final RequestHandler repoHandler, final JsonValue config, final JsonValue queries, final JsonValue commands) {
        super(repoResource, repoHandler, config, queries, commands);

        this.explicitProperties = new HashSet<>();
        this.explicitProperties.add("_id");
        this.explicitProperties.add("_rev");
    }

    private JsonValue inputTransformer(final JsonValue jsonValue, final String type) {
        final JsonValue output = json(object());
        final JsonValue fullobject = jsonValue.clone();
        output.put("fullobject", fullobject);
        output.put(OBJECT_TYPE, type);

        for (final String prop : explicitProperties) {
            output.put(prop, fullobject.get(prop).getObject());
            fullobject.remove(prop);
        }

        return output;
    }

    protected JsonValue outputTransformer(final JsonValue jsonValue) {
        final JsonValue fullobject = jsonValue.get("fullobject");
        final JsonValue output;

        if (fullobject.isNull()) {
            // If no fullobject is present (not in _fields) simply pass the object back
            output = json(object());
        } else {
            output = fullobject.clone();
        }

        for (final String prop : explicitProperties) {
            output.put(prop, jsonValue.get(prop).getObject());
        }

        return output;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest deleteRequest) {
        return super.handleDelete(context, deleteRequest).then(transformOutput);
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return super.handleAction(context, request).then(
                new Function<ActionResponse, ActionResponse, ResourceException>() {
                    @Override
                    public ActionResponse apply(final ActionResponse value) throws ResourceException {
                        return Responses.newActionResponse(outputTransformer(value.getJsonContent()));
                    }
                });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
                                                                     final UpdateRequest _updateRequest) {
        final UpdateRequest updateRequest = Requests.copyOfUpdateRequest(_updateRequest);
        final String id = updateRequest.getResourcePathObject().leaf();
        final String type = updateRequest.getResourcePathObject().parent().toString();

        updateRequest.setResourcePath(repoResource.concat(id));
        updateRequest.setContent(inputTransformer(updateRequest.getContent(), type));

        return super.handleUpdate(context, updateRequest).then(transformOutput);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
                                                                     final CreateRequest _request) {
        final CreateRequest createRequest = Requests.copyOfCreateRequest(_request);
        final String type = _request.getResourcePath();

        createRequest.setResourcePath(this.repoResource);
        createRequest.setContent(inputTransformer(_request.getContent(), type));

        return super.handleCreate(context, createRequest).then(transformOutput);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest readRequest) {
        final QueryRequest queryRequest = Requests.newQueryRequest(this.repoResource);
        final String resourceId = readRequest.getResourcePathObject().leaf();
        final String type = readRequest.getResourcePathObject().parent().toString();
        readRequest.setResourcePath(this.repoResource.child(resourceId));

        queryRequest.setQueryFilter(and(
                equalTo(new JsonPointer("_id"), resourceId),
                equalTo(new JsonPointer(OBJECT_TYPE), type)));

        final List<ResourceResponse> responses = new ArrayList<>();

        try {
            super.handleQuery(context, queryRequest, new QueryResourceHandler() {
                @Override
                public boolean handleResource(final ResourceResponse r) {
                    responses.add(Responses.newResourceResponse(r.getId(), r.getRevision(), outputTransformer(r.getContent())));

                    return false;
                }
            }).get(); // must block with get so empty check occurs after results have been retrieved
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (responses.isEmpty()) {
            return new NotFoundException(
                    String.format("Object %s not found in %s", resourceId, type)).asPromise();
        } else {
            return responses.get(0).asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest _request, final QueryResourceHandler handler) {
        final QueryRequest request;

        try {
            // Normalize request before sending to super so we can amend the filter for objecttype
            request = normalizeQueryRequest(_request);
        } catch (BadRequestException e) {
            logger.error("Failed to normalize query", e);
            return e.asPromise();
        }

        request.setResourcePath(this.repoResource);

        final QueryFilter<JsonPointer> originalFilter = request.getQueryFilter();
        final String type = request.getResourcePath();
        final QueryFilter<JsonPointer> typeFilter = equalTo(new JsonPointer(OBJECT_TYPE), type);

        if (originalFilter == null) {
            request.setQueryFilter(typeFilter);
        } else {
            // Prefix non-explicit fields with /fullobject
            final QueryFilter<JsonPointer> fullObjectPrefixed = originalFilter.accept(transformer, null);
            // append type filter
            request.setQueryFilter(and(fullObjectPrefixed, typeFilter));
        }

        // Create a proxy handler so we can run a transformer on results
        final QueryResourceHandler transformerHandler = new QueryResourceHandler() {
            @Override
            public boolean handleResource(final ResourceResponse r) {
                return handler.handleResource(Responses.newResourceResponse(
                        r.getId(), r.getRevision(), outputTransformer(r.getContent())));
            }
        };

        return super.handleQuery(context, request, transformerHandler);
    }
}
