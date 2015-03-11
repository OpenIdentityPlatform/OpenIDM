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

package org.forgerock.openidm.script;

import java.util.Dictionary;
import java.util.EnumSet;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.script.Scope;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptEvent;
import org.forgerock.script.ScriptListener;
import org.forgerock.script.ScriptName;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.script.source.SourceUnit;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An AbstractScriptedService does ...
 * 
 */
@Component(componentAbstract = true)
public abstract class AbstractScriptedService implements ScriptCustomizer, ScriptListener {

    /**
     * Setup logging for the {@link AbstractScriptedService}.
     */
    private static final Logger logger = LoggerFactory.getLogger(AbstractScriptedService.class);

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scriptRegistry;

    protected void bindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = service;
    }

    protected void unbindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = null;
    }

    /** PersistenceConfig service. */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private PersistenceConfig persistenceConfig;

    protected void bindPersistenceConfig(final PersistenceConfig service) {
        persistenceConfig = service;
    }

    protected void unbindPersistenceConfig(final PersistenceConfig service) {
        persistenceConfig = null;
    }

    private ScriptedRequestHandler embeddedHandler = null;

    private ServiceRegistration<RequestHandler> selfRegistration = null;

    private Dictionary<String, Object> properties = null;

    protected Bindings bindings = null;

    protected ScriptName scriptName = null;

    /**
     * Operation mask for supported operation.
     */
    protected final EnumSet<RequestType> mask;

    protected AbstractScriptedService() {
        mask = EnumSet.allOf(RequestType.class);
    }

    protected AbstractScriptedService(EnumSet<RequestType> mask) {
        this.mask = mask;
    }

    /**
     * Get the {@link ServerConstants#ROUTER_PREFIX} value.
     * <p/>
     * If it return null then the {@link ServerConstants#ROUTER_PREFIX} won't be
     * changed in the service registration properties.
     * 
     * @param factoryPid
     * @param configuration
     * @return null or String or String[]
     */
    protected abstract Object getRouterPrefixes(String factoryPid, JsonValue configuration);

    protected abstract BundleContext getBundleContext();

    protected ScriptCustomizer getScriptCustomizer() {
        return this;
    }

    protected Dictionary<String, Object> getProperties() {
        return properties;
    }

    protected void setProperties(Dictionary<String, Object> properties) {
        this.properties = properties;
    }

    protected Scope activate(final BundleContext context, final String factoryPid,
            final JsonValue configuration) {

        Dictionary<String, Object> prop = getProperties();
        if (null != prop) {
            Object o = getRouterPrefixes(factoryPid, configuration);
            if (null != o) {
                prop.put(ServerConstants.ROUTER_PREFIX, o);
            }
        }

        try {
            ScriptEntry scriptEntry = scriptRegistry.takeScript(configuration);
            if (null == scriptEntry) {
                logger.error("Failed to get the script {}:{}", configuration
                        .get(SourceUnit.ATTR_NAME), configuration.get(SourceUnit.ATTR_TYPE));
                throw new NullPointerException();
            }
            scriptEntry.addScriptListener(this);
            scriptName = scriptEntry.getName();

            embeddedHandler = new ScriptedRequestHandler(scriptEntry, getScriptCustomizer());

            selfRegistration = context.registerService(RequestHandler.class, embeddedHandler, prop);

            return embeddedHandler;
        } catch (ScriptException e) {
            throw new ComponentException("Failed to take script: " + factoryPid, e);
        }
    }

    protected Scope modified(final String factoryPid, final JsonValue configuration) {
        try {
            ScriptEntry scriptEntry = scriptRegistry.takeScript(configuration);
            if (null == scriptEntry) {
                logger.error("Failed to get the script {}:{}", configuration
                        .get(SourceUnit.ATTR_NAME), configuration.get(SourceUnit.ATTR_TYPE));
                throw new NullPointerException();
            }
            if (null != scriptName) {
                scriptRegistry.deleteScriptListener(scriptName, this);
            }
            scriptEntry.addScriptListener(this);
            scriptName = scriptEntry.getName();

            embeddedHandler.setScriptEntry(scriptEntry);
            return embeddedHandler;
        } catch (ScriptException e) {
            logger.error("Failed to modify the ScriptedService", e);
            throw new ComponentException("Failed to take script: " + factoryPid, e);
        }
    }

    protected void deactivate() {
        try {
            if (null != selfRegistration) {
                selfRegistration.unregister();
                selfRegistration = null;
            }
        } catch (IllegalStateException e) {
            /* Catch if the service was already removed */
            selfRegistration = null;
        } finally {
            if (null != scriptName) {
                scriptRegistry.deleteScriptListener(scriptName, this);
            }
        }
        logger.info("OpenIDM Info Service component is deactivated.");
    }

    // ----- Implementation of ScriptListener interface

    public void scriptChanged(ScriptEvent event) {
        if (ScriptEvent.REGISTERED == event.getType()) {
            if (null == selfRegistration) {
                synchronized (selfRegistration) {
                    if (null == selfRegistration) {
                        final ScriptEntry scriptEntry = event.getScriptLibraryEntry();
                        scriptEntry.setBindings(bindings);
                        selfRegistration =
                                getBundleContext().registerService(
                                        RequestHandler.class,
                                        new ScriptedRequestHandler(scriptEntry,
                                                getScriptCustomizer()), getProperties());
                    }
                }
            }
        } else if (ScriptEvent.UNREGISTERING == event.getType()) {
            if (null != selfRegistration) {
                synchronized (selfRegistration) {
                    if (null != selfRegistration) {
                        selfRegistration.unregister();
                    }
                }
            }
        } else if (ScriptEvent.MODIFIED == event.getType()) {

            /*
             * if (null != selfRegistration) { selfRegistration.unregister(); }
             * if (null == selfRegistration) { selfRegistration =
             * getBundleContext().registerService(RequestHandler.class, new
             * ScriptedRequestHandler( event.getScriptLibraryEntry(), this),
             * properties); }
             */

        }
    }

    // ----- Implementation of ScriptCustomizer interface

    public void handleAction(final ServerContext context, final ActionRequest request,
            final Bindings bindings) throws ResourceException {
        if (!mask.contains(RequestType.ACTION)) {
            throw new NotSupportedException("Actions are not supported for resource instances");
        }
        handleRequest(context, request, bindings);
    }

    public void handleCreate(final ServerContext context, final CreateRequest request,
            final Bindings bindings) throws ResourceException {
        if (!mask.contains(RequestType.CREATE)) {
            throw new NotSupportedException("Create operations are not supported");
        }
        handleRequest(context, request, bindings);
    }

    public void handleDelete(final ServerContext context, final DeleteRequest request,
            final Bindings bindings) throws ResourceException {
        if (!mask.contains(RequestType.DELETE)) {
            throw new NotSupportedException("Delete operations are not supported");
        }
        handleRequest(context, request, bindings);
    }

    public void handlePatch(final ServerContext context, final PatchRequest request,
            final Bindings bindings) throws ResourceException {
        if (!mask.contains(RequestType.PATCH)) {
            throw new NotSupportedException("Patch operations are not supported");
        }
        handleRequest(context, request, bindings);
    }

    public void handleQuery(final ServerContext context, final QueryRequest request,
            final Bindings bindings) throws ResourceException {
        if (!mask.contains(RequestType.QUERY)) {
            throw new NotSupportedException("Query operations are not supported");
        }
        handleRequest(context, request, bindings);
    }

    public void handleRead(final ServerContext context, final ReadRequest request,
            final Bindings bindings) throws ResourceException {

        if (!mask.contains(RequestType.READ)) {
            throw new NotSupportedException("Read operations are not supported");
        }
        handleRequest(context, request, bindings);
    }

    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final Bindings bindings) throws ResourceException {
        if (!mask.contains(RequestType.UPDATE)) {
            throw new NotSupportedException("Update operations are not supported");
        }
        handleRequest(context, request, bindings);
    }

    protected void handleRequest(final ServerContext context, final Request request,
            final Bindings bindings) {
        bindings.put("request", request);
        bindings.put("resourceName", request.getResourceNameObject());
        bindings.put("context", context);
    }
}
