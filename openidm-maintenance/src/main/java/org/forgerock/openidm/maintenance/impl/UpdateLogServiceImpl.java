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
package org.forgerock.openidm.maintenance.impl;

import static org.forgerock.json.JsonValue.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.maintenance.upgrade.UpdateFileLogEntry;
import org.forgerock.openidm.maintenance.upgrade.UpdateLogEntry;
import org.forgerock.openidm.maintenance.upgrade.UpdateLogService;
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
public class UpdateLogServiceImpl implements RequestHandler, UpdateLogService {

    private final static Logger logger = LoggerFactory.getLogger(UpdateService.class);

    public static final String PID = "org.forgerock.openidm.maintenance.update.log";

    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    private ConnectionFactory connectionFactory;

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
     * Service does not allow actions.
     */
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return new NotSupportedException("Not allowed on update log service").asPromise();
    }

    /**
     * Service does not allow creating entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        return new NotSupportedException("Not allowed on update log service").asPromise();
    }

    /**
     * Service does not support deleting entries..
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return new NotSupportedException("Not allowed on update log service").asPromise();
    }

    /**
     * Service does not support changing entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return new NotSupportedException("Not allowed on update log service").asPromise();
    }

    /**
     * Query update history objects (wrapper to conceal repo endpoint)
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            final QueryResourceHandler handler) {
        QueryRequest newRequest = Requests.copyOfQueryRequest(request).setResourcePath("repo/updates");
        try {
            QueryResponse result = connectionFactory.getConnection().query(
                    context, newRequest, new QueryResourceHandler() {
                        @Override
                        public boolean handleResource(ResourceResponse resourceResponse) {
                            return handler.handleResource(resourceResponse);
                        }
                    });
            return result.asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /**
     * Read an update history object (wrapper to conceal repo endpoint)
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        ReadRequest newRequest = Requests.copyOfReadRequest(request).setResourcePath("repo/updates");
        try {
            return connectionFactory.getConnection().read(context, newRequest).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }

    /**
     * Service does not support changing entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return new NotSupportedException("Not allowed on update history service").asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logUpdate(UpdateLogEntry entry) throws ResourceException {
        ResourceResponse response = connectionFactory.getConnection().create(ContextUtil.createInternalContext(),
                Requests.newCreateRequest("repo/updates", entry.toJson()));
        entry.setId(response.getContent().get("_id").asString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUpdate(UpdateLogEntry entry) throws ResourceException {
        connectionFactory.getConnection().update(ContextUtil.createInternalContext(),
                Requests.newUpdateRequest("repo/updates", entry.getId(), entry.toJson()));
    }
}
