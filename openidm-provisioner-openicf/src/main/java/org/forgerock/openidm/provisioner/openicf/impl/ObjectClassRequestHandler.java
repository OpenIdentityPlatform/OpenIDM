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

import static java.util.Collections.unmodifiableMap;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
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
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.Promise;
import org.identityconnectors.framework.common.objects.ObjectClass;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acts as a router that delegates routes to object-class request handlers.
 */
class ObjectClassRequestHandler implements RequestHandler, Describable<ApiDescription, Request> {

    private final ConcurrentMap<String, RequestHandler> objectClassHandlers;
    private final ApiDescription apiDescription;
    private final Pattern objectClassPattern;

    public ObjectClassRequestHandler(final ConcurrentMap<String, RequestHandler> objectClassHandlers) {
        this.objectClassHandlers = objectClassHandlers;
        objectClassPattern = buildObjectClassPattern(objectClassHandlers.keySet());
        apiDescription = ObjectClassRequestHandlerApiDescription.build(unmodifiableMap(objectClassHandlers));
    }

    private Pattern buildObjectClassPattern(final Set<String> objectClassSet) {
        // build regex with format "^(account|manager|)(?:/(.*))?$"
        String regex = "^(";
        for (final String objectClass : objectClassSet) {
            if (!ObjectClass.ALL_NAME.equals(objectClass)) {
                regex += Pattern.quote(objectClass) + '|';
            }
        }
        regex += ")(?:/(.*))?$";
        return Pattern.compile(regex);
    }

    private RequestHandler getObjectClassHandler(final Request request) throws ResourceException {
        final String resourcePath = request.getResourcePath();
        if (resourcePath != null && !resourcePath.isEmpty()) {
            final Matcher m = objectClassPattern.matcher(resourcePath);
            if (m.matches()) {
                // lookup handler by object-class name
                final String objectClass = m.group(1);
                final RequestHandler requestHandler = objectClassHandlers.get(objectClass);
                if (requestHandler == null) {
                    throw new NotFoundException("Not found: " + objectClass);
                }

                // set the resource-path to the sub-resource of the object-class
                final String subresourcePath = m.group(2);
                request.setResourcePath(subresourcePath == null ? "" : subresourcePath);

                return requestHandler;
            }
            throw new NotFoundException("Resource path not found: " + resourcePath);
        }
        throw new ForbiddenException("Direct access to this service is forbidden");
    }

    public Promise<ActionResponse, ResourceException> handleAction(final Context context, final ActionRequest request) {
        try {
            return getObjectClassHandler(request).handleAction(context, request);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context, final CreateRequest request) {
        try {
            return getObjectClassHandler(request).handleCreate(context, request);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context, final DeleteRequest request) {
        try {
            return getObjectClassHandler(request).handleDelete(context, request);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context, final PatchRequest request) {
        try {
            return getObjectClassHandler(request).handlePatch(context, request);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request,
            QueryResourceHandler handler) {
        try {
            return getObjectClassHandler(request).handleQuery(context, request, handler);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleRead(final Context context, final ReadRequest request) {
        try {
            return getObjectClassHandler(request).handleRead(context, request);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context, final UpdateRequest request) {
        try {
            return getObjectClassHandler(request).handleUpdate(context, request);
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public ApiDescription api(final ApiProducer<ApiDescription> apiProducer) {
        return apiDescription;
    }

    @Override
    public ApiDescription handleApiRequest(final Context context, final Request request) {
        return apiDescription;
    }

    @Override
    public void addDescriptorListener(final Listener listener) {
        // empty
    }

    @Override
    public void removeDescriptorListener(final Listener listener) {
        // empty
    }
}
