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
import static org.forgerock.json.resource.ResourceException.newInternalServerErrorException;
import static org.forgerock.json.resource.ResourceException.newNotSupportedException;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.promise.Promises.newExceptionPromise;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.http.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
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

    /**
     * The default components to disable during maintenance mode.
     */
    private static final String[] DEFAULT_MAINTENANCE_MODE_COMPONENTS = new String[] {
        "org.forgerock.openidm.cluster",
        "org.forgerock.openidm.config.enhanced.starter",
        "org.forgerock.openidm.config.manage",
        "org.forgerock.openidm.endpoint",
        "org.forgerock.openidm.external.email",
        "org.forgerock.openidm.external.rest",
        "org.forgerock.openidm.health",
        "org.forgerock.openidm.info",
        "org.forgerock.openidm.managed",
        "org.forgerock.openidm.openicf.syncfailure",
        "org.forgerock.openidm.provisioner",
        "org.forgerock.openidm.provisioner.openicf",
        "org.forgerock.openidm.provisioner.openicf.connectorinfoprovider",
        "org.forgerock.openidm.recon",
        "org.forgerock.openidm.schedule",
        "org.forgerock.openidm.scheduler",
        "org.forgerock.openidm.security",        
        "org.forgerock.openidm.sync",
        "org.forgerock.openidm.taskscanner",
        "org.forgerock.openidm.workflow"
        
        /*
         * Components to leave enabled
         * 
        "org.forgerock.openidm.api-servlet",
        "org.forgerock.openidm.audit",
        "org.forgerock.openidm.authnfilter",
        "org.forgerock.openidm.config.enhanced",
        "org.forgerock.openidm.http.context",
        "org.forgerock.openidm.internal",
        "org.forgerock.openidm.policy",
        "org.forgerock.openidm.repo.jdbc",
        "org.forgerock.openidm.repo.orientdb",
        "org.forgerock.openidm.router",
        "org.forgerock.openidm.script",
        "org.forgerock.openidm.servletfilter",
        "org.forgerock.openidm.servletfilter.registrator",
        "org.forgerock.openidm.ui.context",
        */
    };
    

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
        
        BundleContext bundleContext = compContext.getBundleContext();
        ServiceReference<?> scrServiceRef = bundleContext.getServiceReference( ScrService.class.getName() );
        scrService = (ScrService) bundleContext.getService(scrServiceRef);
        setMaintenanceModeComponents(DEFAULT_MAINTENANCE_MODE_COMPONENTS);
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        logger.info("Maintenance service stopped.");
    }

    private enum Action {
        status,
        enable,
        disable,
        upgrade
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
                    return newExceptionPromise(newNotSupportedException(request.getAction() + " is not supported"));
            }
        } catch (ResourceException e) {
            return newExceptionPromise(newInternalServerErrorException("Error processing Action request", e));
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
            try {
                logger.info("Enabling maintenance mode");
                List<String> componentNames = Arrays.asList(maintenanceModeComponents);
                maintenanceEnabled = true;
                org.apache.felix.scr.Component[] components = scrService.getComponents();
                logger.debug("Found {} components", components.length);
                for (org.apache.felix.scr.Component component : components) {
                    if (componentNames.contains(component.getName())
                            && (component.getState() == org.apache.felix.scr.Component.STATE_UNSATISFIED
                                    || component.getState() == org.apache.felix.scr.Component.STATE_ACTIVE || component
                                    .getState() == org.apache.felix.scr.Component.STATE_ACTIVATING)) {
                        logger.info("Disabling component id: {}, name: {}", component.getId(), component.getName());
                        component.disable();
                    } else {
                        logger.debug("Ignoring component id: {}, name: {}", component.getId(), component.getName());
                    }
                }
            } finally {
                maintenanceModeLock.release();
            }
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
            try {
                logger.info("Disabling maintenance mode");
                List<String> componentNames = Arrays.asList(maintenanceModeComponents);
                maintenanceEnabled = false;
                org.apache.felix.scr.Component[] components = scrService.getComponents();
                logger.debug("Found {} components", components.length);
                for (org.apache.felix.scr.Component component : components) {
                    if (componentNames.contains(component.getName()) 
                            && (component.getState() == org.apache.felix.scr.Component.STATE_DISABLED
                            || component.getState() == org.apache.felix.scr.Component.STATE_DISABLING)) {
                        logger.info("Enabling component id: {}, name: {}", component.getId(), component.getName());
                        component.enable();
                    } else {
                        logger.debug("Ignoring component id: {}, name: {}", component.getId(), component.getName());
                    }
                }
            } finally {
                maintenanceModeLock.release();
            }
        } else {
            throw new InternalServerErrorException("Cannot disable maintenance mode, change is already in progress");
        }

    }

    private Promise<ActionResponse, ResourceException> handleMaintenanceStatus() {
        return newResultPromise(newActionResponse(json(object(field("maintenanceEnabled", maintenanceEnabled)))));
    }

    /**
     * Service does not allow creating entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        return newExceptionPromise(newNotSupportedException("Not allowed on maintenance service"));
    }

    /**
     * Service does not support deleting entries..
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return newExceptionPromise(newNotSupportedException("Not allowed on maintenance service"));
    }

    /**
     * Service does not support changing entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return newExceptionPromise(newNotSupportedException("Not allowed on maintenance service"));
    }

    /**
     * Service does not support querying entries yet.
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return newExceptionPromise(newNotSupportedException("Not allowed on maintenance service"));
    }

    /**
     * Service does not support reading entries yet.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        return newExceptionPromise(newNotSupportedException("Not allowed on maintenance service"));
    }


    /**
     * Service does not support changing entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return newExceptionPromise(newNotSupportedException("Not allowed on maintenance service"));
    }
}
