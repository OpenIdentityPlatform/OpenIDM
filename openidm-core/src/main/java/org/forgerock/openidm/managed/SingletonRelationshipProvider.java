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
import static org.forgerock.util.query.QueryFilter.*;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
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
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.util.RelationshipUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RelationshipProvider} representing a singleton relationship for the given field.
 */
class SingletonRelationshipProvider extends RelationshipProvider implements SingletonResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(SingletonRelationshipProvider.class);

    private final RequestHandler requestHandler;

    /**
     * Create a new relationship set for the given managed resource
     *
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath      Name of the resource we are handling relationships for eg. managed/user
     * @param schemaField       The schema of the field representing this relationship in the parent object.
     * @param activityLogger The audit activity logger to use
     * @param managedObjectSyncService Service to send sync events to
     */
    public SingletonRelationshipProvider(final ConnectionFactory connectionFactory, final ResourcePath resourcePath,
            final SchemaField schemaField, final ActivityLogger activityLogger,
            final ManagedObjectSetService managedObjectSyncService) {
        super(connectionFactory, resourcePath, schemaField, activityLogger, managedObjectSyncService);

        final Router router = new Router();
        router.addRoute(STARTS_WITH,
                uriTemplate(String.format("{%s}/%s", PARAM_MANAGED_OBJECT_ID, schemaField.getName())),
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
    public Promise<JsonValue, ResourceException> getRelationshipValueForResource(final Context context, final String resourceId) {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/relationship/singleton/getRelationshipValueForResource"), resourceId, context);

        try {
            return queryRelationship(context, resourceId).thenAsync(new AsyncFunction<ResourceResponse, JsonValue,
                    ResourceException>() {
                @Override
                public Promise<JsonValue, ResourceException> apply(ResourceResponse value) throws ResourceException {
                    return newResultPromise(value.getContent());
                }
            });
        } finally {
            measure.end();
        }
    }
    
    /**
     * Queries relationships, returning the relationship associated with this providers resource path and the specified 
     * relationship field.
     * 
     * @param context The current context
     * @param managedObjectId The id of the managed object to find relationships associated with
     * @return the promise of the query results.
     */
    private Promise<ResourceResponse, ResourceException> queryRelationship(final Context context, 
            final String managedObjectId) {
        try {
            final String resourceFullPath = resourceContainer.child(managedObjectId).toString();
            final QueryRequest queryRequest = Requests.newQueryRequest(REPO_RESOURCE_PATH)
                    .setQueryId(RELATIONSHIP_QUERY_ID)
                    .setAdditionalParameter(QUERY_FIELD_RESOURCE_PATH, resourceFullPath)
                    .setAdditionalParameter(QUERY_FIELD_FIELD_NAME, schemaField.getName())
                    .setAdditionalParameter(PARAM_MANAGED_OBJECT_ID, managedObjectId);
            final List<ResourceResponse> relationships = new ArrayList<>();
            
            getConnection().query(context, queryRequest, relationships);

            if (relationships.isEmpty()) {
                return new NotFoundException().asPromise();
            } else if (relationships.size() == 1) {
                return newResultPromise(formatResponse(context, queryRequest).apply(relationships.get(0)));
            } else {
                // This is a singleton relationship with more than 1 reference - this is an error.
                // Collect all the erroneous references and add them to the error message.
                List<String> errorReferences = new ArrayList<>();
                for (ResourceResponse relationship : relationships) {
                    JsonValue content = relationship.getContent();
                    if (schemaField.isReverseRelationship() &&
                            content.get(REPO_FIELD_FIRST_ID).defaultTo("").asString().equals(resourceFullPath)) {
                        errorReferences.add(content.get(REPO_FIELD_SECOND_ID).asString());
                    } else {
                        errorReferences.add(content.get(REPO_FIELD_FIRST_ID).asString());
                    }
                }
                ResourceResponse relationship = relationships.get(0);
                relationship.getContent().add(RelationshipUtil.REFERENCE_ERROR, true);
                relationship.getContent().add(RelationshipUtil.REFERENCE_ERROR_MESSAGE,
                        "Multiple references found for singleton relationship " + errorReferences);
                return newResultPromise(formatResponse(context, queryRequest).apply(relationship));
            }
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<JsonValue, ResourceException> setRelationshipValueForResource(final boolean clearExisting,
            final Context context, final String resourceId, final JsonValue value) {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/relationship/singleton/setRelationshipValueForResource"), resourceId, context);

        try {
            if (value.isNotNull()) {
                try {
                    final JsonValue id = value.get(FIELD_ID);

                    // Update if we got an id, otherwise replace
                    if (id != null && id.isNotNull()) {
                        final UpdateRequest updateRequest = Requests.newUpdateRequest("", value)
                                .setAdditionalParameter(PARAM_MANAGED_OBJECT_ID, resourceId);
                        return updateInstance(context, value.get(FIELD_ID).asString(), updateRequest)
                                .then(new Function<ResourceResponse, JsonValue, ResourceException>() {
                                    @Override
                                    public JsonValue apply(ResourceResponse resourceResponse) throws ResourceException {
                                        return resourceResponse.getContent();
                                    }
                                });
                    } else { // no id, replace current instance
                        if (!clearExisting) {
                            clear(context, resourceId);
                        }

                        final CreateRequest createRequest = Requests.newCreateRequest("", value)
                                .setAdditionalParameter(PARAM_MANAGED_OBJECT_ID, resourceId);
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
                if (!clearExisting) {
                    clear(context, resourceId);
                }

                return newResultPromise(json(null));
            }
        } finally {
            measure.end();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Promise<JsonValue, ResourceException> clear(final Context context, final String resourceId) {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/relationship/singleton/clear"), resourceId, context);

        try {
            return getRelationshipValueForResource(context, resourceId).then(new Function<JsonValue, JsonValue, ResourceException>() {
                @Override
                public JsonValue apply(JsonValue relationship) throws ResourceException {
                    return deleteInstance(context, relationship.get(FIELD_ID).asString(),
                            Requests.newDeleteRequest("").setAdditionalParameter(PARAM_MANAGED_OBJECT_ID, resourceId))
                                .getOrThrowUninterruptibly().getContent();
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
        } finally {
            measure.end();
        }
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

    /**
     * Implemented to simply call validateRelationship for the single field, if it has changed.
     *
     * @param context context of the original request.
     * @param oldValue old value of field to validate
     * @param newValue new value of field to validate
     * @throws BadRequestException when the relationship isn't valid, ResourceException otherwise.
     * @see RelationshipValidator#validateRelationship(JsonValue, Context)
     */
    public void validateRelationshipField(Context context, JsonValue oldValue, JsonValue newValue)
            throws ResourceException {
        if (oldValue.isNull() && newValue.isNull()) {
            logger.debug("not validating relationship as old and new values are both null.");
        } else if (oldValue.isNull() || !oldValue.getObject().equals(newValue.getObject())) {
            relationshipValidator.validateRelationship(newValue, context);
        }
    }

}
