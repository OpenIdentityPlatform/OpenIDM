package org.forgerock.openidm.repo.opendj.impl;/*
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.forgerock.guava.common.base.Strings;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.repo.util.TokenHandler;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A handler for a single type, eg "managed/user".
 */
public abstract class AbstractDJTypeHandler implements TypeHandler {
    final static Logger logger = LoggerFactory.getLogger(AbstractDJTypeHandler.class);

    /** Configured queries for this type */
    protected final Map<String, JsonValue> queries;

    protected static final ObjectMapper mapper = new ObjectMapper();

    /** The name of the resource in the rest2ldap config */
    protected final ResourcePath resourcePath;

    protected RouteEntry routeEntry;

    protected final RequestHandler handler;

    @Override
    public RouteEntry getRouteEntry() {
        return routeEntry;
    }

    @Override
    public void setRouteEntry(final RouteEntry routeEntry) {
        this.routeEntry = routeEntry;
    }

    /**
     * Create a new DJ type handler.
     *
     * @param resourcePath The path to this resource on {@code repoHandler}
     * @param repoHandler The request handler provided by rest2ldap for repo access
     * @param routeEntry The entry on the IDM router for this handler
     * @param config Configuration specific to this type handler
     * @param queries Configured queries for this resource
     */
    AbstractDJTypeHandler(final ResourcePath resourcePath, final RequestHandler repoHandler, final RouteEntry routeEntry, final JsonValue config, final JsonValue queries) {
        this.routeEntry = routeEntry;
        this.handler = repoHandler;
        this.resourcePath = resourcePath;

        this.queries = new HashMap<>();
        for (String queryId : queries.keys()) {
            this.queries.put(queryId, queries.get(queryId));
        }
    }


    /**
     * Transform a JsonValue prior to inputting it in to OpenDJ
     *
     * @param jsonValue The incoming value to transform
     * @return The transformed value to be placed in the repo
     * @throws ResourceException
     */
    abstract protected JsonValue inputTransformer(JsonValue jsonValue) throws ResourceException;

    /**
     * Transform JsonValue after it has been retrieved from the repo.
     *
     * @param jsonValue The value coming from the repo
     * @return The transformed value
     * @throws ResourceException
     */
    abstract protected JsonValue outputTransformer(JsonValue jsonValue) throws ResourceException;

    /**
     * Prepend the request with this type handlers {@link ResourcePath} in rest2ldap.
     *
     * TODO - this should be unnecessary once {@link OpenDJRepoService} can listen on repo/*
     *
     * @param r The request to modify
     * @return The incoming request with a modified resource path
     */
    protected <R extends Request> R prefixResourcePath(final R r) {
        return (R) r.setResourcePath(resourcePath.child(r.getResourcePathObject()));
    }

//    @Override
    protected String getResourceId(Request request) {
        final ResourcePath path = request.getResourcePathObject();

        // FIXME - this is a hack
        if (path.size() > 0) {
            return path.leaf();
        } else {
            return path.toString();
        }
    }

    /**
     * If the request has a queryId translate it to an appropriate queryFilter and place in request, otherwise
     * return request unchanged.
     *
     * @param request Query request
     * @return A new {@link QueryRequest} with a populated queryFilter or unchanged {@code request}
     */
    protected QueryRequest normalizeQueryRequest(final QueryRequest request) throws BadRequestException {
        if (request.getQueryId() == null || request.getQueryId().trim().isEmpty()) {
            return request;
        }
        final JsonValue queryConfig = queries.get(request.getQueryId());
        if (queryConfig == null) {
            throw new BadRequestException("Requested query " + request.getQueryId() + " does not exist");
        }
        final QueryRequest queryRequest = Requests.copyOfQueryRequest(request);
        queryRequest.setQueryId(null);

        // process sort keys
        final JsonValue sortKeys = queryConfig.get("_sortKeys");
        if (sortKeys.isString()) {
            try {
                queryRequest.addSortKey(sortKeys.asString().split(","));
            } catch (final IllegalArgumentException e) {
                throw new BadRequestException("The value '" + sortKeys + "' for parameter '_sortKeys' could not be"
                        + " parsed as a comma separated list of sort keys");
            }
        } else if (sortKeys.isList()) {
            queryRequest.addSortKey(sortKeys.asList().toArray(new String[sortKeys.size()]));
        }

        // process fields
        final JsonValue fields = queryConfig.get("_fields");
        if (fields.isString()) {
            try {
                queryRequest.addSortKey(fields.asString().split(","));
            } catch (final IllegalArgumentException e) {
                throw new BadRequestException("The value '" + fields + "' for parameter '_fields' could not be"
                        + " parsed as a comma separated list of fields");
            }
        } else if (fields.isList()) {
            queryRequest.addField(fields.asList().toArray(new String[fields.size()]));
        }

        // process queryFilter
        final String tokenizedFilter = queryConfig.get("_queryFilter").asString();

        final TokenHandler handler = new TokenHandler();
        final List<String> tokens = handler.extractTokens(tokenizedFilter);
        final Map<String, String> replacements = new HashMap<>();

        for (final String token : tokens) {
            final String param = queryRequest.getAdditionalParameter(token);
            if (param != null) {
                replacements.put(token, param);
            } else {
                throw new BadRequestException("Query expected additional parameter " + token);
            }
        }

        final String detokenized = handler.replaceTokensWithValues(tokenizedFilter, replacements);
        queryRequest.setQueryFilter(QueryFilters.parse(detokenized));
        return queryRequest;
    }

