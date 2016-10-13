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
     * @param commands Configured commands for this resource
     *
     * @see AbstractDJTypeHandler#AbstractDJTypeHandler(ResourcePath, RequestHandler, RouteEntry, JsonValue, JsonValue, JsonValue)
     */
    GenericDJTypeHandler(final ResourcePath resourcePath, final RequestHandler repoHandler, final RouteEntry routeEntry, final JsonValue config, final JsonValue queries, final JsonValue commands) {
        super(resourcePath, repoHandler, routeEntry, config, queries, commands);
    }

    @Override
    protected JsonValue inputTransformer(JsonValue jsonValue) throws ResourceException {
        return json(object(field("fullobject", jsonValue.getObject())));
    }

    @Override
    protected JsonValue outputTransformer(JsonValue jsonValue) throws ResourceException {
        return jsonValue.get("fullobject");
    }
}
