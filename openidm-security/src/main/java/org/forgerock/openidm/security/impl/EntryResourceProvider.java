/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 ForgeRock AS. All Rights Reserved
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

import static org.forgerock.json.resource.ResourceException.newBadRequestException;
import static org.forgerock.json.resource.ResourceException.newNotSupportedException;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.openidm.security.KeyStoreManager;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.util.promise.Promise;

/**
 * A generic collection resource provider servicing requests on entries in a keystore
 * 
 */
public abstract class EntryResourceProvider extends SecurityResourceProvider implements CollectionResourceProvider {

    public EntryResourceProvider(String resourceName, KeyStoreHandler store, KeyStoreManager manager,
            RepositoryService repoService) {
        super(resourceName, store, manager, repoService);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context,
            final CreateRequest request) {
        try {
            if (null != request.getNewResourceId()) {
                if (store.getStore().containsAlias(request.getNewResourceId())) {
                    // TODO-crest3 Remove cast
                    return newExceptionPromise((ResourceException) new ConflictException(
                            "The resource with ID '" + request.getNewResourceId() + "' could not be created because "
                                    + "there is already another resource with the same ID"));
                } else {
                    String resourceId = request.getNewResourceId();
                    storeEntry(request.getContent(), resourceId);
                    manager.reload();
                    // Save the store to the repo (if clustered)
                    saveStore();
                    return newResultPromise(newResourceResponse(resourceId, null, request.getContent()));
                }
            } else {
                return newExceptionPromise(newBadRequestException(
                        "A valid resource ID must be specified in the request"));
            }
        } catch (Exception e) {
            return newExceptionPromise(ResourceUtil.adapt(e));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(final Context context, final String resourceId,
            final ReadRequest request) {
        try {
            if (!store.getStore().containsAlias(resourceId)) {
                return newExceptionPromise(ResourceException.newNotFoundException());
            } else {
                JsonValue result = readEntry(resourceId);
                return newResultPromise(newResourceResponse(resourceId, null, result));
            }
        } catch (Exception e) {
            return newExceptionPromise(ResourceUtil.adapt(e));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context, final String resourceId,
            final UpdateRequest request) {
        try {
            storeEntry(request.getContent(), resourceId);
            manager.reload();
            // Save the store to the repo (if clustered)
            saveStore();
            return newResultPromise(newResourceResponse(resourceId, null, request.getContent()));
        } catch (Exception e) {
            return newExceptionPromise(ResourceUtil.adapt(e));
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String resourceId,
            final DeleteRequest request) {
        try {
            if (!store.getStore().containsAlias(resourceId)) {
                return newExceptionPromise(ResourceException.newNotFoundException());
            } else {
                store.getStore().deleteEntry(resourceId);
                store.store();
                manager.reload();
                // Save the store to the repo (if clustered)
                saveStore();
                return newResultPromise(newResourceResponse(resourceId, null, new JsonValue(null)));
            }
        } catch (Exception e) {
            return newExceptionPromise(ResourceUtil.adapt(e));
        }
    }
    
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return newExceptionPromise(newNotSupportedException("Action operations are not supported"));
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return newExceptionPromise(newNotSupportedException("Action operations are not supported"));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return newExceptionPromise(newNotSupportedException("Patch operations are not supported"));
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler queryResourceHandler) {
        return newExceptionPromise(newNotSupportedException("Query operations are not supported"));
    }
    
    public abstract void createDefaultEntry(String alias) throws Exception;
    
    public boolean hasEntry(String alias) throws Exception {
        return store.getStore().containsAlias(alias);
    }

    protected abstract void storeEntry(JsonValue value, String alias) throws Exception;
    
    protected abstract JsonValue readEntry(String alias) throws Exception;
}
