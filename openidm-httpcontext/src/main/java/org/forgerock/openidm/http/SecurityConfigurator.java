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
package org.forgerock.openidm.http;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Interface to configure security
 *
 */
public interface SecurityConfigurator {

    /**
     * Let the security configurator apply its configuration 
     * @param context the component context of the main bundle
     * @param httpContext the shared http context to configure
     */
    void activate(HttpService httpService, HttpContext httpContext,  ComponentContext context);

    /**
     * Deactivate security configurators if present to cleanup
     * @param context the component context of the main bundle
     * @param httpContext the shared http context to configure
     */
    void deactivate(HttpService httpService, HttpContext httpContext,  ComponentContext context);
}
