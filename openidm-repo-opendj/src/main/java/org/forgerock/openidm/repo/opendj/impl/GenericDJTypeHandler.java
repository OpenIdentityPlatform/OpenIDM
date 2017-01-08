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

import org.forgerock.guava.common.collect.ObjectArrays;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.openidm.query.FieldTransformerQueryFilterVisitor;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

import java.util.HashSet;
import java.util.Set;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * Type handler for generic objects that simply places all properties in a {@code fullobject} JSON field.
 */
public class GenericDJTypeHandler extends AbstractDJTypeHandler {

    /**
     * Non-generic properties. Currently only containing _id and _rev.
     * Can be used in the future to facilitate hybrid objects (both explicit and properties)
     */
    private final Set<String> explicitProperties;

    /**
     * QueryFilterVisitor that prefixes generic attributes with /fullobject to match DJ schema.
     */
    final FieldTransformerQueryFilterVisitor<Void, JsonPointer> transformer = new FieldTransformerQueryFilterVisitor<Void, JsonPointer>() {
        @Override
        protected JsonPointer transform(Void param, JsonPointer ptr) {
            // TODO is it ok that this is case sensitive?
            if (ptr.isEmpty() || explicitProperties.contains(ptr.get(0))) {
                return ptr;
            } else {
                // generic field, prepend
                return new JsonPointer(ObjectArrays.concat("fullobject", ptr.toArray()));
            }
        }
    };

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

        this.explicitProperties = new HashSet<>();
        this.explicitProperties.add("_id");
        this.explicitProperties.add("_rev");
    }

    @Override
    protected JsonValue inputTransformer(final JsonValue jsonValue) throws ResourceException {
        final JsonValue output = json(object());
        final JsonValue fullobject = jsonValue.clone();
        output.put("fullobject", fullobject);

        for (final String prop : explicitProperties) {
            output.put(prop, fullobject.get(prop).getObject());
            fullobject.remove(prop);
        }

        return output;
    }

    @Override
    protected JsonValue outputTransformer(final JsonValue jsonValue) throws ResourceException {
        final JsonValue output = jsonValue.get("fullobject").clone();

        for (final String prop : explicitProperties) {
            output.put(prop, jsonValue.get(prop).getObject());
        }

        return output;
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request, final QueryResourceHandler handler) {
        final QueryFilter<JsonPointer> originalFilter = request.getQueryFilter();

        // prefix query filters on generic fields with /fullobject to match LDAP object
        if (originalFilter != null) {
            request.setQueryFilter(originalFilter.accept(transformer, null));
        }

        return super.handleQuery(context, request, handler);
    }
}
