/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.servlet.internal;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.script.ScriptException;
import javax.servlet.ServletException;

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
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.servlet.HttpServlet;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.servletregistration.ServletFilterRegistrator;
import org.forgerock.openidm.servletregistration.ServletRegistration;

import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_AUGMENT_SECURITY_CONTEXT;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_SCRIPT_EXTENSIONS;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 * 
 */
@Component(name = "org.forgerock.openidm.api-servlet",
        immediate = true,
        policy = ConfigurationPolicy.IGNORE)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Common REST HttpServlet"),
    @Property(name = EventConstants.EVENT_TOPIC, value = { "org/forgerock/openidm/servlet/*" })
})
public class ServletComponent implements EventHandler {

    public static final String PID = "org.forgerock.openidm.router";

    private static final String SERVLET_ALIAS = "/openidm";

    /** Setup logging for the {@link ServletComponent}. */
    private final static Logger logger = LoggerFactory.getLogger(ServletComponent.class);

    @Reference(policy = ReferencePolicy.DYNAMIC, target = "(service.pid=org.forgerock.openidm.router)")
    protected ConnectionFactory connectionFactory;

    @Reference
    private ServletRegistration servletRegistration;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected ScriptRegistry scriptRegistry;

    // Optional scripts to augment/populate the security context
    private List<ScriptEntry> augmentSecurityScripts = new CopyOnWriteArrayList<ScriptEntry>();

    // Register script extensions configured
    @Reference(
            name = "reference_Servlet_ServletFilterRegistrator",
            referenceInterface = ServletFilterRegistrator.class,
            bind = "bindRegistrator",
            unbind = "unbindRegistrator",
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            strategy = ReferenceStrategy.EVENT
    )
    protected Map<ServletFilterRegistrator, ScriptEntry> filterRegistratorMap =
            new HashMap<ServletFilterRegistrator, ScriptEntry>();

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
                logger.debug("{} when attempting to registered script {}", new Object[] { e.toString(), scriptConfig, e});
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
        logger.debug("Try registering servlet at {}", SERVLET_ALIAS);
        servlet = new HttpServlet(connectionFactory, new IDMSecurityContextFactory(augmentSecurityScripts));

        servletRegistration.registerServlet(SERVLET_ALIAS, servlet, new Hashtable());
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
