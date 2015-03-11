/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.test;

import static org.testng.Assert.assertNotNull;

import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.forgerock.commons.launcher.OSGiFramework;
import org.forgerock.commons.launcher.OSGiFrameworkService;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.testng.IObjectFactory;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.testng.internal.ObjectFactoryImpl;

/**
 * A ITTestSetup start the OpenIDM.
 * 
 */
@Test(groups = { "common" })
public class ITTestSetup {

    private final Semaphore available = new Semaphore(1, true);

    @ObjectFactory
    public IObjectFactory createFactory(ITestContext context) throws Exception {
        startOpenIDM(context);
        return new ObjectFactoryImpl();
    }

    @BeforeSuite
    public void startOpenIDM(ITestContext context) throws Exception {

        OSGiFrameworkService service =
                (OSGiFrameworkService) context.getAttribute(OSGiFramework.class.getName());
        if (null == service) {
            String test =  System.getProperty("testName");
            String test1 =   context.getCurrentXmlTest().getParameter("testName");

            Properties p = System.getProperties();

            Set<String> para = context.getAttributeNames();

            Map<String,String> pp = context.getCurrentXmlTest().getParameters();

            String installLocation =System.getProperty(
                            OSGiFramework.LAUNCHER_INSTALL_LOCATION);
            assertNotNull(installLocation, "The " + OSGiFramework.LAUNCHER_INSTALL_LOCATION
                    + " System Property  is not set!");
            String projectLocation =System.getProperty(
                            OSGiFramework.LAUNCHER_PROJECT_LOCATION);
            assertNotNull(projectLocation, "The " + OSGiFramework.LAUNCHER_PROJECT_LOCATION
                    + " System Property is not set!");
            String workingLocation = System.getProperty(
                            OSGiFramework.LAUNCHER_WORKING_LOCATION);
            assertNotNull(workingLocation, "The " + OSGiFramework.LAUNCHER_WORKING_LOCATION
                    + " System Property is not set!");

            URL configFile = ITTestSetup.class.getResource("/bin/launcher.json");
            assertNotNull(configFile, "The /bin/launcher.json is missing!");
            service = new OSGiFrameworkService();

            available.drainPermits();
            service.setFrameworkListener(new FrameworkListener() {
                public void frameworkEvent(FrameworkEvent event) {
                    if (event.getType() == FrameworkEvent.STARTED) {
                        available.release();
                    }
                }
            });

            service.setConfigFile(URLDecoder.decode(configFile.getFile(), "utf-8"));
            service.setInstallDir(installLocation);
            service.setProjectDir(projectLocation);
            service.setWorkingDir(workingLocation);
            service.setVerbose(true);

            Map bootParameters = null;
            for (String attributeName : context.getAttributeNames()) {
                if (OSGiFramework.class.getName().equals(attributeName)) {
                    continue;
                }
                if (bootParameters == null) {
                    bootParameters = new HashMap(context.getAttributeNames().size());
                }
                bootParameters.put(attributeName, context.getAttribute(attributeName));
            }
            service.setBootParameters(bootParameters);
            service.setNewThread(true);
            service.init();
            service.start();
            available.acquire();
            System.out.println("Framework Started - OK");
            context.setAttribute(OSGiFramework.class.getName(), service);
        }
    }

    @AfterSuite
    public void stopOpenIDM(ITestContext context) throws Exception {
        Object service = context.getAttribute(OSGiFramework.class.getName());
        if (service instanceof OSGiFramework) {
            ((OSGiFramework) service).stop();
            ((OSGiFramework) service).destroy();
        }
    }

}
