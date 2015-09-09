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

import org.forgerock.http.Context;
import org.forgerock.http.ResourcePath;
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
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

import java.util.ArrayList;
import java.util.List;

public class SingletonRelationshipProvider extends RelationshipProvider implements SingletonResourceProvider {
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
        router.addRoute(STARTS_WITH, uriTemplate("{firstId}/" + propertyName.leaf()), Resources.newSingleton(this));
        this.requestHandler = router;
    }

    /** {@inheritDoc} */
    @Override
    public RequestHandler asRequestHandler() {
        return requestHandler;
    }

    /** {@inheritDoc} */
    @Override
    public Promise<JsonValue, ResourceException> fetchJson(Context context, String resourceId) {
        try {
            return newResultPromise(fetch(context, resourceId).getContent());
        } catch (NotFoundException e) {
            return newResultPromise(json(null));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    private ResourceResponse fetch(Context context, String resourceId) throws NotFoundException, ResourceException {
        final QueryRequest queryRequest = Requests.newQueryRequest(REPO_RESOURCE_PATH);
        queryRequest.setAdditionalParameter(PARAM_FIRST_ID, resourceId);
        final List<ResourceResponse> relationships = new ArrayList<>();

        queryRequest.setQueryFilter(QueryFilter.and(
                QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_ID), resourcePath.child(resourceId)),
                QueryFilter.equalTo(new JsonPointer(REPO_FIELD_FIRST_PROPERTY_NAME), propertyName)
        ));

        connectionFactory.getConnection().query(context, queryRequest, relationships);

        if (relationships.isEmpty()) {
            throw new NotFoundException();
        } else {
            // TODO - check size and throw illegal state if more than one?
            return FORMAT_RESPONSE.apply(relationships.get(0));
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
    public Promise<JsonValue, ResourceException> persistJson(Context context, String resourceId, JsonValue value) {
        if (value != null && value.isNotNull()) {
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
        return fetchJson(context, resourceId).then(new Function<JsonValue, JsonValue, ResourceException>() {
            @Override
            public JsonValue apply(JsonValue jsonValue) throws ResourceException {
                return deleteInstance(context, jsonValue.get("_id").asString(), Requests.newDeleteRequest(""))
                        .getOrThrowUninterruptibly().getContent();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        try {
            return super.readInstance(context, relationshipId(context), request);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        try {
            return super.updateInstance(context, relationshipId(context), request);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        try {
            return super.patchInstance(context, relationshipId(context), request);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        try {
            return super.actionInstance(context, relationshipId(context), request);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /**
     * Return the relationship id of the current singleton value.
     *
     * @param context The current context
     * @return The id of the current relationship this singleton represents
     * @throws ResourceException If the id could not be retrieved
     */
    private String relationshipId(Context context) throws ResourceException {
        final String firstId =
                context.asContext(UriRouterContext.class).getUriTemplateVariables().get(URI_PARAM_FIRST_ID);
        return fetch(context, firstId).getId();
    }
}
