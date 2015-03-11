/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.security.impl;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.openidm.security.KeyStoreManager;
import org.forgerock.openidm.util.ResourceUtil;

/**
 * A generic collection resource provider servicing requests on entries in a keystore
 * 
 */
public abstract class EntryResourceProvider extends SecurityResourceProvider implements CollectionResourceProvider {

    public EntryResourceProvider(String resourceName, KeyStoreHandler store, KeyStoreManager manager, RepositoryService repoService) {
        super(resourceName, store, manager, repoService);
    }

    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            if (null != request.getNewResourceId()) {
                if (store.getStore().containsAlias(request.getNewResourceId())) {
                    handler.handleError(new ConflictException("The resource with ID '"
                            + request.getNewResourceId() + "' could not be created because "
                            + "there is already another resource with the same ID"));
                } else {
                    String resourceId = request.getNewResourceId();
                    storeEntry(request.getContent(), resourceId);
                    manager.reload();
                    // Save the store to the repo (if clustered)
                    saveStore();
                    handler.handleResult(new Resource(resourceId, null, request.getContent()));
                }
            } else {
                handler.handleError(new BadRequestException(
                        "A valid resource ID must be specified in the request"));
            }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void readInstance(final ServerContext context, final String resourceId,
            final ReadRequest request, final ResultHandler<Resource> handler) {
        try {
            if (!store.getStore().containsAlias(resourceId)) {
                handler.handleError(new NotFoundException());
            } else {
                JsonValue result = readEntry(resourceId);
                handler.handleResult(new Resource(resourceId, null, result));
            }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void updateInstance(final ServerContext context, final String resourceId,
            final UpdateRequest request, final ResultHandler<Resource> handler) {
        try {
            storeEntry(request.getContent(), resourceId);
            manager.reload();
            // Save the store to the repo (if clustered)
            saveStore();
            handler.handleResult(new Resource(resourceId, null, request.getContent()));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void deleteInstance(final ServerContext context, final String resourceId,
            final DeleteRequest request, final ResultHandler<Resource> handler) {
        try {
            if (!store.getStore().containsAlias(resourceId)) {
                handler.handleError(new NotFoundException());
            } else {
                store.getStore().deleteEntry(resourceId);
                store.store();
                manager.reload();
                // Save the store to the repo (if clustered)
                saveStore();
                handler.handleResult(new Resource(resourceId, null, new JsonValue(null)));
            }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }
    
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        final ResourceException e = new NotSupportedException("Action operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        final ResourceException e = new NotSupportedException("Action operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, 
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        final ResourceException e = new NotSupportedException("Query operations are not supported");
        handler.handleError(e);
    }
    
    public abstract void createDefaultEntry(String alias) throws Exception;
    
    public boolean hasEntry(String alias) throws Exception {
        return store.getStore().containsAlias(alias);
    }

    protected abstract void storeEntry(JsonValue value, String alias) throws Exception;
    
    protected abstract JsonValue readEntry(String alias) throws Exception;
}
