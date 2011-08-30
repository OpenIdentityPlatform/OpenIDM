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
 *
 * $Id$
 */
package org.forgerock.openidm.web.osgi;

import javax.servlet.ServletContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.ServletContextAware;

public class OSGIServiceBridge implements ServletContextAware, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(OSGIServiceBridge.class);
    private ServletContext context = null;
    private ApplicationContext appContext = null;
    private boolean timedOut = false;

    public <T> T getService(Class<T> clazz) {
        T service = null;
        if (null != context) {
            Object o = this.context.getAttribute(BundleContext.class.getName());
            if (o instanceof BundleContext) {
                BundleContext bundleContext = (BundleContext) o;
                int RETRIES = 0;
                while (null == service) {
                    ServiceReference ref = bundleContext.getServiceReference(clazz.getName());
                    if (null != ref) {
                        service = (T) bundleContext.getService(ref);
                    }
                    if (timedOut || null != service || RETRIES > 5) {
                        if (timedOut) {
                            logger.error("System initialization has timed out once. Restart the application for the proper start.");
                        }
                        break;
                    } else {
                        RETRIES++;
                        try {
                            logger.info("Waiting for OSGi context to be initialised");
                            synchronized (appContext) {
                                appContext.wait(5000l);
                            }
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                }
                if (RETRIES > 5) {
                    timedOut = true;
                }
            } else {
                throw new IllegalArgumentException("Missing BundleContext");
            }
        }
        return service;
    }

    @Override
    public void setServletContext(ServletContext sc) {
        this.context = sc;
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        appContext = ac;
    }
}
