package org.forgerock.openidm.repo.opendj.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.openidm.router.RouteEntry;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * Type handler for generic objects that simply places all properties in a {@code fullobject} JSON field.
 */
public class GenericDJTypeHandler extends AbstractDJTypeHandler {
    /**
     * Create a new generic DJ type handler.
     *
     * This handler simply places all object properties within the {@code fullobject} JSON field.
     *
     * @param resourcePath The path to this resource on {@code repoHandler}
     * @param repoHandler The request handler provided by rest2ldap for repo access
     * @param routeEntry The entry on the IDM router for this handler
     * @param config Configuration specific to this type handler
     * @param queries Configured queries for this resource
     *
     * @see AbstractDJTypeHandler#AbstractDJTypeHandler(ResourcePath, RequestHandler, RouteEntry, JsonValue, JsonValue)
     */
    GenericDJTypeHandler(final ResourcePath resourcePath, final RequestHandler repoHandler, final RouteEntry routeEntry, final JsonValue config, final JsonValue queries) {
        super(resourcePath, repoHandler, routeEntry, config, queries);
    }

    @Override
    protected JsonValue inputTransformer(JsonValue jsonValue) throws ResourceException {
        try {
            final String stringified = mapper.writeValueAsString(jsonValue.asMap());

            return json(object(field("fullobject", stringified)));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new InternalServerErrorException("Failed to transform input json", e);
        }
    }

    @Override
    protected JsonValue outputTransformer(JsonValue jsonValue) throws ResourceException {
        final String fullObject = jsonValue.get("fullobject").asString();

        final TypeReference<LinkedHashMap<String,Object>> objectTypeRef = new TypeReference<LinkedHashMap<String,Object>>() {};
        try {
            return json(mapper.readValue(fullObject, objectTypeRef));
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to convert fullobject property of generic object", e);
        }
    }
}
