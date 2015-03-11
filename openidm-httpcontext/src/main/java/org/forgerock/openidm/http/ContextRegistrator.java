/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Http context to share amongst OpenIDM servlets to allow for applying uniform
 * security handling
 * 
 */
@Component(name = "org.forgerock.openidm.http.context", immediate = true,
        policy = ConfigurationPolicy.IGNORE)
public final class ContextRegistrator {
    final static Logger logger = LoggerFactory.getLogger(ContextRegistrator.class);
    public static final String OPENIDM = "openidm";

    @Reference
    private WebContainer httpService;

    HttpContext httpContext;

    static ComponentContext context;

    List<SecurityConfigurator> securityConfigurators = new ArrayList<SecurityConfigurator>();

    /**
     * Allow access for the fragments
     * 
     * @return bundle context if activated, null otherwise
     */
    public static BundleContext getBundleContext() {
        if (context != null) {
            return context.getBundleContext();
        } else {
            return null;
        }
    }

    @Activate
    protected void activate(ComponentContext context) throws Exception {
        ContextRegistrator.context = context;
        httpContext = httpService.createDefaultHttpContext();
        Dictionary<String, Object> contextProps = new Hashtable<String, Object>();
        contextProps.put("openidm.contextid", "shared");
        // TODO: Consider the HttpContextMapping it allows to configure the path
        context.getBundleContext().registerService(HttpContext.class.getName(), httpContext,
                contextProps);
        logger.debug("Registered OpenIDM shared http context");

        // Apply the pluggable security configurations on the httpContext
        initSecurityConfigurators();
        activateSecurityConfigurators(context, httpContext);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        deactivateSecurityConfigurators(context, httpContext);
    }

    /**
     * Loads and instantiates the pluggable Security Configurators
     * 
     * To allow for pluggability with fragments a simple convention is used to
     * find security configurator(s); 1. A properties file contains a property
     * security.configurator.class of a class in the bundle fragment that
     * implements SecurityConfigurator and has a no-arg constructor 2. The
     * properties file is name <prefix>securityconfigurator.properties and
     * placed somewhere in the bundle or attached fragments
     */
    private void initSecurityConfigurators() {

        Enumeration<URL> entries =
                context.getBundleContext().getBundle().findEntries("/",
                        "*securityconfigurator.properties", true);
        while (entries != null && entries.hasMoreElements()) {
            URL entry = entries.nextElement();
            logger.trace("Handle properties file at {}", entry.getPath());

            InputStream is = null;
            java.util.Properties props = new java.util.Properties();
            try {
                is = entry.openStream();
                props.load(is);
            } catch (IOException ex) {
                logger.warn("Failed to load security extension properties file", ex);
                try {
                    is.close();
                } catch (Exception cex) {
                    logger.warn("Failure during close of properties file.", cex);
                }
            }
            logger.trace("Loaded {}: {}", entry.getPath(), props);
            if (props != null) {
                String clazzName = (String) props.get("security.configurator.class");
                logger.debug("Initiating security configurator for class: {}", clazzName);
                SecurityConfigurator configurator = instantiateSecurityConfigurator(clazzName);
                if (configurator != null) {
                    securityConfigurators.add(configurator);
                }
            }
        }
    }

    /**
     * @param clazzName
     *            name of SecurityConfigurator to load and instantiate
     * @return the security configurator instance if it was successfully
     *         instantiated, null if not. Logs any failures
     */
    SecurityConfigurator instantiateSecurityConfigurator(String clazzName) {
        SecurityConfigurator configurator = null;
        Class configuratorClazz = null;
        try {
            configuratorClazz = context.getBundleContext().getBundle().loadClass(clazzName);
            logger.debug("Loaded configurator class {}", clazzName);
        } catch (ClassNotFoundException ex) {
            logger.debug("Security configurator not present: {}", clazzName);
        }
        try {
            if (configuratorClazz != null) {
                Object instance = configuratorClazz.newInstance();
                logger.debug("Instantiated configurator {}", instance);
                configurator = (SecurityConfigurator) instance;
            }
        } catch (Exception ex) {
            logger.warn("Failed to load security configurator class {}", clazzName, ex);
        }
        return configurator;
    }

    /**
     * Activate security configurators if present to enable security
     * 
     * @param context
     *            the component context of the main bundle
     * @param httpContext
     *            the shared http context to configure
     */
    private void activateSecurityConfigurators(ComponentContext context, HttpContext httpContext) {
        for (SecurityConfigurator configurator : securityConfigurators) {
            configurator.activate(httpService, httpContext, context);
            logger.info("Activated security configurator {}", configurator.getClass().getName());
        }
    }

    /**
     * Deactivate security configurators if present to cleanup
     * 
     * @param context
     *            the component context of the main bundle
     * @param httpContext
     *            the shared http context to configure
     */
    private void deactivateSecurityConfigurators(ComponentContext context, HttpContext httpContext) {
        for (SecurityConfigurator configurator : securityConfigurators) {
            configurator.deactivate(httpService, httpContext, context);
            logger.debug("Deactivated security configurator {}", configurator.getClass().getName());
        }
    }

    public String getHttpContextId() {
        return OPENIDM;
    }
}
