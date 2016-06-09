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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newActionResponse;

import java.nio.file.Path;
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
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Responses;
import org.forgerock.openidm.maintenance.upgrade.UpdateException;
import org.forgerock.openidm.maintenance.upgrade.UpdateManager;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
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
public class UpdateService extends AbstractRequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(UpdateService.class);

    public static final String PID = "org.forgerock.openidm.maintenance.update";

    private static final String ARCHIVE_NAME = "archive";
    private static final String UPDATE_ID = "updateId";
    private static final String ARCHIVE_DIRECTORY = "/bin/update/";
    private static final String ACCEPT_LICENSE_PARAMETER = "acceptLicense";

    @Reference(policy=ReferencePolicy.STATIC)
    private UpdateManager updateManager;

    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC)
    private IDMConnectionFactory connectionFactory;

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Update service {}", compContext.getProperties());

        // Ensure archive directory exists
        if (!Paths.get(IdentityServer.getInstance().getInstallLocation() + ARCHIVE_DIRECTORY).toFile().exists()) {
            Paths.get(IdentityServer.getInstance().getInstallLocation() + ARCHIVE_DIRECTORY).toFile().mkdirs();
        }

        logger.info("Update service started.");

    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        logger.info("Update service stopped.");
    }

    private enum Action {
        available,
        listRepoUpdates,
        preview,
        update,
        getLicense,
        markComplete,
        restart,
        lastUpdateId,
        installed
    }

    /**
     * Update action support
     */
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        switch (request.getActionAsEnum(Action.class)) {
            case available:
                return handleListAvailable();
            case listRepoUpdates:
                return handleListRepoUpdates(request.getAdditionalParameters());
            case preview:
                return handlePreviewUpdate(request.getAdditionalParameters());
            case update:
                return handleInstallUpdate(request.getAdditionalParameters(),
                        context.asContext(SecurityContext.class).getAuthenticationId());
            case getLicense:
                return handleLicense(request.getAdditionalParameters());
            case markComplete:
                return handleMarkComplete(request.getAdditionalParameters());
            case restart:
                updateManager.restartNow();
                return newActionResponse(json(object())).asPromise();
            case lastUpdateId:
                return newActionResponse(json(object(field("lastUpdateId", updateManager.getLastUpdateId()))))
                        .asPromise();
            case installed:
                return handleGetInstalledUpdates(context);
            default:
                return new NotSupportedException(request.getAction() + " is not supported").asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleMarkComplete(Map<String, String> additionalParameters) {
        if (!additionalParameters.containsKey(UPDATE_ID)) {
            return new BadRequestException(UPDATE_ID + " not specified").asPromise();
        }

        try {
            return newActionResponse(updateManager.completeRepoUpdates(additionalParameters.get(UPDATE_ID))).asPromise();
        } catch (UpdateException e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleListRepoUpdates(
            Map<String, String> additionalParameters) {
        if (!additionalParameters.containsKey(ARCHIVE_NAME)) {
            return new BadRequestException("Archive name not specified.").asPromise();
        }

        try {
            return newActionResponse(updateManager.listRepoUpdates(
                    archivePath(additionalParameters.get(ARCHIVE_NAME))
            )).asPromise();
        } catch (UpdateException e) {
            return new InternalServerErrorException(e).asPromise();
        }

    }

    // FIXME - Seems like this should be in UpdateManager
    public Path archivePath(String archiveName) {
        return Paths.get(IdentityServer.getInstance().getInstallLocation() + ARCHIVE_DIRECTORY + archiveName);
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
                    archivePath(parameters.get(ARCHIVE_NAME)),
                    IdentityServer.getInstance().getInstallLocation().toPath()
            )).asPromise();
        } catch (UpdateException e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleInstallUpdate(Map<String, String> parameters,
            String userName) {
        try {
            if (!parameters.containsKey(ARCHIVE_NAME)) {
                return new BadRequestException("Archive name not specified.").asPromise();
            }
            if (!Boolean.parseBoolean(parameters.get(ACCEPT_LICENSE_PARAMETER))) {
                return new BadRequestException("This update requires accepting the license.").asPromise();
            }

            try {
                ActionResponse response = connectionFactory.getConnection().action(new UpdateContext(),
                        Requests.newActionRequest("/maintenance", "status"));
                if (!response.getJsonContent().get("maintenanceEnabled").asBoolean().equals(Boolean.TRUE)) {
                    throw new UpdateException("Must be in maintenance mode prior to installing an update.");
                }
            } catch (ResourceException e) {
                throw new UpdateException("Unable to check maintenance mode status.", e);
            }

            return newActionResponse(updateManager.upgrade(
                    archivePath(parameters.get(ARCHIVE_NAME)),
                    IdentityServer.getInstance().getInstallLocation().toPath(), userName
            )).asPromise();
        } catch (UpdateException e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleLicense(Map<String, String> parameters) {
        try {
            return newActionResponse(updateManager.getLicense(
                    archivePath(parameters.get(ARCHIVE_NAME))
            )).asPromise();
        } catch (UpdateException e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    private Promise<ActionResponse, ResourceException> handleGetInstalledUpdates(Context context) {
        try {
            QueryRequest query = Requests.newQueryRequest("repo/updates")
                    .setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());

            final JsonValue results = json(array());
            connectionFactory.getConnection().query(
                    new UpdateContext(context), query, new QueryResourceHandler() {
                        @Override
                        public boolean handleResource(ResourceResponse resource) {
                            results.add(object(
                                    field("archive", resource.getContent().get("archive").asString()),
                                    field("status", resource.getContent().get("status").asString()),
                                    field("completedTasks", resource.getContent().get("completedTasks").asInteger()),
                                    field("totalTasks", resource.getContent().get("totalTasks").asInteger()),
                                    field("startDate", resource.getContent().get("startDate").asString()),
                                    field("endDate", resource.getContent().get("endDate").asString()),
                                    field("userName", resource.getContent().get("userName").asString()),
                                    field("statusMessage", resource.getContent().get("statusMessage").asString())
                            ));
                            return true;
                        }
                    });

            return Responses.newActionResponse(results).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        }
    }
}
