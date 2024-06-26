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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.servlet.internal;

import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_AUGMENT_SECURITY_CONTEXT;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_SCRIPT_EXTENSIONS;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.forgerock.http.ApiProducer;
import org.forgerock.http.DescribedHttpApplication;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.servlet.HttpFrameworkServlet;
import org.forgerock.http.swagger.OpenApiRequestFilter;
import org.forgerock.http.swagger.SwaggerApiProducer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CrestApplication;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.servletregistration.ServletFilterRegistrator;
import org.forgerock.openidm.servletregistration.ServletRegistration;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.util.Factory;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.models.Info;
import io.swagger.models.Swagger;

/**
 * A component to create and register the "API" Servlet; that is, the CHF Servlet that
 *
 * 1) listens on /openidm,
 * 2) dispatches to the HttpApplication, that is composed of
 *    a) the auth filter
 *    b) the JSON resource HTTP Handler, that
 *       i) converts CHF Requests to CREST requests, and
 *       ii) routes them on the CREST router using the external ConnectionFactory.
 */
@Component(
        name = ServletComponent.PID,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        immediate = true,
        property = Constants.SERVICE_PID + "=" + ServletComponent.PID)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM Common REST HttpServlet")
@EventTopics("org/forgerock/openidm/servlet/*")
public class ServletComponent implements EventHandler {

    static final String PID = "org.forgerock.openidm.api-servlet";

    private static final String SERVLET_ALIAS = "/openidm";

    private static final String API_ID = "frapi:openidm";

    private static final String API_TITLE = "API Explorer";

    /** Setup logging for the {@link ServletComponent}. */
    private final static Logger logger = LoggerFactory.getLogger(ServletComponent.class);

    /** The (external) ConnectionFactory */
    @Reference(policy = ReferencePolicy.DYNAMIC, target = ServerConstants.EXTERNAL_ROUTER_SERVICE_PID_FILTER)
    protected volatile ConnectionFactory connectionFactory;

    @Reference(policy = ReferencePolicy.STATIC, target = "(service.pid=org.forgerock.openidm.auth.config)")
    private Filter authFilter;

    @Reference
    private ServletRegistration servletRegistration;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected volatile ScriptRegistry scriptRegistry;

    // Optional scripts to augment/populate the security context
    private List<ScriptEntry> augmentSecurityScripts = new CopyOnWriteArrayList<>();

    // Register script extensions configured
    private Map<ServletFilterRegistrator, ScriptEntry> filterRegistratorMap = new ConcurrentHashMap<>();

    @Reference(
            name = "reference_Servlet_ServletFilterRegistrator",
            service = ServletFilterRegistrator.class,
            unbind = "unbindRegistrator",
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC
    )
    protected synchronized void bindRegistrator(ServletFilterRegistrator registrator, Map<String, Object> properties) {
        JsonValue scriptConfig = registrator.getConfiguration()
                .get(SERVLET_FILTER_SCRIPT_EXTENSIONS)
                .get(SERVLET_FILTER_AUGMENT_SECURITY_CONTEXT);
        if (!scriptConfig.isNull() && !scriptConfig.expect(Map.class).isNull()) {
            try {
                ScriptEntry augmentScript = scriptRegistry.takeScript(scriptConfig);
                filterRegistratorMap.put(registrator, augmentScript);
                augmentSecurityScripts.add(augmentScript);
                logger.debug("Registered script {}", augmentScript);
            } catch (ScriptException e) {
                logger.debug("{} when attempting to registered script {}", e.toString(), scriptConfig, e);
            }
        }
    }

    protected synchronized void unbindRegistrator(ServletFilterRegistrator registrator, Map<String, Object> properties) {
        ScriptEntry augmentScript = filterRegistratorMap.remove(registrator);
        if (augmentScript != null) {
            augmentSecurityScripts.remove(augmentScript);
            logger.debug("Deregistered script {}", augmentScript);
        }
    }

    private HttpServlet servlet;

    @Activate
    protected void activate(ComponentContext context) throws ServletException, NamespaceException {
        logger.debug("Registering servlet at {}", SERVLET_ALIAS);

        final Handler handler = CrestHttp.newHttpHandler(
                new CrestApplication() {
                    @Override
                    public ConnectionFactory getConnectionFactory() {
                        return connectionFactory;
                    }

                    @Override
                    public String getApiId() {
                        return API_ID;
                    }

                    @Override
                    public String getApiVersion() {
                        return ServerConstants.getVersion();
                    }
                }, new IDMSecurityContextFactory(augmentSecurityScripts));

        servlet = new HttpFrameworkServlet(
                new DescribedHttpApplication() {
                    @Override
                    public ApiProducer<Swagger> getApiProducer() {
                        return new SwaggerApiProducer(new Info().title(API_TITLE));
                    }

                    @Override
                    public Handler start() throws HttpApplicationException {
                        return Handlers.chainOf(handler, authFilter, new OpenApiRequestFilter());
                    }

                    @Override
                    public Factory<Buffer> getBufferFactory() {
                        return null;
                    }

                    @Override
                    public void stop() {
                    }
                });

        @SuppressWarnings("rawtypes")
        final Dictionary params = new Hashtable();
        servletRegistration.registerServlet(SERVLET_ALIAS, servlet, params);
        logger.info("Registered servlet at {}", SERVLET_ALIAS);
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        servletRegistration.unregisterServlet(servlet);
    }

    // ----- Implementation of EventHandler

    @Override
    public void handleEvent(Event event) {
        // TODO: receive the OpenIDM started event and enable the full HTTP
        // service
        if (event.getTopic().equals("org/forgerock/openidm/servlet/ACTIVATE")) {
            try {
                activate(null);
            } catch (Exception e) {
                logger.error("Error activating api-servlet", e);
            }
        } else if (event.getTopic().equals("org/forgerock/openidm/servlet/DEACTIVATE")) {
            deactivate(null);
        }
    }
}