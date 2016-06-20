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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.servlet.internal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.forgerock.openidm.jetty.JettyErrorHandler;
import org.forgerock.openidm.servletregistration.ServletRegistration;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component to create and register a Jetty error-handler servlet.
 */
@Component(name = ErrorServletComponent.PID, policy = ConfigurationPolicy.IGNORE, immediate = true)
public class ErrorServletComponent {

    static final String PID = "org.forgerock.openidm.error-servlet";

    private final static Logger logger = LoggerFactory.getLogger(ServletComponent.class);

    private static final String ERROR_SERVLET_ALIAS = "/error";

    @Reference
    private ServletRegistration servletRegistration;

    @Reference
    private WebContainer httpService;

    private HttpServlet errorServlet;

    @Activate
    protected void activate(ComponentContext context) throws ServletException, NamespaceException {
        errorServlet = new HttpServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
                    throws ServletException, IOException {
                JettyErrorHandler.outputErrorPageResponse(request, response);
            }
        };
        @SuppressWarnings("rawtypes")
        final Dictionary params = new Hashtable();
        servletRegistration.registerServlet(ERROR_SERVLET_ALIAS, errorServlet, params);
        logger.info("Registered servlet at {}", ERROR_SERVLET_ALIAS);

        httpService.registerErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE, ERROR_SERVLET_ALIAS,
                httpService.getDefaultSharedHttpContext());
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        servletRegistration.unregisterServlet(errorServlet);
        httpService.unregisterErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE,
                httpService.getDefaultSharedHttpContext());
    }

}
