/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.maintenance.impl;

import static org.forgerock.json.resource.Responses.newActionResponse;

import java.nio.file.Paths;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.openidm.maintenance.upgrade.UpdateException;
import org.forgerock.openidm.maintenance.upgrade.UpdateManager;
import org.forgerock.openidm.maintenance.upgrade.UpdateManagerImpl;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
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
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basis and entry point to initiate the product maintenance and upgrade mechanisms over REST
 */
@Component(name = UpdateService.PID, policy = ConfigurationPolicy.IGNORE, metatype = true,
        description = "OpenIDM Product Update Management Service", immediate = true)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Product Update Management Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/maintenance/update/*")
})
public class UpdateService implements RequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(UpdateService.class);

    public static final String PID = "org.forgerock.openidm.maintenance.update";

    private static final String ARCHIVE_NAME = "archive";
    private static final String ARCHIVE_DIRECTORY = "/bin/update/";

    @Reference(policy=ReferencePolicy.STATIC)
    private UpdateManager updateManager;

    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    private ConnectionFactory connectionFactory;

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Update service {}", compContext.getProperties());
        logger.info("Update service started.");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        logger.info("Update service stopped.");
    }

    private enum Action {
        available,
        preview,
        update,
        getLicense
    }

    /**
     * Update action support
     */
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        switch (request.getActionAsEnum(Action.class)) {
            case available:
                return handleListAvailable();
            case preview:
                return handlePreviewUpdate(request.getAdditionalParameters());
            case update:
                return handleInstallUpdate(request.getAdditionalParameters());
            case getLicense:
                return handleLicense(request.getAdditionalParameters());
            default:
                return new NotSupportedException(request.getAction() + " is not supported").asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleListAvailable() {
        try {
            return newActionResponse(updateManager.listAvailableUpdates()).asPromise();
        } catch (UpdateException e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handlePreviewUpdate(Map<String, String> parameters) {
        try {
            if (!parameters.containsKey(ARCHIVE_NAME)) {
                return new BadRequestException("Archive name not specified.").asPromise();
            }
            return newActionResponse(updateManager.report(
                    Paths.get(IdentityServer.getInstance().getInstallLocation() + ARCHIVE_DIRECTORY +
                            parameters.get(ARCHIVE_NAME)),
                    IdentityServer.getInstance().getInstallLocation().toPath())).asPromise();
        } catch (UpdateException e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleInstallUpdate(Map<String, String> parameters) {
        try {
            if (!parameters.containsKey(ARCHIVE_NAME)) {
                return new BadRequestException("Archive name not specified.").asPromise();
            }

            try {
                ActionResponse response = connectionFactory.getConnection().action(new RootContext(),
                        Requests.newActionRequest("/maintenance", "status"));
                if (!response.getJsonContent().get("maintenanceEnabled").asBoolean().equals(Boolean.TRUE)) {
                    throw new UpdateException("Must be in maintenance mode prior to installing an update.");
                }
            } catch (ResourceException e) {
                throw new UpdateException("Unable to check maintenance mode status.", e);
            }

            return newActionResponse(updateManager.upgrade(
                    Paths.get(IdentityServer.getInstance().getInstallLocation() + ARCHIVE_DIRECTORY +
                            parameters.get(ARCHIVE_NAME)),
                    IdentityServer.getInstance().getInstallLocation().toPath())).asPromise();
        } catch (UpdateException e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleLicense(Map<String, String> parameters) {
        try {
            return newActionResponse(updateManager.getLicense(
                    Paths.get(IdentityServer.getInstance().getInstallLocation() + ARCHIVE_DIRECTORY +
                            parameters.get(ARCHIVE_NAME)))).asPromise();
        } catch (UpdateException e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    /**
     * Service does not allow creating entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        return new NotSupportedException("Not allowed on update service").asPromise();
    }

    /**
     * Service does not support deleting entries..
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return new NotSupportedException("Not allowed on update service").asPromise();
    }

    /**
     * Service does not support changing entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return new NotSupportedException("Not allowed on update service").asPromise();
    }

    /**
     * Service does not support querying entries yet.
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return new NotSupportedException("Not allowed on update service").asPromise();
    }

    /**
     * Service does not support reading entries yet.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        return new NotSupportedException("Not allowed on update service").asPromise();
    }


    /**
     * Service does not support changing entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return new NotSupportedException("Not allowed on update service").asPromise();
    }
}
