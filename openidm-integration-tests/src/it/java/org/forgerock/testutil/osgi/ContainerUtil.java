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
package org.forgerock.testutil.osgi;

import java.lang.Class;
import java.lang.String;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.HashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.nat.internal.NativeTestContainer;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.LibraryOptions.*;
import static org.ops4j.pax.exam.spi.container.PaxExamRuntime.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerUtil  {
    final static Logger logger = LoggerFactory.getLogger(ContainerUtil.class);
    
    Framework framework;
    // How long to wait for services to get registered
    int defaultTimeout = 30000;
    
    private ContainerUtil(Framework framework) {
        this.framework = framework;
    }
    
    /**
     * Start a test container will all the bundles prepared in the 
     * expanded openidm package in bundleDirs
     * @param bundleDirs the directory with the osgi bundles to start, comma delimited
     * @param configDir the directory with the component configuration files to use
     * @return the ContainerUtil helper wrapping the started container
     */
    public static ContainerUtil startContainer(String bundleDirs, String configDir) {
        return startContainer(bundleDirs, configDir, null);
    }
    
    /**
     * Start a test container will all the bundles prepared in the 
     * expanded openidm package in bundleDirs
     * @param bundleDirs the directory with the osgi bundles to start, comma delimited
     * @param configDir the directory with the component configuration files to use
     * @param systemProps Additional system properties to set during container start
     * @return the ContainerUtil helper wrapping the started container
     */
    public static ContainerUtil startContainer(String bundleDirs, String configDir, Map<String, String> systemProps) {
        Framework aFramework = startFramework(bundleDirs, configDir, systemProps);
        return new ContainerUtil(aFramework);
    }
    
    Map<String, String> systemProps = new HashMap<String, String>();

    /** {@inheritdoc} */
    public static ContainerUtil startContainer() {
        String bundleDirs  = "./target/openidm-pkg/openidm/bundle/init,./target/openidm-pkg/openidm/bundle";
        String configDir = "./src/it/resources/conf";
        return startContainer(bundleDirs, configDir);
    }

    /**
     * Change the default timeout to wait for services to get registered/become active
     * @param defaultTimeout timeout in milliseconds
     */
    public void setLookupTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }
    
    /**
     * Wait for a service registered with the passed interface and filter (until timeout expires)
     * 
     * Note: the returned service is a java dynamic proxy and can accept Java system classes as arguments to methods
     * 
     * Throws a runtime exception is the service did not get registered in the timeout time frame.
     * 
     * @param interfaceClass the Interface of the service to be looked up in teh OSGi service registry
     * @param extensionFilter the OSGi filter extension, to be added to the filter on the interface class passed,
     *        or null if no additional filter 
     *        The filter extension will get added to the following filter
     *        (&(objectClass=<interfaceClass name>) <filterExtension>)";
     * @param timeout a timeout in milliseconds to override the default timeout for this service lookup
     * 
     * @return the service if found within the timeout period
     */
    public <T extends Object> T getService(Class<T> interfaceClass, String extensionFilter, int timeout) {
        // wait for service 
        if (framework == null) {
            throw new RuntimeException("Can not get service when no framework handle obtained.");
        }
        Object realService = waitForServiceToStart(framework.getBundleContext(), 
                interfaceClass.getName(), timeout, extensionFilter);

        // Proxy it as it to bridge classloaders
        T proxiedService = (T) SimpleProxy.newInstance(realService, interfaceClass);
        return proxiedService;
    }
    
    /**
     * Wait for a service registered with the passed interface and filter 
     * (until default lookup timeout expires)
     * @see #getService(Class, String, int)
     */
    public <T extends Object> T getService(Class<T> interfaceClass, String extensionFilter) {
        return getService(interfaceClass, extensionFilter, defaultTimeout);
    }

    /**
     * Wait for a service registered with the passed interface 
     * (until default lookup timeout expires)
     * @see #getService(Class, String, int)
     */
    //public Object getService(Class interfaceClass) {
    public <T extends Object> T getService(Class<T> interfaceClass) {
        return getService(interfaceClass, null);
    }
    
    /**
     * Stop the test container
     */
    public void stop() {
        try {
            if (framework != null) {
                framework.stop();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to close framework " + ex.getMessage());
        }
    }

    private Object waitForServiceToStart(BundleContext ctx, String serviceName, int timeout, String extensionFilter) {
        String expectedService = serviceName;
        ServiceTracker serviceTracker = null;
        Object foundService = null;
       
        System.out.println(expectedService);
        String fullFilterStr = "(" + Constants.OBJECTCLASS + "=" + expectedService + ")";
        if (extensionFilter != null) {
            fullFilterStr = "(&(" + Constants.OBJECTCLASS + "=" + expectedService + ")" + extensionFilter + ")";
        }
        logger.info("Full filter string: " + fullFilterStr);
        try {
            Filter filter = ctx.createFilter(fullFilterStr);
            serviceTracker = new ServiceTracker(ctx, filter, null);
            serviceTracker.open();

            logger.debug("Waiting for {}", expectedService);
            foundService = serviceTracker.waitForService(timeout);
        } catch (Exception ex) {
            logger.info("Failed to wait for and find service " + expectedService + " with full filter: " 
                    + fullFilterStr + " " + ex.getMessage());
            throw new RuntimeException("Failed to wait for and find service " + expectedService + " with full filter: " 
                    + fullFilterStr + " " + ex.getMessage(), ex);
        }

        if (foundService == null) {
            ServiceReference[] refs = null;
            try {
                refs = ctx.getServiceReferences(null, null);
            } catch (Exception ex) {
                // Ignore as this is just additional debug info we tried to obtain
            }
            logger.info("Expected service " + expectedService + " with filterExtension " + extensionFilter +  
                    " never got registered. All existing service references: " + java.util.Arrays.asList(refs));
            throw new RuntimeException("Expected service " + extensionFilter + " with filterExtension " + extensionFilter 
                    +  " never got registered. full filter used: " + fullFilterStr + " . All existing service references: " 
                    + java.util.Arrays.asList(refs));
        } else {
            logger.debug("Expected service " + expectedService + " with filterExtension " + extensionFilter + " detected.") ;
        }
        serviceTracker.close();
        
        return foundService;
    }
    
    /**
     * Start the embedded OSGi framework
     */
    private static Framework startFramework(String bundleDirs, String configDir, Map<String, String> systemProps) {
        Option[] options = new Option[]{
            systemProperty("felix.fileinstall.dir").value(bundleDirs + "," + configDir),
            systemProperty("felix.fileinstall.filter").value("^((?!fileinstall).)*$"),
            //systemProperty("felix.fileinstall.log.level").value("4"),
            felix(),
            junitBundles(),
            provision(
                mavenBundle().groupId( "org.apache.felix" ).artifactId( "org.apache.felix.fileinstall").versionAsInProject()
            )
        };
        
        if (systemProps != null) {
            int extendedSize = options.length + systemProps.size();
            Option[] extendedOptions = new Option[extendedSize];
            
            int count = 0;
            for (Map.Entry<String, String> entry : systemProps.entrySet()) {
                extendedOptions[count] = systemProperty(entry.getKey()).value(entry.getValue());
                
                // Work-around: pax exam doesn't seem to set java system properties with the above properly
                System.setProperty(entry.getKey(), entry.getValue());
                // TODO: enhancement: revert system properties after test run
                
                count++;
            }
            
            System.arraycopy(options, 0, extendedOptions, systemProps.size(), options.length);
            options = extendedOptions;
            
        }
        System.out.println("Options: " + java.util.Arrays.asList(options));

        Framework aFramework = null;
        for( TestContainer testContainer : getTestContainerFactory().parse( options ) ) {
            try {
                testContainer.start();
            
                //testContainer.waitForState(long bundleId, int state, long timeoutInMillis));
                //testContainer.install( p.getStream() );
                //for( TestAddress test : p.getTests() ) {
                //    testContainer.call( test );
                //}
                
                logger.info("Test container started {}", testContainer);
                // Hack to get access to the bundle context for now
                // TODO: replace with different container start method
                java.lang.reflect.Field serverField = NativeTestContainer.class.getDeclaredField("m_framework");
                serverField.setAccessible(true);
                aFramework = (Framework) serverField.get(testContainer);
                logger.info("Framework obtained : " + aFramework);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to get access to the framework " + ex.getMessage());
            }
        }
        return aFramework;
    }
}


