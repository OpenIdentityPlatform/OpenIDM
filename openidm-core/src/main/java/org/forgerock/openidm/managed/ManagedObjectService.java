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
 * Portions copyright 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.managed;

import static org.forgerock.json.resource.Resources.newHandler;
import static org.forgerock.json.resource.Router.uriTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.services.routing.RouteMatcher;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
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
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.util.promise.Promise;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides access to managed objects.
 *
 */
@Component(
        name = ManagedObjectService.PID,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                ServerConstants.ROUTER_PREFIX + "=/managed*"
        })
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM managed objects service")
public class ManagedObjectService implements RequestHandler, Describable<ApiDescription, Request> {

    public static final String PID = "org.forgerock.openidm.managed";

    /**
     * Setup logging for the {@link ManagedObjectService}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ManagedObjectService.class);

    /** Cryptographic service. */
    @Reference
    protected CryptoService cryptoService;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected volatile ScriptRegistry scriptRegistry;

    /**
     * Route service on "sync" endpoint.  An aspect of CRUDPAQ on managed objects is to synchronize their
     * attributes with remote system if configured.  As this message is sent over the router, we need to
     * know if the SynchronizationService is available.  This optional reference is used to indicate that
     * availability.
     */

    private final AtomicReference<RouteService> syncRoute = new AtomicReference<RouteService>();

    @Reference(service = RouteService.class,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindSyncRoute",
            cardinality = ReferenceCardinality.OPTIONAL,
            target = "(" + ServerConstants.ROUTER_PREFIX + "=/sync*)")
	void bindSyncRoute(final RouteService service) {
        syncRoute.set(service);
    }

    @SuppressWarnings("unused")
	void unbindSyncRoute(final RouteService service) {
        syncRoute.set(null);
    }

    /* The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    private final ConcurrentMap<String, RouteMatcher<Request>> managedRoutes = new ConcurrentHashMap<String, RouteMatcher<Request>>();

    private final Router managedRouter = new Router();

    /**
     * RequestHandler to handle requests for both a {@link ManagedObjectSet} and its nested
     * {@link RelationshipProvider}s.
     *
     * Requests to {@code /} and {@code /{id}} will be directed to the object set.
     * Requests starting with {@code /{id}/{relationshipField}} will be directed to their relationship provider.
     */
    private class ManagedObjectSetRequestHandler implements RequestHandler, Describable<ApiDescription, Request> {
        final ManagedObjectSet objectSet;
        final RequestHandler objectSetRequestHandler;
        final ApiDescription apiDescription;

        ManagedObjectSetRequestHandler(final ManagedObjectSet objectSet) {
            this.objectSet = objectSet;
            this.objectSetRequestHandler = newHandler(objectSet);
            apiDescription = ManagedObjectApiDescription.build(objectSet);
        }

        private RequestHandler requestHandler(final Request request) {
            final ResourcePath path = request.getResourcePathObject();

            if (path.size() <= 1) { // Either / or /{id}. Use the objectSet
                return objectSetRequestHandler;
            } else { // /{id}/{relationshipField}/... use the relationship provider
                final RelationshipProvider provider = objectSet.getRelationshipProviders().get(new JsonPointer(path.get(1)));

                if (provider != null) {
                    return provider.asRequestHandler();
                } else {
                    // If we don't have a relationship provider for this endpoint send it back ot the objectset
                    // to get a proper 404 response.
                    return objectSetRequestHandler;
                }
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

        @Override
        public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
            return apiDescription;
        }

        @Override
        public ApiDescription handleApiRequest(Context context, Request request) {
            return apiDescription;
        }

        @Override
        public void addDescriptorListener(Listener listener) {
            // empty
        }

        @Override
        public void removeDescriptorListener(Listener listener) {
            // empty
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
        for (JsonValue managedObjectConfig : configuration.get("objects").expect(List.class)) {
            final ManagedObjectSet objectSet = new ManagedObjectSet(scriptRegistry, cryptoService, syncRoute, connectionFactory, managedObjectConfig);
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

    @Override
    public ApiDescription api(final ApiProducer<ApiDescription> apiProducer) {
        return managedRouter.api(apiProducer);
    }

    @Override
    public ApiDescription handleApiRequest(final Context context, final Request request) {
        return managedRouter.handleApiRequest(context, request);
    }

    @Override
    public void addDescriptorListener(final Listener listener) {
        managedRouter.addDescriptorListener(listener);
    }

    @Override
    public void removeDescriptorListener(final Listener listener) {
        managedRouter.removeDescriptorListener(listener);
    }
}
