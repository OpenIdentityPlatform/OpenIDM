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
        
        // TODO: factor out jetty specific handling into fragment
        boolean enabled = false;
        
        // If embedded Jetty is used, enable security
        if (enabled && httpService instanceof org.ops4j.pax.web.service.WebContainer) {
            org.ops4j.pax.web.service.WebContainer webContainer = (org.ops4j.pax.web.service.WebContainer) httpService;
            String authMethod = "BASIC";
            String realmName = "openidm";
            String formLoginPage = null;
            String formErrorPage = null;
            webContainer.registerLoginConfig(authMethod, realmName, formLoginPage, formErrorPage, httpContext);
    
            String constraintName="openidm-core-login";
            String url = "/*";
            String mapping = null;
            String dataConstraint = null;
            boolean authentication = true;
            java.util.List<String> roles = new java.util.ArrayList<String>();
            //roles.add("*");
            roles.add("admin");
            roles.add("user");
            
            webContainer.registerConstraintMapping(constraintName, url, mapping, dataConstraint,
                    authentication, roles, httpContext); 
            logger.debug("Enabled Jetty security for OpenIDM shared context");
        }
    }    
}