/**
 * Handles Proxying between the TestNG classloader and the OSGi internal classloaders
 * 
 * Note the restrictions on what it can and can not support:
 * 
 * supported:
 * - Accessing OSGi services through their interfaces
 * - Calling methods that use Java system classes (not loaded by test specific classloader)
 * - Returning types that implement an interface
 * 
 * not supported
 * - Passing arguments of classes loaded by local classloader
 * - Returning types of concrete class loaded by local classloader
 * 
 */
class SimpleProxy implements java.lang.reflect.InvocationHandler {
    final static Logger logger = LoggerFactory.getLogger(SimpleProxy.class);
    
    // The object to proxy to
    private Object obj;
    
    public static Object newInstance(Object obj, Class clazz) {
        
        Object proxyService = Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[] {clazz},
                new SimpleProxy(obj));
        return proxyService;
    }

    private SimpleProxy(Object obj) {
        this.obj = obj;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        try {
            Method meth = obj.getClass().getMethod(m.getName(), m.getParameterTypes());
            // TODO: wrap params if loaded a local classloader
            Object ret = meth.invoke(obj, args);
            Class retType = meth.getReturnType();
            if (!retType.isPrimitive() && !retType.equals(Class.forName(retType.getName()))) {
                // Bridge different classloaders
                if (retType.isInterface()) {
                    ret = newInstance(ret, retType);
                } else {
                    // Warn about incompatible classloaders
                    logger.warn("Return value is a class loaded by a different classloader, " 
                            + "which could not be proxied as return type is not an interface. " 
                            + " This may cause failures in the test if it's not treated as generic "
                            + "java.lang.Object");
                }
            }
            return ret;
        } catch (java.lang.reflect.InvocationTargetException ex) {
            // The exceptions thrown may be loaded by the OSGi classloaders and hence 
            // Would be considered different from the classes loaded in the test.
            Throwable exToThrow = ex.getTargetException();
            
            Class exClass = ex.getTargetException().getClass();
            String exName = exClass.getName();
            Class localClass = Class.forName(exName);
            if (localClass != null && !(localClass.equals(exClass))) {
                // If exception loaded by different classloader, re-create with compatible classloader
                Constructor ctor = localClass.getConstructor(new Class[] {String.class, Throwable.class});
                exToThrow = (Throwable) ctor.newInstance(
                        new Object[] {
                                ex.getTargetException().getMessage(), 
                                ex.getTargetException().getCause()});
                exToThrow.setStackTrace(ex.getTargetException().getStackTrace());
            }
            throw exToThrow;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Unexpected exception: " + ex.getMessage(), ex);
        }
    }
}
