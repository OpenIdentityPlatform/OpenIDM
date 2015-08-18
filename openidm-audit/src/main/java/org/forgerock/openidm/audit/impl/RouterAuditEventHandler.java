/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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
package org.forgerock.openidm.audit.impl;

import static org.forgerock.json.resource.Requests.copyOfQueryRequest;
import static org.forgerock.json.resource.Requests.copyOfReadRequest;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.openidm.util.ResourceUtil.adapt;
import static org.forgerock.openidm.util.ResourceUtil.notSupported;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.audit.DependencyProvider;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.http.Context;
import org.forgerock.http.ResourcePath;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit event handler that logs to a router target.
 */
public class RouterAuditEventHandler extends AuditEventHandlerBase<RouterAuditEventHandlerConfiguration> {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(RouterAuditEventHandler.class);

    /** router target resource path */
    private ResourcePath resourcePath;

    /** the DependencyProvider to provide access to the ConnectionFactory */
    private DependencyProvider dependencyProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(RouterAuditEventHandlerConfiguration config) throws ResourceException {
        resourcePath = ResourcePath.valueOf(config.getResourcePath());
        logger.info("Audit logging to: {}", resourcePath.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDependencyProvider(DependencyProvider provider) {
        this.dependencyProvider = provider;
    }

    private ConnectionFactory getConnectionFactory() throws ClassNotFoundException {
        return dependencyProvider.getDependency(ConnectionFactory.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ResourceException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest actionRequest) {
        return newExceptionPromise(notSupported(actionRequest));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest actionRequest) {
        return newExceptionPromise(notSupported(actionRequest));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        try {

            return newResultPromise(getConnectionFactory().getConnection().create(new AuditContext(context),
                    newCreateRequest(
                            resourcePath.child(request.getResourcePath()),
                            request.getNewResourceId(),
                            request.getContent())));
        } catch (Exception e) {
            return newExceptionPromise(adapt(e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            final QueryResourceHandler queryResourceHandler) {
        try {
            final ConnectionFactory connectionFactory = getConnectionFactory();
            final QueryRequest newRequest = copyOfQueryRequest(request);
            newRequest.setResourcePath(resourcePath.child(request.getResourcePath()));

            return newResultPromise(
                    connectionFactory.getConnection().query(new AuditContext(context), newRequest,
                            new QueryResourceHandler() {
                                @Override
                                public boolean handleResource(ResourceResponse resourceResponse) {
                                    return queryResourceHandler.handleResource(resourceResponse);
                                }
                            }));

        } catch (Exception e) {
            return newExceptionPromise(adapt(e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        try {
            final ReadRequest newRequest = copyOfReadRequest(request);
            newRequest.setResourcePath(resourcePath.child(request.getResourcePath()).child(resourceId));
            return newResultPromise(getConnectionFactory().getConnection().read(new AuditContext(context), newRequest));
        } catch (Exception e) {
            return newExceptionPromise(adapt(e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<RouterAuditEventHandlerConfiguration> getConfigurationClass() {
        return RouterAuditEventHandlerConfiguration.class;
    }

}
