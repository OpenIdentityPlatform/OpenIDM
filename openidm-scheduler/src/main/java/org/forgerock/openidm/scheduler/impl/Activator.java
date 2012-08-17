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
package org.forgerock.openidm.scheduler.impl;

import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle activator for scheduler facility
 * @author aegloff
 */
public class Activator implements BundleActivator {
    final static Logger logger = LoggerFactory.getLogger(Activator.class);

    static Scheduler inMemoryScheduler;
    static Scheduler persistentScheduler;

    public void start(BundleContext context) throws SchedulerException {
        initScheduler(context);
        logger.debug("Bundle started", context);
    }

    public void stop(BundleContext context) {
        if (inMemoryScheduler != null) {
            try {
                inMemoryScheduler.shutdown();
            } catch (SchedulerException ex) {
                logger.warn("Failure during shutdown of scheduler", ex);
            }
            logger.info("Scheduler facility shut down");
        }
        logger.debug("Bundle stopped", context);
    }

    public static Scheduler getInMemoryScheduler() {
        return inMemoryScheduler;
    }

    public static Scheduler getPersistentScheduler() throws SchedulerException {
        if (persistentScheduler == null) {
            try {
                // Create a properties object for our custom JobStore implementation
                // TODO: Makes some (or all) of these properties configurable?
                // TODO: Add setting of org.quartz.scheduler.idleWaitTime (time between acquireNextTrigger calls)?
                Properties persistentProps = new Properties();
                persistentProps.put("org.quartz.scheduler.instanceName", "DefaultQuartzScheduler");
                persistentProps.put("org.quartz.scheduler.rmi.export", "false");
                persistentProps.put("org.quartz.scheduler.rmi.proxy", "false");
                persistentProps.put("org.quartz.scheduler.wrapJobExecutionInUserTransaction", "false");
                persistentProps.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
                persistentProps.put("org.quartz.threadPool.threadCount", "10");
                persistentProps.put("org.quartz.threadPool.threadPriority", "5");
                persistentProps.put("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
                persistentProps.put("org.quartz.jobStore.class", "org.forgerock.openidm.quartz.impl.RepoJobStore");

                // Get the persistent scheduler using our custom JobStore implementation
                logger.info("Creating Persistent Scheduler");
                StdSchedulerFactory sf = new StdSchedulerFactory();
                sf.initialize(persistentProps);
                persistentScheduler = sf.getScheduler();
                
                logger.info("Starting Persistent Scheduler");
                persistentScheduler.start();
            } catch (SchedulerException e) {
                logger.warn("Failure in initializing the scheduler facility " + e.getMessage(), e);
                throw e;
            }
        }
        return persistentScheduler;
    }

    private void initScheduler(BundleContext context) throws SchedulerException {
        try {

            // Quartz tries to be too smart about classloading, 
            // but relies on the thread context classloader to load classload helpers
            // That is not a good idea in OSGi, 
            // hence, hand it the OSGi classloader for the ClassLoadHelper we want it to find
            ClassLoader original = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(CascadingClassLoadHelper.class.getClassLoader());
            
            // Get the in-memory scheduler
            // Must use DirectSchedulerFactory instance so that it does not confict with
            // the StdSchedulerFactory (used to create the persistent schedulers below).
            logger.info("Creating In-Memory Scheduler");
            DirectSchedulerFactory.getInstance().createVolatileScheduler(10);
            inMemoryScheduler = DirectSchedulerFactory.getInstance().getScheduler();

            // Set back to the original thread context classloader
            Thread.currentThread().setContextClassLoader(original);

            // Start processing schedules
            logger.info("Starting Volatile Scheduler");
            inMemoryScheduler.start();
        } catch (SchedulerException ex) {
            logger.warn("Failure in initializing the scheduler facility " + ex.getMessage(), ex);
            throw ex;
        }
        logger.info("Scheduler facility started");
    }
}