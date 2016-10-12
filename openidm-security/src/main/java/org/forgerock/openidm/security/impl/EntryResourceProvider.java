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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openidm.security.impl;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.security.KeyStore;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.keystore.KeyStoreManagementService;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A generic collection resource provider servicing requests on entries in a keystore
 */
public abstract class EntryResourceProvider extends SecurityResourceProvider implements CollectionResourceProvider {

    final KeyStoreManagementService keyStoreManager;

    public EntryResourceProvider(String resourceName, KeyStore keyStore, KeyStoreService keyStoreService,
            RepositoryService repoService, CryptoService cryptoService, KeyStoreManagementService keyStoreManager) {
        super(resourceName, keyStore, keyStoreService, repoService, cryptoService);
        this.keyStoreManager = keyStoreManager;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context,
            final CreateRequest request) {
        try {
            if (null != request.getNewResourceId()) {
                if (keyStore.containsAlias(request.getNewResourceId())) {
                    return new ConflictException("The resource with ID '" + request.getNewResourceId()
                            + "' could not be created because there is already another resource with the same ID")
                            .asPromise();
                } else {
                    String resourceId = request.getNewResourceId();
                    storeEntry(request.getContent(), resourceId);
                    keyStoreManager.reloadSslContext();
                    return newResourceResponse(resourceId, null, request.getContent()).asPromise();
                }
            } else {
                return new BadRequestException("A valid resource ID must be specified in the request").asPromise();
            }
        } catch (Exception e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(final Context context, final String resourceId,
            final ReadRequest request) {
        try {
            if (!keyStore.containsAlias(resourceId)) {
                return new NotFoundException().asPromise();
            } else {
                JsonValue result = readEntry(resourceId);
                return newResourceResponse(resourceId, null, result).asPromise();
            }
        } catch (Exception e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context, final String resourceId,
            final UpdateRequest request) {
        try {
            storeEntry(request.getContent(), resourceId);
            keyStoreManager.reloadSslContext();
            return newResourceResponse(resourceId, null, request.getContent()).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String resourceId,
            final DeleteRequest request) {
        try {
            if (!keyStore.containsAlias(resourceId)) {
                return new NotFoundException().asPromise();
            } else {
                keyStore.deleteEntry(resourceId);
                keyStoreService.store();
                keyStoreManager.reloadSslContext();
                return newResourceResponse(resourceId, null, json(object())).asPromise();
            }
        } catch (Exception e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }
    
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return new NotSupportedException("Action operations are not supported").asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return new NotSupportedException("Action operations are not supported").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return new NotSupportedException("Patch operations are not supported").asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler queryResourceHandler) {
        return new NotSupportedException("Query operations are not supported").asPromise();
    }

    protected abstract void storeEntry(JsonValue value, String alias) throws Exception;
    
    protected abstract JsonValue readEntry(String alias) throws Exception;
}
