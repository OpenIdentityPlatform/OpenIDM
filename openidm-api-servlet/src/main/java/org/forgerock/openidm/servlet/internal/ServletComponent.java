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

package org.forgerock.openidm.servlet.internal;

import java.util.Hashtable;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.servlet.HttpServlet;
import org.forgerock.json.resource.servlet.HttpServletContextFactory;
import org.forgerock.openidm.core.ServerConstants;
import org.ops4j.pax.web.service.WebContainer;
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
 * @author Laszlo Hordos
 * @author ckienle
 */
@Component(name = "org.forgerock.openidm.api-servlet", immediate = true,
        policy = ConfigurationPolicy.IGNORE)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Common REST HttpServlet"),
    @Property(name = EventConstants.EVENT_TOPIC, value = { "org/forgerock/openidm/servlet/*" }) })
public class ServletComponent implements EventHandler {

    /**
     * Setup logging for the {@link ServletComponent}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ServletComponent.class);

    @Reference(policy = ReferencePolicy.DYNAMIC,
            target = "(service.pid=org.forgerock.openidm.router)")
    protected ConnectionFactory connectionFactory;

    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected HttpServletContextFactory servletContextFactory;

    @Reference
    private WebContainer webContainer;
    
    private HttpServlet servlet;

    @Activate
    protected void activate(ComponentContext context) throws ServletException, NamespaceException {
        logger.debug("Try registering servlet at {}", "/openidm");
        servlet = new HttpServlet(connectionFactory, servletContextFactory);

        String alias = "/openidm";
        webContainer.registerServlet(alias, servlet, new Hashtable(), webContainer.getDefaultSharedHttpContext());
        logger.info("Registered servlet at {}", "/openidm");
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        webContainer.unregisterServlet(servlet);
    }

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
