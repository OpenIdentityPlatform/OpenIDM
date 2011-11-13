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
package org.forgerock.openidm.http.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.forgerock.openidm.http.SecurityConfigurator;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apply Jetty security constraints of applicable
 *
 * @author aegloff
 */
public final class JettySecurityConfigurator implements SecurityConfigurator {
    final static Logger logger = LoggerFactory.getLogger(JettySecurityConfigurator.class);

    public void activate(HttpService httpService, HttpContext httpContext,  ComponentContext context) {
        
        // TODO: consider making configurable
        boolean enabled = true;

        // If embedded Jetty is used, enable security
        if (enabled && httpService instanceof org.ops4j.pax.web.service.WebContainer) {
            org.ops4j.pax.web.service.WebContainer webContainer = (org.ops4j.pax.web.service.WebContainer) httpService;
            String authMethod = "BASIC";
            String realmName = "openidm";
            String formLoginPage = null;
            String formErrorPage = null;
            webContainer.registerLoginConfig(authMethod, realmName, formLoginPage, formErrorPage, httpContext);
    
            String constraintName="openidm-core-login";
            String url = "/openidm/*";
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