    final Function<ResourceResponse, ResourceResponse, ResourceException> transformOutput = new Function<ResourceResponse, ResourceResponse, ResourceException>() {
        @Override
        public ResourceResponse apply(ResourceResponse r) throws ResourceException {
            return Responses.newResourceResponse(r.getId(), r.getRevision(), outputTransformer(r.getContent()));
        }
    };

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
                                                                     final DeleteRequest deleteRequest) {
        return handler.handleDelete(context, prefixResourcePath(deleteRequest)).then(transformOutput);
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context, final ActionRequest request) {
        return handler.handleAction(context, prefixResourcePath(request)).then(new Function<ActionResponse, ActionResponse, ResourceException>() {
            @Override
            public ActionResponse apply(final ActionResponse value) throws ResourceException {
                return Responses.newActionResponse(outputTransformer(value.getJsonContent()));
            }
        });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
                                                                    final PatchRequest patchRequest) {
        return handler.handlePatch(context, prefixResourcePath(patchRequest)).then(transformOutput);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest readRequest) {
        return handler.handleRead(context, prefixResourcePath(readRequest)).then(transformOutput);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
                                                                     final UpdateRequest _updateRequest) {
        final UpdateRequest updateRequest = Requests.copyOfUpdateRequest(_updateRequest);

        try {
            updateRequest.setContent(inputTransformer(updateRequest.getContent()));
        } catch (ResourceException e) {
            return e.asPromise();
        }

        return handler.handleUpdate(context, prefixResourcePath(updateRequest)).then(transformOutput);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
                                                                     final CreateRequest _request) {
        final CreateRequest createRequest = Requests.copyOfCreateRequest(_request);

        try {
            Map<String, Object> obj = inputTransformer(createRequest.getContent()).asMap();

            // Set id to a new UUID if none is specified (_action=create)
            if (Strings.isNullOrEmpty(createRequest.getNewResourceId().trim())) {
                createRequest.setNewResourceId(UUID.randomUUID().toString());
            }

            obj.put("_id", createRequest.getNewResourceId());

            /*
             * XXX - all nulls are coming in as blank Strings. INVESTIGATE
             */

            Iterator<Map.Entry<String, Object>> iter = obj.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<String, Object> entry = iter.next();
                Object val = entry.getValue();

                if (val instanceof String && Strings.isNullOrEmpty((String) val)) {
                    iter.remove();
                }
            }

            createRequest.setContent(new JsonValue(obj));

            return handler.handleCreate(context, prefixResourcePath(createRequest)).then(transformOutput);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest _request, final QueryResourceHandler _handler) {
        try {
            // check for a queryId and if so convert it to a queryFilter
            final QueryRequest queryRequest = normalizeQueryRequest(_request);

            final ResourceException[] exception = new ResourceException[]{};

            // Create a proxy handler so we can run a transformer on results
            final QueryResourceHandler proxy = new QueryResourceHandler() {
                @Override
                public boolean handleResource(ResourceResponse resourceResponse) {
                    try {
                        return _handler.handleResource(transformOutput.apply(resourceResponse));
                    } catch (ResourceException e) {
                        exception[0] = e;
                        return false;
                    }
                }
            };

            if (exception.length > 0) {
                return exception[0].asPromise();
            } else {
                return handler.handleQuery(context, prefixResourcePath(queryRequest), proxy);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }
}
