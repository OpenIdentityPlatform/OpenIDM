/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;

import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;

import org.forgerock.openidm.maintenance.upgrade.UpgradeException;
import org.forgerock.openidm.maintenance.upgrade.UpgradeManager;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basis and entry point to initiate the product
 * maintenance and upgrade mechanisms over REST
 */
@Component(name = MaintnenanceService.PID, policy = ConfigurationPolicy.IGNORE,
description = "OpenIDM Product Upgrade Management Service", immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Product Maintenance Management Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/maintenance/*")
})
public class MaintnenanceService implements RequestHandler {

    private final static Logger logger = LoggerFactory.getLogger(MaintnenanceService.class);
    
    public static final String PID = "org.forgerock.openidm.maintenance";

    private boolean maintenanceEnabled = false;

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Maintenance service {}", compContext.getProperties());
        logger.info("Maintenance service started.");
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

    /**
     * Maintenance action support
     */
    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        switch (request.getActionAsEnum(Action.class)) {
            case status:
                handleMaintenanceStatus(handler);
                break;
            /*
            case enable:
                maintenanceEnabled = true;
                handleMaintenanceStatus(handler);
                break;
            case disable:
                maintenanceEnabled = false;
                handleMaintenanceStatus(handler);
                break;
            case upgrade:
                beginMaintenance();
                // attempt to perform an upgrade via REST
                try {
                    new UpgradeManager()
                            .execute(request.getAdditionalParameter("url"),
                                    IdentityServer.getFileForWorkingPath(""),
                                    IdentityServer.getFileForInstallPath(""),
                                    request.getAdditionalParameters());
                } catch (InvalidArgsException ex) {
                    handler.handleError(new BadRequestException(ex.getMessage(), ex));
                } catch (UpgradeException ex) {
                    handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
                } finally {
                    endMaintenance();
                }
                break;
            */
            default:
                handler.handleError(new NotSupportedException(request.getAction() + " is not supported"));
                break;
        }
    }

    private void beginMaintenance() {
        maintenanceEnabled = true;

    }

    private void endMaintenance() {
        maintenanceEnabled = false;

    }

    private void handleMaintenanceStatus(ResultHandler<JsonValue> handler) {
        handler.handleResult(json(object(field("maintenanceEnabled", maintenanceEnabled))));
    }

    /**
     * Service does not allow creating entries.
     */
    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Not allowed on maintenance service"));
    }


    /**
     * Service does not support deleting entries..
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Not allowed on maintenance service"));
    }

    /**
     * Service does not support changing entries.
     */
    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Not allowed on maintenance service"));
    }

    /**
     * Service does not support querying entries yet.
     */
    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleError(new ForbiddenException("Not allowed on maintenance service"));
    }

    /**
     * Service does not support reading entries yet.
     */
    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Not allowed on maintenance service"));
    }


    /**
     * Service does not support changing entries.
     */
    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Not allowed on maintenance service"));
    }

}
