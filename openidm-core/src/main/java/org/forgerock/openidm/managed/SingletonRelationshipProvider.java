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
package org.forgerock.openidm.managed;

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

class SingletonRelationshipProvider extends RelationshipProvider implements SingletonResourceProvider {
    
    private final RequestHandler requestHandler;

    /**
     * Create a new relationship set for the given managed resource
     *
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath      Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName      Name of property on first object represents the relationship
     */
    public SingletonRelationshipProvider(ConnectionFactory connectionFactory, ResourcePath resourcePath, 
            JsonPointer propertyName, ActivityLogger activityLogger, 
            final ManagedObjectSyncService managedObjectSyncService) {
        super(connectionFactory, resourcePath, propertyName, activityLogger, managedObjectSyncService);

        final Router router = new Router();
        router.addRoute(STARTS_WITH,
                uriTemplate(String.format("{%s}/%s", URI_PARAM_FIRST_ID, propertyName.leaf())),
                Resources.newSingleton(this));
        this.requestHandler = router;
    }

    /** {@inheritDoc} */
    @Override
    public RequestHandler asRequestHandler() {
        return requestHandler;
    }

    /** {@inheritDoc} */
    @Override
    public Promise<JsonValue, ResourceException> getRelationshipValueForResource(Context context, String resourceId) {
        return queryRelationship(context, resourceId).thenAsync(new AsyncFunction<ResourceResponse, JsonValue, 
                ResourceException>() {
            @Override
            public Promise<JsonValue, ResourceException> apply(ResourceResponse value) throws ResourceException {
                return newResultPromise(value.getContent());
            }
        });
    }
    
    /**
     * Queries relationships, returning the relationship associated with this providers resource path and the specified 
     * relationship field.
     * 
     * @param context
     * @param resourceId
     * @return
     */
    private Promise<ResourceResponse, ResourceException> queryRelationship(Context context, String managedObjectId) {
        try {
            final QueryRequest queryRequest = Requests.newQueryRequest(REPO_RESOURCE_PATH);
            queryRequest.setAdditionalParameter(PARAM_FIRST_ID, managedObjectId);
            final List<ResourceResponse> relationships = new ArrayList<>();

            queryRequest.setQueryFilter(QueryFilter.and(
                    QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_ID), resourcePath.child(managedObjectId)),
                    QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_PROPERTY_NAME), propertyName)
            ));

            connectionFactory.getConnection().query(context, queryRequest, relationships);

            if (relationships.isEmpty()) {
                return new NotFoundException().asPromise();
            } else {
                // TODO OPENIDM-4094 - check size and throw illegal state if more than one?
                return newResultPromise(FORMAT_RESPONSE.apply(relationships.get(0)));
            }
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }
    
    /**
     * Persist the supplied {@link JsonValue} {@code value} as the new state of this singleton relationship on
     * {@code resourceId}.
     *
     * <em>This is currently the only means of creating an instance of this singleton</em>
     *
     * @param context The context of this request
     * @param resourceId Id of the resource relation fields in value are to be memebers of
     * @param value A {@link JsonValue} map of relationship fields and their values
     *
     * @return The persisted instance of {@code value}
     */
    @Override
    public Promise<JsonValue, ResourceException> setRelationshipValueForResource(Context context, String resourceId, 
            JsonValue value) {
        if (value.isNotNull()) {
            try {
                final JsonValue id = value.get(FIELD_ID);

                // Update if we got an id, otherwise replace
                if (id != null && id.isNotNull()) {
                    final UpdateRequest updateRequest = Requests.newUpdateRequest("", value);
                    updateRequest.setAdditionalParameter(PARAM_FIRST_ID, resourceId);
                    return updateInstance(context, value.get(FIELD_ID).asString(), updateRequest)
                            .then(new Function<ResourceResponse, JsonValue, ResourceException>() {
                                @Override
                                public JsonValue apply(ResourceResponse resourceResponse) throws ResourceException {
                                    return resourceResponse.getContent();
                                }
                            });
                } else { // no id, replace current instance
                    clear(context, resourceId);

                    final CreateRequest createRequest = Requests.newCreateRequest("", value);
                    createRequest.setAdditionalParameter(PARAM_FIRST_ID, resourceId);
                    return createInstance(context, createRequest).then(new Function<ResourceResponse, JsonValue, ResourceException>() {
                        @Override
                        public JsonValue apply(ResourceResponse resourceResponse) throws ResourceException {
                            return resourceResponse.getContent();
                        }
                    });
                }
            } catch (ResourceException e) {
                return e.asPromise();
            }

        } else {
            clear(context, resourceId);
            return newResultPromise(json(null));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Promise<JsonValue, ResourceException> clear(final Context context, final String resourceId) {
        return getRelationshipValueForResource(context, resourceId).then(new Function<JsonValue, JsonValue, ResourceException>() {
            @Override
            public JsonValue apply(JsonValue relationship) throws ResourceException {
                return deleteInstance(context, relationship.get(FIELD_ID).asString(),
                        Requests.newDeleteRequest("")).getOrThrowUninterruptibly().getContent();
            }
        }).thenCatch(new Function<ResourceException, JsonValue, ResourceException>() {
            @Override
            public JsonValue apply(ResourceException e) throws ResourceException {
                // Since we wish to clear here NotFound is not an error. Return empty json
                if (e instanceof NotFoundException) {
                    return json(null);
                } else {
                    throw e;
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(final Context context, 
            final ReadRequest request) {
        return getRelationshipId(context).thenAsync(new AsyncFunction<String, ResourceResponse, ResourceException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(String relationshipId) throws ResourceException {
                return readInstance(context, relationshipId, request);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context, 
            final UpdateRequest request) {
        return getRelationshipId(context).thenAsync(new AsyncFunction<String, ResourceResponse, ResourceException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(String relationshipId) throws ResourceException {
                return updateInstance(context, relationshipId, request);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(final Context context, 
            final PatchRequest request) {
        return getRelationshipId(context).thenAsync(new AsyncFunction<String, ResourceResponse, ResourceException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(String relationshipId) throws ResourceException {
                return patchInstance(context, relationshipId, request);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(final Context context, 
            final ActionRequest request) {
        return getRelationshipId(context).thenAsync(new AsyncFunction<String, ActionResponse, ResourceException>() {
            @Override
            public Promise<ActionResponse, ResourceException> apply(String relationshipId) throws ResourceException {
                return actionInstance(context, relationshipId, request);
            }
        });
    }

    /**
     * Return the id of the relationship that this singleton represents.  The {@link Context} is used to find the id of 
     * the managed object for this request.
     * 
     * @param context The current context
     * @return The id of the current relationship this singleton represents
     */
    private Promise<String, ResourceException> getRelationshipId(Context context) {
        final String managedObjectId = getManagedObjectId(context);

        return queryRelationship(context, managedObjectId)
                .then(new Function<ResourceResponse, String, ResourceException>() {
                    @Override
                    public String apply(ResourceResponse value) throws ResourceException {
                        return value.getContent().get(FIELD_ID).asString();
                    }
                });
    }
}
