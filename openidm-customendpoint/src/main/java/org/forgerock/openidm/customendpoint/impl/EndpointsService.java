/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.customendpoint.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.script.ScriptException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom endpoints service to provide a scriptable way to extend and
 * customize the system
 * 
 * @author Laszlo Hordos
 * @author aegloff
 */
@Component(name = EndpointsService.PID, policy = ConfigurationPolicy.OPTIONAL,
        description = "OpenIDM Custom Endpoints Service", immediate = true)
@Service()
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Custom Endpoints Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = { "endpoint/{pid}" }) })
public class EndpointsService implements RequestHandler {

    public static final String PID = "org.forgerock.openidm.endpointservice";

    /**
     * Setup logging for the {@link EndpointsService}.
     */
    private static final Logger logger = LoggerFactory.getLogger(EndpointsService.class);



    // public static final String ROUTER_PREFIX = "endpoint";

    // Property names in configuration
    public static final String CONFIG_RESOURCE_CONTEXT = "context";

    ComponentContext context;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scriptRegistry;

    private ScriptEntry scriptEntry = null;

    private ServiceRegistration<RequestHandler> selfService = null;

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;

        //Do more programmatic registration on the Router
        String root = "endpoint/" + context.getProperties().get("config.factory-pid");
        Dictionary properties = new Hashtable();
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);
        properties.put(Constants.SERVICE_DESCRIPTION, "OpenIDM Custom Endpoints Service");
        properties.put(ServerConstants.ROUTER_PREFIX, new String[] { root });
        selfService =
                context.getBundleContext().registerService(RequestHandler.class, this, properties);

        try {
            scriptEntry =
                    scriptRegistry.takeScript(JSONEnhancedConfig.newInstance()
                            .getConfigurationAsJson(context));
        } catch (ScriptException e) {
            throw new ComponentException(e);
        }

        logger.info("OpenIDM Custom Endpoints Service component is activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        selfService.unregister();
        logger.info("OpenIDM Custom Endpoints Service component is deactivated.");
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        try {
            Script script = scriptEntry.getScript(context);
            // TODO Wrap them
            script.put("context", context);
            script.put("request", request);
            Object o = script.eval();
            if (o instanceof JsonValue) {
                handler.handleResult((JsonValue) o);
            }
        } catch (Exception e) {
            // TDOD better handling of script thrown exceptions
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Create are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Delete are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Patch are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        final ResourceException e =
                new NotSupportedException("Query are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Read are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update are not supported for resource instances");
        handler.handleError(e);
    }
}
