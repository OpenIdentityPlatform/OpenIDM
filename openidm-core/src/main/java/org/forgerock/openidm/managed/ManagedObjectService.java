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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.managed;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// OSGi
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
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
import org.forgerock.script.ScriptRegistry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

// Felix SCR
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import static org.forgerock.json.resource.RoutingMode.EQUALS;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.crypto.CryptoService;


/**
 * Provides access to managed objects.
 *
 * @author Paul C. Bryan
 */
@Component(
    name = "org.forgerock.openidm.managed",
    immediate = true,
    policy = ConfigurationPolicy.REQUIRE
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM managed objects service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = "managed")
})
@Service
public class ManagedObjectService implements RequestHandler {

    /** Internal object set router service. */
//    @Reference(
//        name = "ref_ManagedObjectService_JsonResourceRouterService",
//        referenceInterface = JsonResource.class,
//        bind = "bindRouter",
//        unbind = "unbindRouter",
//        cardinality = ReferenceCardinality.MANDATORY_UNARY,
//        policy = ReferencePolicy.DYNAMIC,
//        target = "(service.pid=org.forgerock.openidm.router)"
//    )
//    protected ObjectSet router;
//    protected void bindRouter(JsonResource router) {
//        this.router = new JsonResourceObjectSet(router);
//    }
//    protected void unbindRouter(JsonResource router) {
//        this.router = null;
//    }

// TODO: Use router to send notifications to synchronization service.
//    /** Synchronization listeners. */
//    @Reference(
//        name="ref_ManagedObjectService_SynchronizationListener",
//        referenceInterface=SynchronizationListener.class,
//        bind="bindListener",
//        unbind="unbindListener",
//        cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
//        policy=ReferencePolicy.DYNAMIC,
//        strategy=ReferenceStrategy.EVENT
//    )
//    protected final HashSet<SynchronizationListener> listeners = new HashSet<SynchronizationListener>();
//    protected void bindListener(SynchronizationListener listener) {
//        listeners.add(listener);
//    }
//    protected void unbindListener(SynchronizationListener listener) {
//        listeners.remove(listener);
//    }

    /** Cryptographic service. */
    @Reference(
        referenceInterface=CryptoService.class,
        policy = ReferencePolicy.DYNAMIC
    )
    protected CryptoService cryptoService;

//    /** Router service. */
//    @Reference(
//            referenceInterface=CryptoService.class,
//            policy = ReferencePolicy.DYNAMIC
//    )
//    protected Router routerService;


    private Router managedRouter = new Router();

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scriptRegistry;

    /** TODO: Description. */
    private ComponentContext context;

    /**
     * TODO: Description.
     *
     * @param context TODO.
     */
    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
        JsonValue config = new JsonValue(new JSONEnhancedConfig().getConfiguration(context));
        try {
            for (JsonValue value : config.get("objects").expect(List.class)) {
                ManagedObjectSet objectSet = new ManagedObjectSet(this, value); // throws JsonValueException
                String name = objectSet.getName();
                //TODO Fix this check
//                if (routes.containsKey(name)) {
//                    throw new JsonValueException(value, "object " + name + " already defined");
//                }
                managedRouter.addRoute(EQUALS, name, objectSet);
            }
        } catch (Exception jve) {
            throw new ComponentException("Configuration error", jve);
        }
    }

    /**
     * TODO: Description.
     *
     * @param context TODO.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.context = null;
        managedRouter.removeAllRoutes();
    }


    /**
     * TODO: Description.
     * @return
     */
    CryptoService getCryptoService() {
        return cryptoService;
    }

    ScriptRegistry getScriptRegistry() {
        return scriptRegistry;
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        managedRouter.handleAction(context,request,handler);
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        managedRouter.handleCreate(context,request,handler);
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        managedRouter.handleDelete(context,request,handler);
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        managedRouter.handlePatch(context,request,handler);
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        managedRouter.handleQuery(context,request,handler);
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        managedRouter.handleRead(context,request,handler);
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        managedRouter.handleUpdate(context,request,handler);
    }
}
