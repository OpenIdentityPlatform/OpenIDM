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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.Scheduler;

/**
 * OSGi bundle activator for scheduler facility
 * @author aegloff
 */
public class Activator implements BundleActivator {
    final static Logger logger = LoggerFactory.getLogger(Activator.class);

     static Scheduler scheduler;
    
     public void start(BundleContext context) throws SchedulerException {
         initScheduler(context);
         logger.debug("Bundle started", context);
     }

     public void stop(BundleContext context) {
         if (scheduler != null) {
             try {
                 scheduler.shutdown();
             } catch (SchedulerException ex) {
                 logger.warn("Failure during shutdown of scheduler", ex);
             }
             logger.info("Scheduler facility shut down");
         }
         logger.debug("Bundle stopped", context);
     }
     
     public static Scheduler getScheduler() {
         return scheduler;
     }
     
     private void initScheduler(BundleContext context) throws SchedulerException {
         try {
             
             // Quartz tries to be too smart about classloading, 
             // but relies on the thread context classloader to load classload helpers
             // That is not a good idea in OSGi, 
             // hence, hand it the OSGi classloader for the ClassLoadHelper we want it to find
             ClassLoader original = Thread.currentThread().getContextClassLoader();
             Thread.currentThread().setContextClassLoader(CascadingClassLoadHelper.class.getClassLoader());
             
             SchedulerFactory sf = new StdSchedulerFactory(); 
             //sf.initialize(properties);
             scheduler = sf.getScheduler();

             // Set back to the original thread context classloader
             Thread.currentThread().setContextClassLoader(original);
             
             // Start processing schedules
             scheduler.start();
         } catch (SchedulerException ex) {
             logger.warn("Failure in initializing the scheduler facility " + ex.getMessage(), ex);
             throw ex;
         }
         logger.info("Scheduler facility started");
     }
}