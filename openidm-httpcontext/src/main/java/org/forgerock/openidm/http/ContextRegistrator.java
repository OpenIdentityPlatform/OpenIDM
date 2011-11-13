/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.forgerock.openidm.http;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Http context to share amongst OpenIDM servlets to allow
 * for applying uniform security handling
 *
 * @author aegloff
 */
@Component(name = "org.forgerock.openidm.http.contextregistrator", 
        immediate = true,
        policy = ConfigurationPolicy.IGNORE)
public final class ContextRegistrator {
    final static Logger logger = LoggerFactory.getLogger(ContextRegistrator.class);
    
    @Reference
    HttpService httpService;

    ComponentContext context;
    
    @Activate
    protected void activate(ComponentContext context) throws ServletException, NamespaceException {
        this.context = context;

        // register the http context so other bundles can add filters etc.
        HttpContext httpContext = httpService.createDefaultHttpContext();
        Dictionary<String, Object> contextProps = new Hashtable<String, Object>();
        contextProps.put("openidm.contextid", "shared");
        context.getBundleContext().registerService(HttpContext.class.getName(), httpContext, contextProps);
        logger.debug("Registered OpenIDM shared http context");
        
        callSecurityConfigurators(context, httpContext);
    }
    
    /**
     * Call security configurators if present to enable security
     */
    private void callSecurityConfigurators(ComponentContext context, HttpContext httpContext) {
        // TODO: dynamic discovery of security configurator(s)
        String clazzName = "org.forgerock.openidm.http.internal.JettySecurityConfigurator";
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
                SecurityConfigurator configurator = (SecurityConfigurator) instance;
                configurator.activate(httpService, httpContext, context);
                logger.debug("Completed security configurator {}", clazzName);
            }
        } catch (Exception ex) {
            logger.warn("Failed to load security configurator class {}", clazzName, ex);
        }
    }
}
