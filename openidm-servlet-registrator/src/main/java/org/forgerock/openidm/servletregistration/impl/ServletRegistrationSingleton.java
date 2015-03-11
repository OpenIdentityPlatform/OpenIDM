/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.servletregistration.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.servletregistration.RegisteredFilter;
import org.forgerock.openidm.servletregistration.ServletRegistration;
import org.forgerock.openidm.servletregistration.ServletFilterRegistrator;
import org.forgerock.util.promise.Function;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes configuration to register and de-register configured servlet filters, 
 * with support to load the filter or supporting classes off a defined class path
 * 
 */

@Component(
    name = "org.forgerock.openidm.servletfilter.registrator",
    immediate = true,
    policy = ConfigurationPolicy.IGNORE,
    configurationFactory=true
)
@Service
public class ServletRegistrationSingleton implements ServletRegistration {

    private static final Logger logger = LoggerFactory.getLogger(ServletRegistrationSingleton.class);

    private static final String[] DEFAULT_SERVLET_NAME = new String[] { "OpenIDM REST" };

    private static final String[] DEFAULT_SERVLET_URL_PATTERNS = new String[] { "/openidm/*", "/openidmui/*" };

    // Context of this scr component
    private BundleContext bundleContext;
    
    @Reference
    private WebContainer webContainer;
    
    private List<RegisteredFilterImpl> filters = new ArrayList<RegisteredFilterImpl>();
    
    private final static Object registrationLock = new Object();
    
    /**
     * Initialises the ComponentContext.
     *
     * @param context The ComponentContext.
     */
    @Activate
    public void activate(ComponentContext context) {
        bundleContext = context.getBundleContext();
    }

    /**
     * Nullifies the ComponentContext member variable.
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
    public void registerServlet(String alias, Servlet servlet, Dictionary initparams) throws ServletException, NamespaceException {
        webContainer.registerServlet(alias, servlet, initparams, webContainer.getDefaultSharedHttpContext());
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterServlet(Servlet servlet) {
        webContainer.unregisterServlet(servlet);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public RegisteredFilter registerFilter(JsonValue config) throws Exception {
        synchronized (registrationLock) {
            RegisteredFilterImpl newFilter = new RegisteredFilterImpl(config);
            // Unregister existing filters if necessary
            for (RegisteredFilter regFilter : filters) {
                unregisterFilterWithWebContainer(regFilter.getFilter());
            }
            // Add new filter
            filters.add(newFilter);
            // Sort the filters
            Collections.sort(filters);
            // Register all filters
            Exception failedRegistrationException = null;
            for (RegisteredFilterImpl regFilter : filters) {
                try {
                    regFilter.setFilter(registerFilterWithWebContainer(regFilter.getConfig()));
                } catch (Exception e) {
                    logger.error("Error registering filter {}", config);
                    if (regFilter.equals(newFilter)) {
                        failedRegistrationException = e;
                    }
                }
            }
            // Throw the exception if the new filter failed to register
            if (failedRegistrationException != null) {
                filters.remove(newFilter);
                throw failedRegistrationException;
            }
            logger.info("Successfully registered servlet filter {}", config);
            // Register ServletFilterRegistrator service
            registerFilterService(newFilter.getConfig());
            return newFilter;
        }
    }
    
    /**
     * Registers a servlet filter configuration
     * @param config the filter configuration
     * @return the registered Filter
     * @throws Exception
     */
    private Filter registerFilterWithWebContainer(JsonValue config) throws Exception {
        // Get required info from config
        String filterClass = config.get(SERVLET_FILTER_CLASS).required().asString();
        logger.info("Using filter class: {}", filterClass);
        List<URL> urls = config.get(SERVLET_FILTER_CLASS_PATH_URLS).asList(
                new Function<JsonValue, URL, JsonValueException>() {
                    @Override
                    public URL apply(JsonValue jsonValue) throws JsonValueException {
                        return jsonValue.asURL();
                    }
                });
        logger.info("Added URLs { {} })) to filter classpath", StringUtils.join(urls, ", "));

        Map<String, Object> preInvokeReqAttributes = config.get(SERVLET_FILTER_PRE_INVOKE_ATTRIBUTES).asMap();
        
        // Servlet names this filter should apply to, e.g. one could also add "OpenIDM Web"
        List<String> servletNames = config.get(SERVLET_FILTER_SERVLET_NAMES)
                .defaultTo(Arrays.asList(DEFAULT_SERVLET_NAME))
                .asList(String.class);

        // URL patterns to apply the filter to, e.g. one could also add "/openidmui/*");
        List<String> urlPatterns = config.get(SERVLET_FILTER_URL_PATTERNS)
                .defaultTo(Arrays.asList(DEFAULT_SERVLET_URL_PATTERNS))
                .asList(String.class);

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

        // Create filter
        Filter proxiedFilter = (Filter) Proxy.newProxyInstance(
                filter.getClass().getClassLoader(),
                new Class[] { Filter.class },
                new FilterProxy(filter, filterCL, preInvokeReqAttributes));

        // Register filter
        webContainer.registerFilter(proxiedFilter,
                urlPatterns.toArray(new String[urlPatterns.size()]),
                servletNames.toArray(new String[servletNames.size()]),
                new Hashtable<String, Object>(initParams),
                webContainer.getDefaultSharedHttpContext());
        return proxiedFilter;
    }
    
    private void registerFilterService(JsonValue config) {
        // Register ServletFilterRegistrator service
        ServletFilterRegistrator servletFilterRegistrator = new ServletFilterRegistratorSvc(config);
        bundleContext.registerService(ServletFilterRegistrator.class.getName(), servletFilterRegistrator, null);
    }

    @Override
    public void unregisterFilter(RegisteredFilter filter) throws Exception {
        synchronized (registrationLock) {
            unregisterFilterWithWebContainer(filter.getFilter());
            filters.remove(filter);
            logger.info("Successfully unregistered servlet filter {}", filter);
        }
    }
    
    public void unregisterFilterWithWebContainer(Filter filter) {
        webContainer.unregisterFilter(filter);
    }
    
    /**
     * Wraps Filter to set the thread context classloader
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

