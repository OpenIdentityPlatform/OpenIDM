/*
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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;

import java.util.concurrent.Semaphore;

import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.openidm.servlet.MaintenanceFilterWrapper;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
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
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basis and entry point to initiate the product maintenance and upgrade mechanisms over REST
 */
@Component(name = MaintenanceService.PID, policy = ConfigurationPolicy.IGNORE, metatype = true,
        description = "OpenIDM Product Upgrade Management Service", immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Product Maintenance Management Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/maintenance/*")
})
public class MaintenanceService implements RequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(MaintenanceService.class);
    
    public static final String PID = "org.forgerock.openidm.maintenance";

    @Reference(policy = ReferencePolicy.STATIC, target = "(service.pid=org.forgerock.openidm.maintenancemodefilter)")
    private MaintenanceFilterWrapper maintenanceFilterWrapper;

    public void bindMaintenanceFilterWrapper(MaintenanceFilterWrapper mfw) {
        maintenanceFilterWrapper = mfw;
    }

    /**
     * A boolean indicating if maintenance mode is currently enabled
     */
    private boolean maintenanceEnabled = false;
    
    /**
     * A lock used in the enabling and disabling of maintenance mode.
     */
    private Semaphore maintenanceModeLock = new Semaphore(1);
    
    /**
     * An array of component names representing the components to disable during maintenance mode.
     */
    private String[] maintenanceModeComponents;
    
    /**
     * The SCR Service managed used to activate/deactivate components.
     */
    protected ScrService scrService;


    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Maintenance service {}", compContext.getProperties());
        logger.info("Maintenance service started.");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        logger.info("Maintenance service stopped.");
        maintenanceFilterWrapper.disable();
        maintenanceEnabled = false;
    }

    private enum Action {
        status,
        enable,
        disable
    }
    
    protected void setMaintenanceModeComponents(String[] components) {
        this.maintenanceModeComponents = components;
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
                    enableMaintenanceMode();
                    return handleMaintenanceStatus();
                case disable:
                    disableMaintenanceMode();
                    return handleMaintenanceStatus();
                default:
                    return new NotSupportedException(request.getAction() + " is not supported").asPromise();
            }
        } catch (ResourceException e) {
            return new InternalServerErrorException("Error processing Action request", e).asPromise();
        }
    }

    /**
     * Enables maintenance mode by disabling the currently active (or unsatisfied) components contained in 
     * the list of maintenance mode components.
     * 
     * @throws ResourceException if an error occurs when attempting to enable maintenance mode
     */
    private void enableMaintenanceMode() throws ResourceException {
        if (maintenanceModeLock.tryAcquire()) {
            if (!maintenanceEnabled) {
                maintenanceFilterWrapper.enable();
                maintenanceEnabled = true;
            }
            maintenanceModeLock.release();
        } else {
            throw new InternalServerErrorException("Cannot enable maintenance mode, change is already in progress");
        }
    }

    /**
     * Disables maintenance mode by enabling the currently disabled components contained in the list of 
     * maintenance mode components.
     * 
     * @throws ResourceException if an error occurs when attempting to enable maintenance mode
     */
    private void disableMaintenanceMode() throws ResourceException {
        if (maintenanceModeLock.tryAcquire()) {
            if (maintenanceEnabled) {
                maintenanceFilterWrapper.disable();
                maintenanceEnabled = false;
            }
            maintenanceModeLock.release();
        } else {
            throw new InternalServerErrorException("Cannot disable maintenance mode, change is already in progress");
        }

    }

    private Promise<ActionResponse, ResourceException> handleMaintenanceStatus() {
        return newActionResponse(json(object(field("maintenanceEnabled", maintenanceEnabled)))).asPromise();
    }

    /**
     * Service does not allow creating entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        return new NotSupportedException("Not allowed on maintenance service").asPromise();
    }

    /**
     * Service does not support deleting entries..
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return new NotSupportedException("Not allowed on maintenance service").asPromise();
    }

    /**
     * Service does not support changing entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return new NotSupportedException("Not allowed on maintenance service").asPromise();
    }

    /**
     * Service does not support querying entries yet.
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return new NotSupportedException("Not allowed on maintenance service").asPromise();
    }

    /**
     * Service does not support reading entries yet.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        return new NotSupportedException("Not allowed on maintenance service").asPromise();
    }


    /**
     * Service does not support changing entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return new NotSupportedException("Not allowed on maintenance service").asPromise();
    }
}
