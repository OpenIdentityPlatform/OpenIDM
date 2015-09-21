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
 * Portions copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.managed;

import static org.forgerock.json.resource.Resources.newCollection;
import static org.forgerock.json.resource.Router.uriTemplate;

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
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.services.context.Context;
import org.forgerock.services.routing.RouteMatcher;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.util.promise.Promise;
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

    /**
     * Route service on "sync" endpoint.  An aspect of CRUDPAQ on managed objects is to synchronize their
     * attributes with remote system if configured.  As this message is sent over the router, we need to
     * know if the SynchronizationService is available.  This optional reference is used to indicate that
     * availability.
     */
    @Reference(referenceInterface = RouteService.class,
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindSyncRoute",
            unbind = "unbindSyncRoute",
            cardinality = ReferenceCardinality.OPTIONAL_UNARY,
            target = "(" + ServerConstants.ROUTER_PREFIX + "=/sync*)")
    private final AtomicReference<RouteService> syncRoute = new AtomicReference<RouteService>();

    @SuppressWarnings("unused")
	private void bindSyncRoute(final RouteService service) {
        syncRoute.set(service);
    }

    @SuppressWarnings("unused")
	private void unbindSyncRoute(final RouteService service) {
        syncRoute.set(null);
    }

    /* The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    private final ConcurrentMap<String, RouteMatcher<Request>> managedRoutes = new ConcurrentHashMap<String, RouteMatcher<Request>>();

    private final Router managedRouter = new Router();

    /**
     * RequestHandler to handle requests for both a {@link ManagedObjectSet} and its nested
     * {@link RelationshipProvider}s.
     *
     * Requests to {@code /} and {@code /{id}} will be directed to the object set.
     * Requests starting with {@code /{id}/{relationshipField}} will be directed to their relationship provider.
     */
    private class ManagedObjectSetRequestHandler implements RequestHandler {
        final ManagedObjectSet objectSet;
        final RequestHandler objectSetRequestHandler;

        ManagedObjectSetRequestHandler(final ManagedObjectSet objectSet) {
            this.objectSet = objectSet;
            this.objectSetRequestHandler = newCollection(objectSet);
        }

        private RequestHandler requestHandler(final Request request) {
            final ResourcePath path = request.getResourcePathObject();

            if (path.size() <= 1) { // Either / or /{id}. Use the objectSet
                return objectSetRequestHandler;
            } else { // /{id}/{relationshipField}/... use the relationship provider
                return objectSet.getRelationshipProviders().get(new JsonPointer(path.get(1))).asRequestHandler();
            }
        }

        @Override
        public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
            return requestHandler(request).handleAction(context, request);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
            return requestHandler(request).handleCreate(context, request);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
            return requestHandler(request).handleDelete(context, request);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
            return requestHandler(request).handlePatch(context, request);
        }

        @Override
        public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request, QueryResourceHandler handler) {
            return requestHandler(request).handleQuery(context, request, handler);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
            return requestHandler(request).handleRead(context, request);
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
            return requestHandler(request).handleUpdate(context, request);
        }
    }


    /**
     * Activates the component
     * 
     * @param context the {@link ComponentContext} object for this component.
     */
    @Activate
    protected void activate(ComponentContext context) throws Exception {
        JsonValue configuration = enhancedConfig.getConfigurationAsJson(context);
        for (JsonValue value : configuration.get("objects").expect(List.class)) {
            final ManagedObjectSet objectSet = new ManagedObjectSet(scriptRegistry, cryptoService, syncRoute, connectionFactory, value);
            if (managedRoutes.containsKey(objectSet.getName())) {
                throw new ComponentException("Duplicate definition of managed object type: " + objectSet.getName());
            }

            managedRoutes.put(objectSet.getName(),
                    managedRouter.addRoute(RoutingMode.STARTS_WITH, uriTemplate(objectSet.getTemplate()),
                            new ManagedObjectSetRequestHandler(objectSet)));
        }
    }

    /**
     * Modifies the component
     * 
     * @param context the {@link ComponentContext} object for this component.
     */
    @Modified
    protected void modified(ComponentContext context) throws Exception {
        JsonValue configuration = enhancedConfig.getConfigurationAsJson(context);

        Set<String> routesToKeep = new HashSet<String>();
        for (JsonValue value : configuration.get("objects").expect(List.class)) {
            ManagedObjectSet objectSet = new ManagedObjectSet(scriptRegistry, cryptoService, syncRoute, connectionFactory, value);
            if (routesToKeep.contains(objectSet.getName())) {
                throw new ComponentException("Duplicate definition of managed object type: " + objectSet.getName());
            }
            RouteMatcher<Request> oldRoute = managedRoutes.get(objectSet.getName());
            if (null != oldRoute) {
                managedRouter.removeRoute(oldRoute);
            }
            managedRoutes.put(objectSet.getName(),
                    managedRouter.addRoute(RoutingMode.STARTS_WITH, uriTemplate(objectSet.getTemplate()),
                            new ManagedObjectSetRequestHandler(objectSet)));
            routesToKeep.add(objectSet.getName());
        }
        for (Map.Entry<String, RouteMatcher<Request>> entry : managedRoutes.entrySet()){
            //Use ConcurrentMap to avoid ConcurrentModificationException with this iteration
            if (routesToKeep.contains(entry.getKey())) {
                continue;
            }
            managedRouter.removeRoute(managedRoutes.remove(entry.getKey()));
        }
    }


    /**
     * Deactivates the component
     * 
     * @param context the {@link ComponentContext} object for this component.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        managedRouter.removeAllRoutes();
        managedRoutes.clear();
    }
    
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context, final ActionRequest request) {
        return managedRouter.handleAction(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
            final CreateRequest request) {
        return managedRouter.handleCreate(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
            final DeleteRequest request) {
        return managedRouter.handleDelete(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context, final PatchRequest request) {
        return managedRouter.handlePatch(context, request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request,
            QueryResourceHandler queryResourceHandler) {
        return managedRouter.handleQuery(context, request, queryResourceHandler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(final Context context, final ReadRequest request) {
        return managedRouter.handleRead(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
            final UpdateRequest request) {
        return managedRouter.handleUpdate(context, request);
    }
}
