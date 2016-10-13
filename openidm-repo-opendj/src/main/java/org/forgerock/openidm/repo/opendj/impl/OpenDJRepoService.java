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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.repo.opendj.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.forgerock.opendj.grizzly.GrizzlyTransportProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
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
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Response;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.rest2ldap.AuthenticatedConnectionContext;
import org.forgerock.opendj.rest2ldap.Resource;
import org.forgerock.opendj.rest2ldap.Rest2Ldap;
import org.forgerock.opendj.rest2ldap.Rest2LdapJsonConfigurator;
import org.forgerock.opendj.server.embedded.EmbeddedDirectoryServer;
import org.forgerock.opendj.server.embedded.EmbeddedDirectoryServerException;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.router.RouteBuilder;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.openidm.router.RouterRegistry;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.Function;
import org.forgerock.util.Options;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.omg.CORBA.INTERNAL;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;

/**
 * Repository service implementation using OpenDJ
 * 
 * Currently only servicing requests on managed/user
 */
@Component(name = OpenDJRepoService.PID, immediate=true, policy=ConfigurationPolicy.REQUIRE, enabled=true)
@Service (value = {/*RepositoryService.class, */ RequestHandler.class})
@Properties({
    @Property(name = "service.description", value = "Repository Service using OpenDJ"),
    @Property(name = "service.vendor", value = ServerConstants.SERVER_VENDOR_NAME) })
public class OpenDJRepoService implements RepositoryService, RequestHandler {

    final static Logger logger = LoggerFactory.getLogger(OpenDJRepoService.class);
    
    public static final String PID = "org.forgerock.openidm.repo.opendj";

    static OpenDJRepoService bootSvc = null;

    /**
     * The current OpenDJ configuration
     */
    private JsonValue existingConfig;

    /**
     * Used for parsing the configuration
     */
    private EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private EmbeddedDirectoryServer embeddedDirectoryServer;

    /**
     * Router registry used to register additional routes while we can't support /repo/*.
     *
     * TODO - remove this once we support /repo/*
     */
    @Reference(policy = ReferencePolicy.STATIC)
    protected RouterRegistry routerRegistry;

    private ConnectionFactory ldapFactory;

    private TypeHandler getTypeHandler(final String uri) {
        final TypeHandler handler = typeHandlers.get(uri);

        if (handler == null) {
            if (uri.contains("/")) {
                return getTypeHandler(uri.substring(0, uri.lastIndexOf("/")));
            } else {
                throw new NullPointerException("Type handler not found");
            }
        } else {
            return handler;
        }
    }

    /** Extract type from request and return the assocaited handler */
    private TypeHandler getTypeHandler(final Context ctx) {
        // chop /repo from uri
        final String uri = ctx.asContext(UriRouterContext.class).getMatchedUri().substring(5);

        return getTypeHandler(uri);
    }

    /**
     * Map of pre-configured queryFilters in the form of key: queryId value: tokenized queryFilter.
     *
     * TODO - these should probably be object specific
     */
    private Map<String, JsonValue> configQueries = new HashMap<>();

    /**
     * Map of handlers for each configured type
     */
    private Map<String, TypeHandler> typeHandlers;

    @Activate
    void activate(final ComponentContext compContext) throws Exception {
        logger.info("Activating Service with configuration {}", compContext.getProperties());

        try {
            existingConfig = enhancedConfig.getConfigurationAsJson(compContext);
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid and could not be parsed, can not start OrientDB repository: " 
                    + ex.getMessage(), ex);
            throw ex;
        }

        //  Initialize the repo service
        init(existingConfig);

