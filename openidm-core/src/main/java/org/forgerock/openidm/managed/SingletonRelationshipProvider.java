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

import org.forgerock.http.routing.UriRouterContext;
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
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

import java.util.ArrayList;
import java.util.List;

class SingletonRelationshipProvider extends RelationshipProvider implements SingletonResourceProvider {
    private final RequestHandler requestHandler;

    /**
     * Create a new relationship set for the given managed resource
     *
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath      Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName      Name of property on first object represents the relationship
     */
    public SingletonRelationshipProvider(ConnectionFactory connectionFactory, ResourcePath resourcePath, JsonPointer propertyName) {
        super(connectionFactory, resourcePath, propertyName);

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
        return fetch(context, resourceId).thenAsync(new AsyncFunction<ResourceResponse, JsonValue, ResourceException>() {
            @Override
            public Promise<JsonValue, ResourceException> apply(ResourceResponse value) throws ResourceException {
                return newResultPromise(value.getContent());
            }
        });
    }

    private Promise<ResourceResponse, ResourceException> fetch(Context context, String resourceId) {
        try {
            final QueryRequest queryRequest = Requests.newQueryRequest(REPO_RESOURCE_PATH);
            queryRequest.setAdditionalParameter(PARAM_FIRST_ID, resourceId);
            final List<ResourceResponse> relationships = new ArrayList<>();

            queryRequest.setQueryFilter(QueryFilter.and(
                    QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_ID), resourcePath.child(resourceId)),
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
    public Promise<JsonValue, ResourceException> setRelationshipValueForResource(Context context, String resourceId, JsonValue value) {
        if (value.isNotNull()) {
            try {
                final JsonValue id = value.get(FIELD_PROPERTIES.child("_id"));

                // Update if we got an id, otherwise replace
                if (id != null && id.isNotNull()) {
                    final UpdateRequest updateRequest = Requests.newUpdateRequest("", value);
                    updateRequest.setAdditionalParameter(PARAM_FIRST_ID, resourceId);
                    return updateInstance(context, value.get(FIELD_PROPERTIES.child("_id")).asString(), updateRequest)
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
            public JsonValue apply(JsonValue jsonValue) throws ResourceException {
                return deleteInstance(context, jsonValue.get("_id").asString(), Requests.newDeleteRequest(""))
                        .getOrThrowUninterruptibly().getContent();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(final Context context, final ReadRequest request) {
        return relationshipId(context).thenAsync(new AsyncFunction<String, ResourceResponse, ResourceException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(String relationshipId) throws ResourceException {
                return readInstance(context, relationshipId, request);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context, final UpdateRequest request) {
        return relationshipId(context).thenAsync(new AsyncFunction<String, ResourceResponse, ResourceException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(String relationshipId) throws ResourceException {
                return updateInstance(context, relationshipId, request);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(final Context context, final PatchRequest request) {
        return relationshipId(context).thenAsync(new AsyncFunction<String, ResourceResponse, ResourceException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(String relationshipId) throws ResourceException {
                return patchInstance(context, relationshipId, request);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(final Context context, final ActionRequest request) {
        return relationshipId(context).thenAsync(new AsyncFunction<String, ActionResponse, ResourceException>() {
            @Override
            public Promise<ActionResponse, ResourceException> apply(String relationshipId) throws ResourceException {
                return actionInstance(context, relationshipId, request);
            }
        });
    }

    /**
     * Return the relationship id of the current singleton value.
     *
     * @param context The current context
     * @return The id of the current relationship this singleton represents
     */
    private Promise<String, ResourceException> relationshipId(Context context) {
        final String firstId =
                context.asContext(UriRouterContext.class).getUriTemplateVariables().get(URI_PARAM_FIRST_ID);

        return fetch(context, firstId)
                .then(new Function<ResourceResponse, String, ResourceException>() {
                    @Override
                    public String apply(ResourceResponse value) throws ResourceException {
                        return value.getId();
                    }
                });
    }
}
