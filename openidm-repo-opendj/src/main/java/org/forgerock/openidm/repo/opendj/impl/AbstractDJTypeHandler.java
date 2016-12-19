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
 * Copyright 2015-2017 ForgeRock AS.
 */
package org.forgerock.openidm.repo.opendj.impl;

import static org.forgerock.guava.common.base.Strings.isNullOrEmpty;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValueFunctions.pointer;
import static org.forgerock.json.resource.Responses.newActionResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueFunctions;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
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
import org.forgerock.services.context.RootContext;
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

    private static final String ID = "_id";
    private static final String FIELDS = "_fields";
    private static final String SORT_KEYS = "_sortKeys";
    private static final String QUERY_FILTER = "_queryFilter";
    private static final String OPERATION = "operation";
    private static final String DELETE_OPERATION = "DELETE";
    private static final String ACTION_COMMAND = "command";
    static final String UNIQUE_CONSTRAINTS = "uniqueConstraints";

    /** Configured queries for this type */
    protected final Map<String, JsonValue> queries;

    /** Configured commands for this type */
    protected final Map<String, JsonValue> commands;

    protected static final ObjectMapper mapper = new ObjectMapper();

    /** The name of the resource in the rest2ldap config */
    protected final ResourcePath resourcePath;

    protected RouteEntry routeEntry;

    protected final RequestHandler handler;

    private final UniqueAttributeResolver uniqueAttributeResolver;

    /**
     * Create a new DJ type handler.
     *
     * @param resourcePath The path to this resource on {@code repoHandler}
     * @param repoHandler The request handler provided by rest2ldap for repo access
     * @param routeEntry The entry on the IDM router for this handler
     * @param config Configuration specific to this type handler
     * @param queries Configured queries for this resource
     * @param commands Configured commands for this resource
     */
    AbstractDJTypeHandler(final ResourcePath resourcePath, final RequestHandler repoHandler,
            final RouteEntry routeEntry, final JsonValue config, final JsonValue queries, final JsonValue commands) {
        this.routeEntry = routeEntry;
        this.handler = repoHandler;
        this.resourcePath = resourcePath;

        this.queries = new HashMap<>();
        for (final String queryId : queries.keys()) {
            final JsonValue query = queries.get(queryId);
            validateQuery(queryId, query);
            this.queries.put(queryId, query);
        }

        this.commands = new HashMap<>();
        if (commands.isNotNull()) {
            for (final String commandId : commands.keys()) {
                final JsonValue command = commands.get(commandId);
                validateCommand(commandId, command);
                this.commands.put(commandId, command);
            }
        }

        final List<List<JsonPointer>> uniqueConstraints = new LinkedList<>();
        for (final JsonValue uniqueConstraint : config.get(UNIQUE_CONSTRAINTS)) {
            uniqueConstraints.add(uniqueConstraint.as(JsonValueFunctions.listOf(pointer())));
        }

        this.uniqueAttributeResolver = new UniqueAttributeResolver(uniqueConstraints, handler, resourcePath);
    }

    private void validateQuery(final String queryId, final JsonValue query) {
        if (isNullOrEmpty(query.get(QUERY_FILTER).asString())) {
            throw new IllegalStateException("query missing '" + QUERY_FILTER + "' field: " + queryId);
        }
    }

    private void validateCommand(final String commandId, final JsonValue command) {
        final String operation = command.get(OPERATION).asString();
        if (isNullOrEmpty(operation)) {
            throw new IllegalStateException("command missing '" + OPERATION + "' field: " + commandId);
        }
        if (DELETE_OPERATION.equalsIgnoreCase(operation)) {
            if (isNullOrEmpty(command.get(QUERY_FILTER).asString())) {
                throw new IllegalStateException("command missing '" + QUERY_FILTER + "' field: " + commandId);
            }
        } else {
            throw new IllegalStateException("command operation '" + operation + "' unsupported: " + commandId);
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
     * If the request has a queryId translate it to an appropriate queryFilter and place in request, otherwise
     * return request unchanged.
     *
     * @param request Query request
     * @return A new {@link QueryRequest} with a populated queryFilter or unchanged {@code request}
     */
    protected QueryRequest normalizeQueryRequest(final QueryRequest request) throws BadRequestException {
        if (isNullOrEmpty(request.getQueryId())) {
            return request;
        }
        final JsonValue queryConfig = queries.get(request.getQueryId());
        if (queryConfig == null) {
            throw new BadRequestException("Requested query " + request.getQueryId() + " does not exist");
        }
        final QueryRequest queryRequest = Requests.copyOfQueryRequest(request);
        queryRequest.setQueryId(null);

        // process sort keys
        final JsonValue sortKeys = queryConfig.get(SORT_KEYS);
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
        final JsonValue fields = queryConfig.get(FIELDS);
        if (fields.isString()) {
            try {
                queryRequest.addField(fields.asString().split(","));
            } catch (final IllegalArgumentException e) {
                throw new BadRequestException("The value '" + fields + "' for parameter '_fields' could not be"
                        + " parsed as a comma separated list of fields");
            }
        } else if (fields.isList()) {
            queryRequest.addField(fields.asList().toArray(new String[fields.size()]));
        }

        // process queryFilter
        final String tokenizedFilter = queryConfig.get(QUERY_FILTER).asString();

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
        return handler.handleDelete(context, deleteRequest).then(transformOutput);
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context, final ActionRequest request) {
        if (ACTION_COMMAND.equalsIgnoreCase(request.getAction())) {
            final String commandId = request.getAdditionalParameters().get("commandId");
            if (isNullOrEmpty(commandId)) {
                return new BadRequestException("commandId parameter is required").asPromise();
            }
            final JsonValue command = commands.get(commandId);
            if (command == null) {
                return new BadRequestException("commandId parameter unknown: " + commandId).asPromise();
            }
            final String operation = command.get(OPERATION).asString();
            if (DELETE_OPERATION.equalsIgnoreCase(operation)) {
                return handleDeleteCommand(command, request);
            } else {
                return new InternalServerErrorException("command operation '" + operation + "' unsupported: "
                        + commandId).asPromise();
            }
        } else {
            return handler.handleAction(context, request).then(
                    new Function<ActionResponse, ActionResponse, ResourceException>() {
                        @Override
                        public ActionResponse apply(final ActionResponse value) throws ResourceException {
                            return Responses.newActionResponse(outputTransformer(value.getJsonContent()));
                        }
                    });
        }
    }

    /**
     * Handles a delete-command, which deletes multiple records at once.
     *
     * @param command Command config
     * @param request Action request
     * @return Response containing number of records deleted, or error response
     */
    private Promise<ActionResponse, ResourceException> handleDeleteCommand(final JsonValue command, final ActionRequest request) {
        // query for identifiers to delete
        final QueryRequest queryRequest = Requests.newQueryRequest(request.getResourcePath());
        queryRequest.addField(ID);
        queryRequest.setQueryFilter(QueryFilters.parse(command.get(QUERY_FILTER).asString()));

        final List<ResourceResponse> results = new ArrayList<>();
        final QueryResourceHandler handler = new QueryResourceHandler() {
            @Override
            public boolean handleResource(final ResourceResponse resourceResponse) {
                results.add(resourceResponse);
                return true;
            }
        };
        try {
            handleQuery(new RootContext(), queryRequest, handler).getOrThrowUninterruptibly();

            // delete each result by identifier
            for (final ResourceResponse result : results) {
                final DeleteRequest deleteRequest =
                        Requests.newDeleteRequest(request.getResourcePath(), result.getId());
                handleDelete(new RootContext(), deleteRequest).getOrThrow();
            }
            // return count of deleted records (See org.forgerock.openidm.repo.jdbc.impl.query.TableQueries.command())
            return newActionResponse(json(results.size())).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException("DELETE command failed", e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context,
                                                                    final PatchRequest patchRequest) {
        return handler.handlePatch(context, patchRequest).then(transformOutput);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest readRequest) {
        return handler.handleRead(context, readRequest).then(transformOutput);
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

        if (!uniqueAttributeResolver.isUnique(context, updateRequest.getContent())) {
            return new ConflictException("This entry already exists").asPromise();
        }

        return handler.handleUpdate(context, updateRequest).then(transformOutput);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
                                                                     final CreateRequest _request) {
        final CreateRequest createRequest = Requests.copyOfCreateRequest(_request);

        try {
            Map<String, Object> obj = inputTransformer(createRequest.getContent()).asMap();

            // Set id to a new UUID if none is specified (_action=create)
            if (isNullOrEmpty(createRequest.getNewResourceId())) {
                createRequest.setNewResourceId(UUID.randomUUID().toString());
            }

            obj.put(ID, createRequest.getNewResourceId());

            /*
             * XXX - all nulls are coming in as blank Strings. INVESTIGATE
             */

            Iterator<Map.Entry<String, Object>> iter = obj.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<String, Object> entry = iter.next();
                Object val = entry.getValue();

                if (val instanceof String && isNullOrEmpty((String) val)) {
                    iter.remove();
                }
            }

            final JsonValue content = new JsonValue(obj);

            if (!uniqueAttributeResolver.isUnique(context, content)) {
                return new ConflictException("This entry already exists").asPromise();
            }

            createRequest.setContent(content);

            return handler.handleCreate(context, createRequest).then(transformOutput);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest _request, final QueryResourceHandler _handler) {
        try {
            logger.debug("Querying {}", _request.getResourcePath());
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
                return handler.handleQuery(context, queryRequest, proxy);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }
}
