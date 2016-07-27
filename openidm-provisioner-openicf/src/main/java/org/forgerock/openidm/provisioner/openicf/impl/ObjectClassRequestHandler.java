/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.openicf.impl;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

class ObjectClassRequestHandler implements RequestHandler {

    public static final String OBJECTCLASS = "objectclass";
    public static final String OBJECTCLASS_TEMPLATE = "/{objectclass}";

    private final ConcurrentMap<String, RequestHandler> objectClassHandlers;

    public ObjectClassRequestHandler(ConcurrentMap<String, RequestHandler> objectClassHandlers) {
        this.objectClassHandlers = objectClassHandlers;
    }

    protected String getObjectClass(Context context) throws ResourceException {
        Map<String, String> variables = ResourceUtil.getUriTemplateVariables(context);
        if (null != variables && variables.containsKey(OBJECTCLASS)) {
            return variables.get(OBJECTCLASS);
        }
        throw new ForbiddenException(
                "Direct access without Router to this service is forbidden.");
    }

    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            String objectClass = getObjectClass(context);
            RequestHandler delegate = objectClassHandlers.get(objectClass);
            if (null != delegate) {
                return delegate.handleAction(context, request);
            } else {
                throw new NotFoundException("Not found: " + objectClass);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        try {
            String objectClass = getObjectClass(context);
            RequestHandler delegate = objectClassHandlers.get(objectClass);
            if (null != delegate) {
                return delegate.handleCreate(context, request);
            } else {
                throw new NotFoundException("Not found: " + objectClass);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        try {
            String objectClass = getObjectClass(context);
            RequestHandler delegate = objectClassHandlers.get(objectClass);
            if (null != delegate) {
                return delegate.handleDelete(context, request);
            } else {
                throw new NotFoundException("Not found: " + objectClass);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        try {
            String objectClass = getObjectClass(context);
            RequestHandler delegate = objectClassHandlers.get(objectClass);
            if (null != delegate) {
                return delegate.handlePatch(context, request);
            } else {
                throw new NotFoundException("Not found: " + objectClass);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        try {
            String objectClass = getObjectClass(context);
            RequestHandler delegate = objectClassHandlers.get(objectClass);
            if (null != delegate) {
                return delegate.handleQuery(context, request, handler);
            } else {
                throw new NotFoundException("Not found: " + objectClass);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            String objectClass = getObjectClass(context);
            RequestHandler delegate = objectClassHandlers.get(objectClass);
            if (null != delegate) {
                return delegate.handleRead(context, request);
            } else {
                throw new NotFoundException("Not found: " + objectClass);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        try {
            String objectClass = getObjectClass(context);
            RequestHandler delegate = objectClassHandlers.get(objectClass);
            if (null != delegate) {
                return delegate.handleUpdate(context, request);
            } else {
                throw new NotFoundException("Not found: " + objectClass);
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }
}
