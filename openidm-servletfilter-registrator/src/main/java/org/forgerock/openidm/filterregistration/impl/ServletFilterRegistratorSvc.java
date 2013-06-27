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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.filterregistration.impl;

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
import javax.servlet.Filter;
import javax.servlet.ServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.filterregistration.ServletFilterRegistrator;
import org.forgerock.openidm.http.ContextRegistrator;
import org.ops4j.pax.web.extender.whiteboard.FilterMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultFilterMapping;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes configuration to register and de-register configured servlet filters, 
 * with support to load the filter or supporting classes off a defined class path
 * @author aegloff
 */

@Component(
    name = "org.forgerock.openidm.servletfilter", 
    immediate = true,
    policy = ConfigurationPolicy.REQUIRE,
    configurationFactory=true
)
@Service
public class ServletFilterRegistratorSvc implements ServletFilterRegistrator {
    private final static Logger logger = LoggerFactory.getLogger(ServletFilterRegistratorSvc.class);

    // Config received for this component
    private JsonValue config;

    // Handle to registered servlet filter
    private ServiceRegistration serviceRegistration;
    
    // Original setting of system properties
    Map<String, String> origSystemProperties = new HashMap<String, String>();
    
    Map<String, Object> preInvokeReqAttributes;

    @Activate
    protected synchronized void activate(ComponentContext context) throws Exception {
        logger.info("Activating servlet registrator with configuration {}", context.getProperties());
        config = new JSONEnhancedConfig().getConfigurationAsJson(context);

        logger.debug("Parsed servlet filter config: {}", config);

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
        origSystemProperties = new HashMap<String, String>();
        JsonValue rawSystemProperties = config.get("systemProperties");
        for (String key : rawSystemProperties.keys()) {
            String prev = System.setProperty(key, rawSystemProperties.get(key).asString());
            // null value is used to keep track of properties that weren't set before
            origSystemProperties.put(key, prev);
        }
        preInvokeReqAttributes = config.get("requestAttributes").asMap();
        
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
                        new Class[] { Filter.class },
                        new FilterProxy(filter, filterCL, preInvokeReqAttributes));
        
        DefaultFilterMapping filterMapping = new DefaultFilterMapping();
        filterMapping.setFilter(proxiedFilter);
        filterMapping.setHttpContextId(httpContextId);
        filterMapping.setServletNames(servletNames.toArray(new String[0]));
        filterMapping.setUrlPatterns(urlPatterns.toArray(new String[0]));
        filterMapping.setInitParams(initParams);
        serviceRegistration = FrameworkUtil.getBundle(ContextRegistrator.class).getBundleContext().registerService(FilterMapping.class.getName(), filterMapping, null);
        logger.info("Successfully registered servlet filter {}", context.getProperties());
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext context) {
        if (serviceRegistration != null) {
            try {
                serviceRegistration.unregister();
                logger.info("Unregistered servlet filter {}.", context.getProperties());
            } catch (Exception ex) {
                logger.warn("Failure reported during unregistering of servlet filter {}: {}", 
                        new Object[]{context.getProperties(), ex.getMessage(), ex});
            }
        }

        // Restore the system properties before this config was applied
        for (String key : origSystemProperties.keySet()) {
            String val = origSystemProperties.get(key);
            if (val == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, val);
            }
        }
        logger.debug("Deactivated {}", context);
    }
    
    /**
     * {@inheritDoc}
     */
    public JsonValue getConfiguration() {
        return config;
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

