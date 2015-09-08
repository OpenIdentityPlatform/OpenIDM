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

import org.forgerock.http.Context;
import org.forgerock.http.ResourcePath;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

public class SingletonRelationshipProvider extends RelationshipProvider implements SingletonResourceProvider {
    /** The id of this singleton relationship */
    private String relationshipId = null;

    /**
     * Create a new relationship set for the given managed resource
     *
     * @param connectionFactory Connection factory used to access the repository
     * @param resourcePath      Name of the resource we are handling relationships for eg. managed/user
     * @param propertyName      Name of property on first object represents the relationship
     */
    public SingletonRelationshipProvider(ConnectionFactory connectionFactory, ResourcePath resourcePath, JsonPointer propertyName) {
        super(connectionFactory, resourcePath, propertyName);
    }

    @Override
    public RequestHandler asRequestHandler() {
        return Resources.newSingleton(this);
    }

    @Override
    public Promise<JsonValue, ResourceException> fetchJson(Context context, String resourceId) {
        return null;
    }

    @Override
    public Promise<JsonValue, ResourceException> persistJson(Context context, String resourceId, JsonValue value) {
        return null;
    }

    @Override
    public Promise<JsonValue, ResourceException> clear(Context context, String resourceId) {
        return null;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        return null;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return null;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return null;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return null;
    }
}
