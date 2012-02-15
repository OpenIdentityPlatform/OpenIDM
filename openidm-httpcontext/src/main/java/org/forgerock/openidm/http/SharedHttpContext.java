/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class SharedHttpContext implements HttpContext {

    /**
     * Logger.
     */
    final Logger logger = LoggerFactory.getLogger(SharedHttpContext.class);

    private Queue<Bundle> bundles = new ConcurrentLinkedQueue<Bundle>();

    /**
     * The http context to delegate to.
     */
    private final HttpContext httpContext;

    public SharedHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    /**
     * {@inheritDoc}
     */
    public boolean handleSecurity(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        return null != httpContext ? httpContext.handleSecurity(httpServletRequest, httpServletResponse) : true;
    }

    /**
     * {@inheritDoc}
     */
    public URL getResource(String path) {
        URL pathUrl = null != httpContext ? httpContext.getResource(path) : null;
        if (null == pathUrl) {
            for (Bundle bundle : bundles) {
                pathUrl = getResource(bundle, path);
                if (pathUrl != null) {
                    break;
                }
            }
        }
        return pathUrl;
    }

    /**
     * {@inheritDoc}
     */
    public String getMimeType(String s) {
        return null != httpContext ? httpContext.getMimeType(s) : null;
    }

    public boolean registerBundle(Bundle bundle) {
        if (!bundles.contains(bundle)) {
            bundles.add(bundle);
            logger.debug("Register bundle [{}]", bundle);
            return true;
        }
        return false;
    }

    public boolean deregisterBundle(Bundle bundle) {
        logger.debug("Deregister bundle [{}]", bundle);
        return bundles.remove(bundle);
    }

    private URL getResource(Bundle bundle, final String name) {
        final String normalizedname = normalizeResourcePath(name);
        logger.debug("Searching bundle [{}] for resource [{}]", bundle, normalizedname);
        return bundle.getResource(normalizedname);
    }

    /**
     * TODO: convert the path to OSGi friendly format
     */
    protected String normalizeResourcePath(String path) {
        return path;
    }
}
