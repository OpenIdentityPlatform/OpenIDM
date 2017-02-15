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

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.forgerock.guava.common.base.Strings.isNullOrEmpty;

public class ExplicitDJTypeHandler extends AbstractDJTypeHandler {

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
    }

    @Override
    protected JsonValue outputTransformer(JsonValue jsonValue) throws ResourceException {
        return jsonValue;
    }


    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
                                                                     final UpdateRequest _updateRequest) {
        final UpdateRequest updateRequest = Requests.copyOfUpdateRequest(_updateRequest);

        if (!uniqueAttributeResolver.isUnique(context, updateRequest.getContent())) {
            return new ConflictException("This entry already exists").asPromise();
        }

        return handler.handleUpdate(context, updateRequest).then(transformOutput);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
                                                                     final CreateRequest _request) {
        final CreateRequest createRequest = Requests.copyOfCreateRequest(_request);

        Map<String, Object> obj = createRequest.getContent().asMap();

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
    }

}
