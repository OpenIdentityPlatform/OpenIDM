package org.forgerock.openidm.audit.impl;

import org.forgerock.audit.DependencyProvider;
import org.forgerock.audit.events.handlers.AuditEventHandlerBase;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;

/**
 * Audit event handler for Repository.  This is a decorator to the RouterAuditEventHandler where the resourcePath is
 * hardcoded to be "repo/audit".
 *
 * @see RouterAuditEventHandler
 */
public class RepositoryAuditEventHandler extends AuditEventHandlerBase<RepositoryAuditEventHandlerConfiguration> {
    public static final String REPO_AUDIT_PATH = "repo/audit";
    private RouterAuditEventHandler routerAuditEventHandler;

    /**
     * Constructs the decorated RouterAuditEventHandler.
     */
    public RepositoryAuditEventHandler() {
        this.routerAuditEventHandler = new RouterAuditEventHandler();
    }

    /**
     * Configures the decorated RouterAuditEventHandler with a fixed path of "repo/audit"
     *
     * @param config empty RepositoryAuditEventHandlerConfiguration
     * @throws ResourceException
     */
    @Override
    public void configure(RepositoryAuditEventHandlerConfiguration config) throws ResourceException {
        RouterAuditEventHandlerConfiguration routerConfig = new RouterAuditEventHandlerConfiguration();
        routerConfig.setResourcePath(REPO_AUDIT_PATH);
        routerAuditEventHandler.configure(routerConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDependencyProvider(DependencyProvider provider) {
        routerAuditEventHandler.setDependencyProvider(provider);
    }

    /**
     * {@inheritDoc}
     *
     * @throws ResourceException
     */
    @Override
    public void close() throws ResourceException {
        routerAuditEventHandler.close();
    }

    /**
     * {@inheritDoc}
     *
     * @param serverContext
     * @param actionRequest
     * @param resultHandler
     */
    @Override
    public void actionCollection(ServerContext serverContext, ActionRequest actionRequest,
            ResultHandler<JsonValue> resultHandler) {
        routerAuditEventHandler.actionCollection(serverContext, actionRequest, resultHandler);
    }

    /**
     * {@inheritDoc}
     *
     * @param serverContext
     * @param resourceId
     * @param actionRequest
     * @param resultHandler
     */
    @Override
    public void actionInstance(ServerContext serverContext, String resourceId, ActionRequest actionRequest,
            ResultHandler<JsonValue> resultHandler) {
        routerAuditEventHandler.actionInstance(serverContext, resourceId, actionRequest, resultHandler);
    }

    /**
     * {@inheritDoc}
     *
     * @param serverContext
     * @param createRequest
     * @param resultHandler
     */
    @Override
    public void createInstance(ServerContext serverContext, CreateRequest createRequest,
            ResultHandler<Resource> resultHandler) {
        routerAuditEventHandler.createInstance(serverContext, createRequest, resultHandler);
    }

    /**
     * {@inheritDoc}
     *
     * @param serverContext
     * @param queryRequest
     * @param queryResultHandler
     */
    @Override
    public void queryCollection(ServerContext serverContext, QueryRequest queryRequest,
            QueryResultHandler queryResultHandler) {
        routerAuditEventHandler.queryCollection(serverContext, queryRequest, queryResultHandler);
    }

    /**
     * {@inheritDoc}
     *
     * @param serverContext
     * @param resourceId
     * @param readRequest
     * @param resultHandler
     */
    @Override
    public void readInstance(ServerContext serverContext, String resourceId, ReadRequest readRequest,
            ResultHandler<Resource> resultHandler) {
        routerAuditEventHandler.readInstance(serverContext, resourceId, readRequest, resultHandler);
    }
}
