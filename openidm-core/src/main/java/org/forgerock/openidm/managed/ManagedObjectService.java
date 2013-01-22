/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.managed;

import static org.forgerock.json.resource.RoutingMode.EQUALS;

import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides access to managed objects.
 * 
 * @author Paul C. Bryan
 */
@Component(name = "org.forgerock.openidm.managed", immediate = true,
        policy = ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM managed objects service"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "managed") })
public class ManagedObjectService implements RequestHandler {

    /**
     * Setup logging for the {@link ManagedObjectService}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ManagedObjectService.class);

    /** Cryptographic service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected CryptoService cryptoService;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected ScriptRegistry scriptRegistry;

    private Router managedRouter = new Router();

    /**
     * TODO: Description.
     * 
     * @param context
     *            TODO.
     */
    @Activate
    protected void activate(ComponentContext context) {
        JsonValue config = new JsonValue(new JSONEnhancedConfig().getConfiguration(context));
        try {
            for (JsonValue value : config.get("objects").expect(List.class)) {
                ManagedObjectSet objectSet =
                        new ManagedObjectSet(scriptRegistry, cryptoService, value);
                String name = objectSet.getName();
                // TODO Fix this check
                // if (routes.containsKey(name)) {
                // throw new JsonValueException(value, "object " + name +
                // " already defined");
                // }
                managedRouter.addRoute(EQUALS, name, objectSet);
            }
        } catch (Exception jve) {
            throw new ComponentException("Configuration error", jve);
        }
    }

    /**
     * TODO: Description.
     * 
     * @param context
     *            TODO.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        managedRouter.removeAllRoutes();
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        managedRouter.handleAction(context, request, handler);
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        managedRouter.handleCreate(context, request, handler);
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        managedRouter.handleDelete(context, request, handler);
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        managedRouter.handlePatch(context, request, handler);
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        managedRouter.handleQuery(context, request, handler);
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        managedRouter.handleRead(context, request, handler);
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        managedRouter.handleUpdate(context, request, handler);
    }
}
