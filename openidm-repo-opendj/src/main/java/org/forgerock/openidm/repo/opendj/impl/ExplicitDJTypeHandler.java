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

import com.fasterxml.jackson.core.type.TypeReference;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import java.io.IOException;
import java.util.*;

import static org.forgerock.guava.common.base.Strings.isNullOrEmpty;

public class ExplicitDJTypeHandler extends AbstractDJTypeHandler {

    /** Properties that should be stored as strings */
    private final List<String> propertiesToStringify;

    /**
     * Create a new DJ type handler.
     *
     * @param repoResource The path to this resource on {@code repoHandler}
     * @param repoHandler The request handler provided by rest2ldap for repo access
     * @param config Configuration specific to this type handler
     * @param queries Configured queries for this resource
     * @param commands Configured commands for this resource
     *
     * @see AbstractDJTypeHandler#AbstractDJTypeHandler(ResourcePath, RequestHandler, JsonValue, JsonValue, JsonValue)
     */
    ExplicitDJTypeHandler(final ResourcePath repoResource, final RequestHandler repoHandler, final JsonValue config, final JsonValue queries, final JsonValue commands) {
        super(repoResource, repoHandler, config, queries, commands);

        final List<String> propertiesToStringify = new ArrayList<>();
        for (final String prop : config.get("propertiesToStringify").defaultTo(new ArrayList<String>()).asList(String.class)) {
            propertiesToStringify.add(prop);
        }
        this.propertiesToStringify = propertiesToStringify;
    }

    private JsonValue inputTransformer(JsonValue jsonValue) throws ResourceException {
        return stringify(jsonValue, propertiesToStringify);
    }

    @Override
    protected JsonValue outputTransformer(JsonValue jsonValue) throws ResourceException {
        return destringify(jsonValue, propertiesToStringify);
    }

    /**
     * Convert the given collection of properties to Strings. This can be useful if a datastore does not support
     * embedded objects.
     *
     * @param jsonValue Json object to convert
     * @param properties List of properties that need to be processed
     *
     * @return A new JSON object with stringified properties
     *
     * @throws ResourceException If there is an error converting the property
     */
    static JsonValue stringify(JsonValue jsonValue, Collection<String> properties) throws ResourceException {
        Map<String, Object> obj = jsonValue.asMap();

        for (String property : properties) {
            try {
                Object val = obj.get(property);

                if (val != null) {
                    obj.put(property, mapper.writeValueAsString(val));
                }
            } catch (IOException e) {
                throw new InternalServerErrorException("Failed to convert property '" + property + "' to String", e);
            }
        }

        return new JsonValue(obj);
    }

    /**
     * Convert stringified properties in the Json object to JsonValues
     *
     * @param jsonValue Json object to de-stringify
     * @param properties Collection of object attributes to destringify
     *
     * @return A new JsonValue with the given properties de-stringified
     *
     * @throws ResourceException
     */
    static JsonValue destringify(final JsonValue jsonValue, final Collection<String> properties) throws ResourceException {
        Map<String, Object> obj = jsonValue.asMap();

        final TypeReference<LinkedHashMap<String,Object>> objectTypeRef = new TypeReference<LinkedHashMap<String,Object>>() {};
        final TypeReference<List<LinkedHashMap<String,Object>>> arrayTypeRef = new TypeReference<List<LinkedHashMap<String,Object>>>() {};

        try {
            // FIXME - parameterize
            for (String key : properties) {
                String val = (String) obj.get(key);

                // TODO - this is yuck ... remove once we have JSON attribute support in DJ
                if (val != null && val.trim().startsWith("{")) {
                    obj.put(key, mapper.readValue(val, objectTypeRef));
                } else if (val != null && val.trim().startsWith("[")) {
                    obj.put(key, mapper.readValue(val, arrayTypeRef));
                } else {
                    logger.error("Could not destringify value '{}'", val);
                }
            }
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to convert String property to object", e);
        }

        return new JsonValue(obj);
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

}
