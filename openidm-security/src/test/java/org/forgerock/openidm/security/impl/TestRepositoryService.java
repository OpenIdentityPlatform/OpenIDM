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
package org.forgerock.openidm.security.impl;

import java.util.LinkedList;
import java.util.List;

import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;

class TestRepositoryService implements RepositoryService {

    private final MemoryBackend repo = new MemoryBackend();

    @Override
    public ResourceResponse create(CreateRequest request) throws ResourceException {
        try {
            final Promise<ResourceResponse, ResourceException> promise =
                    repo.createInstance(new RootContext(), request);
            return promise.getOrThrow();
        } catch (InterruptedException e) {
            throw new InternalServerErrorException("Unable to create object in repo", e);
        }
    }

    @Override
    public ResourceResponse read(ReadRequest request) throws ResourceException {
        try {
            final Promise<ResourceResponse, ResourceException> promise =
                    repo.readInstance(new RootContext(), request.getResourcePath(), request);
            return promise.getOrThrow();
        } catch (InterruptedException e) {
            throw new InternalServerErrorException("Unable to read object in repo", e);
        }
    }

    @Override
    public ResourceResponse update(UpdateRequest request) throws ResourceException {
        try {
            final Promise<ResourceResponse, ResourceException> promise =
                    repo.updateInstance(new RootContext(), request.getResourcePath(), request);
            return promise.getOrThrow();
        } catch (InterruptedException e) {
            throw new InternalServerErrorException("Unable to update object in repo", e);
        }
    }

    @Override
    public ResourceResponse delete(DeleteRequest request) throws ResourceException {
        try {
            final Promise<ResourceResponse, ResourceException> promise =
                    repo.deleteInstance(new RootContext(), request.getResourcePath(), request);
            return promise.getOrThrow();
        } catch (InterruptedException e) {
            throw new InternalServerErrorException("Unable to delete object in repo", e);
        }
    }

    @Override
    public List<ResourceResponse> query(QueryRequest request) throws ResourceException {
        try {
            final List<ResourceResponse> resources = new LinkedList<>();
            final Promise<QueryResponse, ResourceException> promise =
                    repo.queryCollection(new RootContext(), request, new QueryResourceHandler() {
                        @Override
                        public boolean handleResource(ResourceResponse resource) {
                            resources.add(resource);
                            return true;
                        }
                    });
            promise.getOrThrow();
            return resources;
        } catch (InterruptedException e) {
            throw new InternalServerErrorException("Unable to query objects in repo", e);
        }
    }
}
