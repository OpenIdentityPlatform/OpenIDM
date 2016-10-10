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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.repo.opendj.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.util.Strings;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
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
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ExplicitDJTypeHandler extends AbstractDJTypeHandler {

    /** Properties that should be stored as strings */
    private final Set<String> propertiesToStringify;
    /**
     * Create a new DJ type handler.
     *
     * @param resourcePath The path to this resource on {@code repoHandler}
     * @param repoHandler The request handler provided by rest2ldap for repo access
     * @param routeEntry The entry on the IDM router for this handler
     * @param config Configuration specific to this type handler
     * @param queries Configured queries for this resource
     * @param commands Configured commands for this resource
     *
     * @see AbstractDJTypeHandler#AbstractDJTypeHandler(ResourcePath, RequestHandler, RouteEntry, JsonValue, JsonValue, JsonValue)
     */
    ExplicitDJTypeHandler(final ResourcePath resourcePath, final RequestHandler repoHandler, final RouteEntry routeEntry, final JsonValue config, final JsonValue queries, final JsonValue commands) {
        super(resourcePath, repoHandler, routeEntry, config, queries, commands);

        final Set<String> propertiesToStringify = new HashSet<String>();
        for (final String prop : config.get("propertiesToStringify").defaultTo(new HashSet<String>()).asList(String.class)) {
            propertiesToStringify.add(prop);
        }
        this.propertiesToStringify = propertiesToStringify;
    }

    @Override
    protected JsonValue inputTransformer(JsonValue jsonValue) throws ResourceException {
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

}