        logger.info("Repository started.");
    }

    @Deactivate
    void deactivate(final ComponentContext ctx) {
        final Set<Map.Entry<String, TypeHandler>> entries = typeHandlers.entrySet();

        for (Map.Entry<String, TypeHandler> entry : entries) {
            if (entry.getValue() != null && entry.getValue().getRouteEntry() != null) {
                entry.getValue().getRouteEntry().removeRoute();
            }
        }

        if (ldapFactory != null) {
            ldapFactory.close();
            ldapFactory = null;
        }
    }

    void init(final JsonValue config) {
        final TrustManager trustManager = Rest2LdapJsonConfigurator.configureTrustManager(config.get("security"));
        final X509KeyManager keyManager = Rest2LdapJsonConfigurator.configureKeyManager(config.get("security"));

        ldapFactory = Rest2LdapJsonConfigurator.configureConnectionFactory(
                config.get("ldapConnectionFactories").required(), "root",
                trustManager, keyManager, GrizzlyTransportProvider.class.getClassLoader());

        /*
         * Queries
         */

        final JsonValue queries = config.get("queries").required();

        for (String queryId : queries.keys()) {
            final JsonValue queryConfig = queries.get(queryId).required();

            // TODO - improve initial config check.
            //        We should be checking if fields is a csv.
            //        Possibly if _queryFilter is valid syntax?
            queryConfig.get("_queryFilter").required();

            configQueries.put(queryId, queryConfig);
        }

        /*
         * Mappings
         */

        // IDM-specific handler config (outside scope of rest2ldap)
        final JsonValue resourceMappings = config.get("resourceMapping").required().expect(Map.class);

        final Options rest2LdapOptions = Rest2LdapJsonConfigurator.configureOptions(
                config.get("rest2LdapOptions").required().expect(Map.class));

        final List<Resource> resources = Rest2LdapJsonConfigurator.configureResources(
                config.get("resources").required().expect(Map.class));

        final Map<String, TypeHandler> typeHandlers = new HashMap<>();

        final Rest2Ldap rest2Ldap = Rest2Ldap.rest2Ldap(rest2LdapOptions, resources);
        final RequestHandler repoHandler = rest2Ldap.newRequestHandlerFor("repo");

        /*
         * FIXME - remove duplicate code for generic/explicit mappings
         */

        // Generic mappings
        final JsonValue genericMappings = resourceMappings.get("genericMapping").expect(Map.class);
        if (genericMappings.isNotNull()) {
            for (String type : genericMappings.keys()) {
                final JsonValue handlerConfig = genericMappings.get(type);

                // The path to this resource on the rest2ldap router
                final ResourcePath path = new ResourcePath(type.split("/"));
                final TypeHandler typeHandler = new GenericDJTypeHandler(path, repoHandler, null, handlerConfig,
                        queries, config.get("commands"));

            /*
             * Since we cannot simply listen on /repo/* while we do not have all objects mapped/implemented
             * We must register individual routes for each handler.
             */

                final RouteEntry routeEntry = routerRegistry.addRoute(RouteBuilder.newBuilder()
                        .withModeStartsWith()
                        .withTemplate("/repo/" + type)
                        .withRequestHandler(this)
                        .seal());

                typeHandler.setRouteEntry(routeEntry);

                // TODO - breakup queries
                typeHandlers.put(type, typeHandler);
            }
        }

        // Explicit mappings
        final JsonValue explicitMappings = resourceMappings.get("explicitMapping").expect(Map.class);
        if (explicitMappings.isNotNull()) {
            for (String type : explicitMappings.keys()) {
                final JsonValue handlerConfig = explicitMappings.get(type);
                // The path to this resource on the rest2ldap router
                final ResourcePath path = new ResourcePath(type.split("/"));
                final TypeHandler typeHandler = new ExplicitDJTypeHandler(path, repoHandler, null, handlerConfig,
                        queries, config.get("commands"));

            /*
             * Since we cannot simply listen on /repo/* while we do not have all objects mapped/implemented
             * We must register individual routes for each handler.
             */

                final RouteEntry routeEntry = routerRegistry.addRoute(RouteBuilder.newBuilder()
                        .withModeStartsWith()
                        .withTemplate("/repo/" + type)
                        .withRequestHandler(this)
                        .seal());

                typeHandler.setRouteEntry(routeEntry);

                // TODO - breakup queries
                typeHandlers.put(type, typeHandler);
            }
        }

        this.typeHandlers = typeHandlers;
    }

    private Connection getLdapConnection() throws InternalServerErrorException {
        try {
            if (embeddedDirectoryServer != null) {
                return embeddedDirectoryServer.getInternalConnection();
            } else {
                return ldapFactory.getConnection();
            }
        } catch (EmbeddedDirectoryServerException|LdapException e) {
            logger.error("Failed to acquire connection", e);
            throw new InternalServerErrorException("Failed to acquire connection", e);
        }
    }

    /**
     * Function wrapper that passes in a context containing a {@link AuthenticatedConnectionContext}
     * and closes the connection after the function promise is complete.
     *
     * @param context The base context to chain the {@link AuthenticatedConnectionContext} to
     * @param function The function to use the provided context
     *
     * @return The return value of function
     */
    private <R extends Response> Promise<R, ResourceException> withConnectionContext(
            final Context context,
            final Function<Context, Promise<R, ResourceException>, NeverThrowsException> function) {
        try {
            // must check context before acquiring a connection
            Reject.checkNotNull(context);
            final Connection connection = getLdapConnection();
            final AuthenticatedConnectionContext authCtx =
                    new AuthenticatedConnectionContext(context, connection);

            return function.apply(authCtx).then(new Function<R, R, ResourceException>() {
                @Override
                public R apply(final R response) throws ResourceException {
                    connection.close();
                    return response;
                }
            });
        } catch (InternalServerErrorException e) {
            return e.asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context, final CreateRequest request) {
        return withConnectionContext(context, new Function<Context, Promise<ResourceResponse, ResourceException>, NeverThrowsException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(final Context context) throws NeverThrowsException {
                return getTypeHandler(context).handleCreate(context, request);
            }
        });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(final Context context, final ReadRequest request) {
        return withConnectionContext(context, new Function<Context, Promise<ResourceResponse, ResourceException>, NeverThrowsException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(final Context context) throws NeverThrowsException {
                return getTypeHandler(context).handleRead(context, request);
            }
        });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, final UpdateRequest request) {
        return withConnectionContext(context, new Function<Context, Promise<ResourceResponse, ResourceException>, NeverThrowsException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(final Context context) throws NeverThrowsException {
                return getTypeHandler(context).handleUpdate(context, request);
            }
        });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context, final PatchRequest request) {
        return withConnectionContext(context, new Function<Context, Promise<ResourceResponse, ResourceException>, NeverThrowsException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(final Context context) throws NeverThrowsException {
                return getTypeHandler(context).handlePatch(context, request);
            }
        });
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context, final ActionRequest request) {
        return withConnectionContext(context, new Function<Context, Promise<ActionResponse, ResourceException>, NeverThrowsException>() {
            @Override
            public Promise<ActionResponse, ResourceException> apply(final Context context) throws NeverThrowsException {
                return getTypeHandler(context).handleAction(context, request);
            }
        });
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context, final DeleteRequest request) {
        return withConnectionContext(context, new Function<Context, Promise<ResourceResponse, ResourceException>, NeverThrowsException>() {
            @Override
            public Promise<ResourceResponse, ResourceException> apply(final Context context) throws NeverThrowsException {
                return getTypeHandler(context).handleDelete(context, request);
            }
        });
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest queryRequest, final QueryResourceHandler queryResourceHandler) {
        return withConnectionContext(context, new Function<Context, Promise<QueryResponse, ResourceException>, NeverThrowsException>() {
            @Override
            public Promise<QueryResponse, ResourceException> apply(final Context context) throws NeverThrowsException {
                return getTypeHandler(context).handleQuery(context, queryRequest, queryResourceHandler);
            }
        });
    }

    /*
     * RepositoryService methods just forward to the RequestHandler and block until compltete.
     * Must pass in a new RootContext to chain the AuthenticatedConnectionContex
     */

    @Override
    public ResourceResponse create(final CreateRequest request) throws ResourceException {
        return handleCreate(new RootContext(), request).getOrThrowUninterruptibly();
    }

    @Override
    public ResourceResponse read(final ReadRequest request) throws ResourceException {
        return handleRead(new RootContext(), request).getOrThrowUninterruptibly();
    }

    @Override
    public ResourceResponse update(final UpdateRequest request) throws ResourceException {
        return handleUpdate(new RootContext(), request).getOrThrowUninterruptibly();
    }

    @Override
    public ResourceResponse delete(final DeleteRequest request) throws ResourceException {
        return handleDelete(new RootContext(), request).getOrThrowUninterruptibly();
    }

    @Override
    public List<ResourceResponse> query(final QueryRequest request) throws ResourceException {
        final List<ResourceResponse> results = new ArrayList<>();
        final QueryResourceHandler handler = new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resourceResponse) {
                results.add(resourceResponse);
                return true;
            }
        };

        handleQuery(new RootContext(), request, handler).getOrThrowUninterruptibly();

        return results;
    }
}
