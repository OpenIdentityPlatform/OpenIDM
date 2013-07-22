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

package org.forgerock.openidm.filterregistration.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.filterregistration.ServletFilterRegistration;
import org.forgerock.openidm.filterregistration.ServletFilterRegistrator;
import org.forgerock.openidm.http.ContextRegistrator;
import org.ops4j.pax.web.extender.whiteboard.FilterMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultFilterMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton service for registering servlet filters in OSGi.
 *
 * @author Phill Cunnington
 */
@Component(
        name = "org.forgerock.openidm.servletfilter.registrator",
        policy = ConfigurationPolicy.IGNORE,
        immediate = true
)
@Service
public class ServletFilterRegistrationSingleton implements ServletFilterRegistration {

    private final static Logger logger = LoggerFactory.getLogger(ServletFilterRegistrationSingleton.class);

    private BundleContext bundleContext;

    /**
     * Initialises the BundleContext from the ComponentContext.
     *
     * @param context The ComponentContext.
     */
    @Activate
    public void activate(ComponentContext context) {
        bundleContext = context.getBundleContext();
    }

    /**
     * Nullifies the BundleContext member variable.
     *
     * @param context The ComponentContext.
     */
    @Deactivate
    public void deactivate(ComponentContext context) {
        bundleContext = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceRegistration registerFilter(JsonValue config) throws Exception {
        // Get required info from config
        String filterClass = config.get("filterClass").required().asString();
        logger.info("Using filter class: {}", filterClass);
        JsonValue urlStrings = config.get("classPathURLs");
        List<URL> urls = new ArrayList<URL>();
        for (JsonValue urlStr : urlStrings.expect(List.class)) {
            try {
                URL url = new URL(urlStr.asString());
                urls.add(url);
                logger.info("Added URL to filter classpath: {}", url);
            } catch (MalformedURLException ex) {
                logger.warn("Configured classPathURL is not a valid URL: {}", urlStr, ex);
                throw ex;
            }
        }

        Map<String, Object> preInvokeReqAttributes = config.get("requestAttributes").asMap();

        String httpContextId = config.get("httpContextId").defaultTo("openidm").asString();

        // Servlet names this filter should apply to, e.g. one could also add "OpenIDM Web"
        List<String> servletNames = null;
        JsonValue rawServletNames = config.get("servletNames");
        if (rawServletNames.isNull()) {
            // default
            servletNames = new ArrayList<String>();
            servletNames.add("OpenIDM REST");
        } else {
            servletNames = rawServletNames.asList(String.class);
        }

        // URL patterns to apply the filter to, e.g. one could also add "/openidmui/*");
        List<String> urlPatterns = null;
        JsonValue rawUrlPatterns = config.get("urlPatterns");
        if (rawUrlPatterns.isNull()) {
            // default
            urlPatterns = new ArrayList<String>();
            urlPatterns.add("/openidm/*");
            // TODO Now that UI urlContexts are configurable, the servlet-filter config
            // either needs to list the urlContext urlPatterns that are configured by
            // ui.context-<x>.json, or this code needs to programmatically deduce the
            // urlPatterns represented by UI-context config (preferred).
            urlPatterns.add("/openidmui/*");
        } else {
            urlPatterns = rawUrlPatterns.asList(String.class);
        }

        // Filter init params, a string to string map
        JsonValue rawInitParams = config.get("initParams");
        Map<String, String> initParams = null;
        if (!rawInitParams.isNull()) {
            initParams = new HashMap<String, String>();
        }
        for (String initParamKey : rawInitParams.keys()) {
            initParams.put(initParamKey, rawInitParams.get(initParamKey).asString());
        }

        // Create a classloader and dynamically create the requested filter
        Filter filter = null;
        ClassLoader filterCL = null;
        ClassLoader origCL = Thread.currentThread().getContextClassLoader();
        try {
            filterCL = new URLClassLoader(urls.toArray(new URL[0]), this.getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(filterCL);
            filter = (Filter)(Class.forName(filterClass, true, filterCL).newInstance());
        } catch (Exception ex) {
            logger.warn("Configured class {} failed to load from configured class path URLs {}", new Object[] {filterClass, urls, ex});
            throw ex;
        } finally {
            Thread.currentThread().setContextClassLoader(origCL);
        }

        Filter proxiedFilter = (Filter)
                Proxy.newProxyInstance(
                        filter.getClass().getClassLoader(),
                        new Class[]{Filter.class},
                        new FilterProxy(filter, filterCL, preInvokeReqAttributes));

        DefaultFilterMapping filterMapping = new DefaultFilterMapping();
        filterMapping.setFilter(proxiedFilter);
        filterMapping.setHttpContextId(httpContextId);
        filterMapping.setServletNames(servletNames.toArray(new String[0]));
        filterMapping.setUrlPatterns(urlPatterns.toArray(new String[0]));
        filterMapping.setInitParams(initParams);

        ServiceRegistration serviceRegistration = FrameworkUtil.getBundle(ContextRegistrator.class).getBundleContext().registerService(FilterMapping.class.getName(), filterMapping, null);
        ServletFilterRegistrator servletFilterRegistrator = new ServletFilterRegistratorSvc(config);
        bundleContext.registerService(ServletFilterRegistrator.class.getName(), servletFilterRegistrator, null);
        logger.info("Successfully registered servlet filter {}", config);
        return serviceRegistration;
    }

    /**
     * Wraps Filter to set the thread context classloader
     * @author aegloff
     */
    private static class FilterProxy implements InvocationHandler {
        Filter filter;
        ClassLoader threadCtxClassloader;
        Map<String, Object> preInvokeReqAttributes;
        /**
         * Sets the thread context for a filter invocation to the desired value
         * @param filter filter to wrap
         * @param threadCtxClassloader classloader to set as thread context classloader
         * @param preInvokeReqAttributes request attributes to set before the filter doFilter is invoked
         */
        public FilterProxy(Filter filter, ClassLoader threadCtxClassloader,
                Map<String, Object> preInvokeReqAttributes) {
            this.filter = filter;
            this.threadCtxClassloader = threadCtxClassloader;
            this.preInvokeReqAttributes = preInvokeReqAttributes;
        }

        public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
            ClassLoader origCL = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(threadCtxClassloader);
                if (preInvokeReqAttributes != null) {
                    if (m.getName().equals("doFilter") && args != null && args.length == 3 && args[0] instanceof ServletRequest) {
                        ServletRequest request = (ServletRequest) args[0];
                        for (Map.Entry<String, Object> entry : preInvokeReqAttributes.entrySet()) {
                            logger.trace("Adding attribute to doFilter request {}", entry);
                            request.setAttribute(entry.getKey(), entry.getValue());
                        }
                    }
                }
                Object val = m.invoke(filter, args);
                return val;
            } catch (InvocationTargetException e) {
                logger.debug("Filter invocation InvocationTargetException", e.getTargetException());
                throw e.getTargetException();
            } catch (Exception e) {
                logger.debug("Filter invocation Exception", e);
                throw e;
            } finally {
                Thread.currentThread().setContextClassLoader(origCL);
            }
        }
    }
}
