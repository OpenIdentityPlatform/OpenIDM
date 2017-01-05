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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Schema;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.maintenance.impl.api.CallActionResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basis and entry point to initiate the product maintenance and update mechanisms over REST
 */
@org.forgerock.api.annotations.RequestHandler(@Handler(
        id = "maintenanceService:0",
        title = "Maintenance",
        description = "Basis and entry point to initiate the product maintenance and update mechanisms over REST.",
        mvccSupported = false))
@Component(name = MaintenanceService.PID, policy = ConfigurationPolicy.IGNORE, metatype = true,
        description = "OpenIDM Product Upgrade Management Service", immediate = true)
@Service({ RequestHandler.class })
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Product Maintenance Management Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/maintenance/*")
})
public class MaintenanceService extends AbstractRequestHandler {

    static final String PID = "org.forgerock.openidm.maintenance";

    private final static Logger logger = LoggerFactory.getLogger(MaintenanceService.class);

    @Reference
    private MaintenanceFilter maintenanceFilter;

    /**
     * A boolean indicating if maintenance mode is currently enabled
     */
    private final AtomicBoolean maintenanceEnabled = new AtomicBoolean(false);

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Maintenance service {}", compContext.getProperties());
        logger.info("Maintenance service started.");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        logger.info("Maintenance service stopped.");
        maintenanceEnabled.set(false);
    }

    private enum Action {
        status,
        enable,
        disable
    }

    /**
     * Maintenance action support
     */
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            switch (request.getActionAsEnum(Action.class)) {
                case status:
                    return handleMaintenanceStatus();
                case enable:
                    return handleMaintenanceEnable();
                case disable:
                    return handleMaintenanceDisable();
                default:
                    return new NotSupportedException(request.getAction() + " is not supported").asPromise();
            }
        } catch (Exception e) {
            return new InternalServerErrorException("Error processing Action request", e).asPromise();
        }
    }

    /**
     * Enables maintenance mode by enabling the maintenance filter to disable modification by certain endpoints.
     *
     * @throws ResourceException if an error occurs when attempting to enable maintenance mode
     */
    private void  enableMaintenanceMode() {
        synchronized (maintenanceEnabled) {
            if (!maintenanceEnabled.getAndSet(true)) {
                maintenanceFilter.enableMaintenanceMode();
            }
        }
    }

    /**
     * Disables maintenance mode by disabling the maintenance filter to enable modification by all endpoints.
     *
     * @throws ResourceException if an error occurs when attempting to enable maintenance mode
     */
    private void disableMaintenanceMode() {
        synchronized (maintenanceEnabled) {
            if (maintenanceEnabled.getAndSet(false)) {
                maintenanceFilter.disableMaintenanceMode();
            }
        }
    }

    @org.forgerock.api.annotations.Action(
            operationDescription = @Operation(
                    description = "Reads maintenance status.",
                    errorRefs = "frapi:common#/errors/badRequest"),
            name = "status",
            response = @Schema(fromType = CallActionResponse.class)
    )
    public Promise<ActionResponse, ResourceException> handleMaintenanceStatus() {
        return newActionResponse(json(object(field("maintenanceEnabled", maintenanceEnabled.get())))).asPromise();
    }

    @org.forgerock.api.annotations.Action(
            operationDescription = @Operation(
                    description = "Enables maintenance mode.",
                    errorRefs = "frapi:common#/errors/badRequest"),
            name = "enable",
            response = @Schema(fromType = CallActionResponse.class)
    )
    public Promise<ActionResponse, ResourceException> handleMaintenanceEnable() {
        enableMaintenanceMode();
        return handleMaintenanceStatus();
    }

    @org.forgerock.api.annotations.Action(
            operationDescription = @Operation(
                    description = "Disables maintenance mode.",
                    errorRefs = "frapi:common#/errors/badRequest"),
            name = "disable",
            response = @Schema(fromType = CallActionResponse.class)
    )
    public Promise<ActionResponse, ResourceException> handleMaintenanceDisable() {
        disableMaintenanceMode();
        return handleMaintenanceStatus();
    }
}
