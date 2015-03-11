/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.http.internal;

import org.forgerock.openidm.http.SecurityConfigurator;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apply Jetty security constraints of applicable.
 */
public final class JettySecurityConfigurator implements SecurityConfigurator {
    final static Logger logger = LoggerFactory.getLogger(JettySecurityConfigurator.class);

    static String enabled = null;
    static {
         enabled = System.getProperty("openidm.jetty.jaas.enabled", "false");
    }
    
    public JettySecurityConfigurator() {
    }

    public void activate(HttpService httpService, HttpContext httpContext,  ComponentContext context) {
        
        // TODO: consider making configurable

        // If embedded Jetty is used, enable security
        if (enabled.equals("true") && httpService instanceof org.ops4j.pax.web.service.WebContainer) {
            org.ops4j.pax.web.service.WebContainer webContainer = (org.ops4j.pax.web.service.WebContainer) httpService;
/*
            String authMethod = "FORM";
            String realmName = "openidm";
            String formLoginPage = "/login/login";
            String formErrorPage = "/login/error";
*/
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
            roles.add("openidm-authorized");
            //roles.add("*");
            
            webContainer.registerConstraintMapping(constraintName, url, mapping, dataConstraint,
                    authentication, roles, httpContext); 
            logger.debug("Enabled Jetty security for OpenIDM shared context");
        }
    }

    public void deactivate(HttpService httpService, HttpContext httpContext,  ComponentContext context) {
    }
}
