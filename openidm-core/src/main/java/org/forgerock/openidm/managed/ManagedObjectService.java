/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All Rights Reserved
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Route;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides access to managed objects.
 * 
 */
@Component(name = ManagedObjectService.PID, immediate = true,
        policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM managed objects service"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/managed*") })
public class ManagedObjectService implements RequestHandler {

    public static final String PID = "org.forgerock.openidm.managed";

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

    /** Route service. */
    @Reference(referenceInterface = RouteService.class,
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindSyncRoute",
            unbind = "unbindSyncRoute",
            cardinality = ReferenceCardinality.OPTIONAL_UNARY,
            target = "(" + ServerConstants.ROUTER_PREFIX + "=/sync*)")
    private final AtomicReference<RouteService> syncRoute = new AtomicReference<RouteService>();

    private void bindSyncRoute(final RouteService service) {
        syncRoute.set(service);
    }

    private void unbindSyncRoute(final RouteService service) {
        syncRoute.set(null);
    }

    /* The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    private final ConcurrentMap<String, Route> managedRoutes = new ConcurrentHashMap<String, Route>();

    private final Router managedRouter = new Router();


    /**
     * TODO: Description.
     * 
     * @param context
     *            TODO.
     */
    @Activate
    protected void activate(ComponentContext context) throws Exception {
        JsonValue configuration = enhancedConfig.getConfigurationAsJson(context);
        for (JsonValue value : configuration.get("objects").expect(List.class)) {
            ManagedObjectSet objectSet = new ManagedObjectSet(scriptRegistry, cryptoService, syncRoute, connectionFactory, value);
            if (managedRoutes.containsKey(objectSet.getName())) {
                throw new ComponentException("Duplicate definition of managed object type: " + objectSet.getName());
            }
            managedRoutes.put(objectSet.getName(),managedRouter.addRoute(objectSet.getTemplate(), objectSet));
        }
    }

    @Modified
    protected void modified(ComponentContext context) throws Exception {
        JsonValue configuration = enhancedConfig.getConfigurationAsJson(context);

        Set<String> tempRoutes = new HashSet<String>();
        for (JsonValue value : configuration.get("objects").expect(List.class)) {
            ManagedObjectSet objectSet = new ManagedObjectSet(scriptRegistry, cryptoService, syncRoute, connectionFactory, value);
            if (tempRoutes.contains(objectSet.getName())) {
                throw new ComponentException("Duplicate definition of managed object type: " + objectSet.getName());
            }
            Route oldRoute = managedRoutes.get(objectSet.getName());
            if (null != oldRoute) {
                managedRouter.removeRoute(oldRoute);
            }
            managedRoutes.put(objectSet.getName(),managedRouter.addRoute(objectSet.getTemplate(), objectSet));
            tempRoutes.add(objectSet.getName());
        }
        for (Map.Entry<String, Route> entry : managedRoutes.entrySet()){
           //Use ConcurrentMap to avoid ConcurrentModificationException with this iteration
            if (tempRoutes.contains(entry.getKey())) {
                continue;
            }
            managedRouter.removeRoute(managedRoutes.remove(entry.getKey()));
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
        managedRoutes.clear();
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
