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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptEvent;
import org.forgerock.script.ScriptListener;
import org.forgerock.script.ScriptName;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.util.Factory;
import org.forgerock.util.LazyMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
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

    private ServiceRegistration<RequestHandler> selfRegistration = null;

    private Dictionary<String, Object> properties = null;

    protected Bindings bindings = null;

    protected ScriptName scriptName = null;

    /**
     * Operation mask for supported operation.
     */
    protected final int mask;

    protected AbstractScriptedService() {
        mask = CREATE | READ | UPDATE | PATCH | QUERY | DELETE | ACTION;
    }

    protected AbstractScriptedService(int mask) {
        this.mask = mask;
    }

    protected abstract Object getRouterPrefixes(String factoryPid, JsonValue configuration);

    protected abstract JsonValue serialiseServerContext(final ServerContext context)
            throws ResourceException;

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

    protected void activate(final BundleContext context, final String factoryPid,
            final JsonValue configuration) {

        Dictionary<String, Object> prop = getProperties();
        if (null != prop) {
            prop.put(ServerConstants.ROUTER_PREFIX, getRouterPrefixes(factoryPid, configuration));
        }

        try {
            ScriptEntry scriptEntry = scriptRegistry.takeScript(configuration);
            scriptEntry.addScriptListener(this);
            scriptName = scriptEntry.getName();

            selfRegistration =
                    context.registerService(RequestHandler.class, new ScriptedRequestHandler(
                            scriptEntry, getScriptCustomizer()), prop);

        } catch (ScriptException e) {
            throw new ComponentException("Failed to take script: " + factoryPid, e);
        }
    }

    protected void deactivate() {
        if (null != selfRegistration) {
            selfRegistration.unregister();
        }
        if (null != scriptName) {
            scriptRegistry.deleteScriptListener(scriptName, this);
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
            final Bindings handler) throws ResourceException {
        if ((ACTION & mask) == 0) {
            throw new NotSupportedException("Actions are not supported for resource instances");
        }
        handleRequest(context, request, handler);
    }

    public void handleCreate(final ServerContext context, final CreateRequest request,
            final Bindings handler) throws ResourceException {
        if ((CREATE & mask) == 0) {
            throw new NotSupportedException("Create operations are not supported");
        }
        handleRequest(context, request, handler);
    }

    public void handleDelete(final ServerContext context, final DeleteRequest request,
            final Bindings handler) throws ResourceException {
        if ((DELETE & mask) == 0) {
            throw new NotSupportedException("Delete operations are not supported");
        }
        handleRequest(context, request, handler);
    }

    public void handlePatch(final ServerContext context, final PatchRequest request,
            final Bindings handler) throws ResourceException {
        if ((PATCH & mask) == 0) {
            throw new NotSupportedException("Patch operations are not supported");
        }
        handleRequest(context, request, handler);
    }

    public void handleQuery(final ServerContext context, final QueryRequest request,
            final Bindings handler) throws ResourceException {
        if ((QUERY & mask) == 0) {
            throw new NotSupportedException("Query operations are not supported");
        }
        handleRequest(context, request, handler);
    }

    public void handleRead(final ServerContext context, final ReadRequest request,
            final Bindings handler) throws ResourceException {

        if ((READ & mask) == 0) {
            throw new NotSupportedException("Read operations are not supported");
        }
        handleRequest(context, request, handler);
    }

    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final Bindings handler) throws ResourceException {
        if ((UPDATE & mask) == 0) {
            throw new NotSupportedException("Update operations are not supported");
        }
        handleRequest(context, request, handler);
    }

    protected void handleRequest(final ServerContext context, final Request request,
            final Bindings handler) {
        handler.put("request", request);
        handler.put("context", request);

        handler.put("_context", new LazyMap<String, Object>(new Factory<Map<String, Object>>() {
            @Override
            public Map<String, Object> newInstance() {
                final JsonValue serverContext;
                try {
                    serverContext = serialiseServerContext(context);
                    if (null != serverContext) {
                        return serverContext.required().asMap();
                    }
                } catch (ResourceException e) {
                    logger.error("Failed to serialise the ServerContext", e);
                    /* ignore */
                }
                return Collections.emptyMap();
            }
        }));
    }
}
