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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openidm.servletregistration;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.forgerock.json.fluent.JsonValue;
import org.osgi.service.http.NamespaceException;

import java.util.Dictionary;

/**
 * Interface for registering servlet filters and servlets in OSGi.
 *
 * @author Phill Cunnington
 * @author ckienle
 * @author Alin Brici
 */
public interface ServletRegistration {

    /**
     * Parses the given servlet filter configuration and registers a servlet filter in OSGi.
     *
     * @param config The servlet filter configuration.
     * @return The registered Filter.
     * @throws Exception If a problem occurs registering the servlet filter.
     */
    RegisteredFilter registerFilter(JsonValue config) throws Exception;

    /**
     * Unregisters a servlet filter in OSGi.
     *
     * @param filter The registered Filter.
     * @throws Exception If a problem occurs unregistering the servlet filter.
     */
    void unregisterFilter(RegisteredFilter filter) throws Exception;

    /**
     * Registers a servlet in OSGI
     * @param alias name in the URI namespace at which the servlet is registered
     * @param servlet the servlet object to register
     * @param initparams initialization arguments for the servlet or
     *        <code>null</code> if there are none. This argument is used by the
     *        servlet's <code>ServletConfig</code> object.
     * @throws Exception if a problem occurs registering a servlet filter
     */
    void registerServlet(String alias, Servlet servlet, Dictionary initparams) throws ServletException, NamespaceException;

    /**
     * Unregisters a servlet in OSGI
     * @param servlet the servlet to be unregistered
     */
    void unregisterServlet(Servlet servlet);

}
