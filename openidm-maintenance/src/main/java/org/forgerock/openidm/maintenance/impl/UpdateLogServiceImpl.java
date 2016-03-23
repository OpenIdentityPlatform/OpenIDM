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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.impl;

import static org.forgerock.json.resource.Requests.copyOfQueryRequest;
import static org.forgerock.json.resource.Requests.copyOfReadRequest;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.ResourcePath.resourcePath;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.maintenance.upgrade.UpdateLogEntry;
import org.forgerock.openidm.maintenance.upgrade.UpdateLogService;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint for managing history of product updates.
 */
@Component(name = UpdateLogServiceImpl.PID, policy = ConfigurationPolicy.IGNORE, metatype = true,
        description = "OpenIDM Product Update Log Service", immediate = true)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Product Update Log Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/maintenance/update/log/*")
})
public class UpdateLogServiceImpl extends AbstractRequestHandler implements UpdateLogService {

    private final static Logger logger = LoggerFactory.getLogger(UpdateService.class);

    public static final String PID = "org.forgerock.openidm.maintenance.update.log";

    /** ResourcePath to store the updates. */
    private static final ResourcePath updateStore = resourcePath("repo/updates");

    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC)
    private IDMConnectionFactory connectionFactory;

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Update Log service {}", compContext.getProperties());
        logger.info("Update Log service started.");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Update Log Service {}", compContext.getProperties());
        logger.info("Update Log service stopped.");
    }

    /**
     * Query update history objects (wrapper to conceal repo endpoint)
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            final QueryResourceHandler handler) {
        try {
            return connectionFactory.getConnection().queryAsync(
                    new UpdateContext(context),
                    copyOfQueryRequest(request).setResourcePath(updateStore),
                    handler);
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /**
     * Read an update history object (wrapper to conceal repo endpoint)
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            return connectionFactory.getConnection().readAsync(
                    new UpdateContext(context),
                    copyOfReadRequest(request).setResourcePath(updateStore.concat(request.getResourcePathObject())));
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    // UpdateLogService implementation

    /**
     * {@inheritDoc}
     */
    @Override
    public void logUpdate(UpdateLogEntry entry) throws ResourceException {
        ResourceResponse response = connectionFactory.getConnection().create(
                new UpdateContext(ContextUtil.createInternalContext()),
                newCreateRequest(updateStore, entry.toJson()));
        entry.setId(response.getContent().get("_id").asString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUpdate(UpdateLogEntry entry) throws ResourceException {
        connectionFactory.getConnection().update(
                new UpdateContext(ContextUtil.createInternalContext()),
                Requests.newUpdateRequest(updateStore, entry.getId(), entry.toJson()));
    }
}
