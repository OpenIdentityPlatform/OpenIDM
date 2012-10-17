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

import java.io.File;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.forgerock.commons.launcher.Daemon;
import org.forgerock.commons.launcher.OSGiDaemonBean;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.testng.internal.ObjectFactoryImpl;

/**
 * A ITTestSetup does ...
 *
 * @author Laszlo Hordos
 */
public class ITTestSetup {

    @ObjectFactory
    public IObjectFactory createFactory(ITestContext context) throws Exception {
        startOpenIDM(context);
        return new ObjectFactoryImpl();
    }

    @BeforeSuite
    public void startOpenIDM(ITestContext context) throws Exception {

        OSGiDaemonBean service = (OSGiDaemonBean) context.getAttribute(Daemon.class.getName());
        if (null == service) {
            service = new OSGiDaemonBean();

            String testName = context.getCurrentXmlTest().getParameter("testName");
            if (null == testName) {
                testName = System.getProperty("testName", "scenario1");
            }

            String rootDir = URLDecoder.decode(new File(ITTestSetup.class.getResource("/" + testName).toURI())
                    .toString(), "utf-8");
            String configFile = "bin/launcher.json";
            String storageDir = "../../osgi/" + testName + "/cache";

            final Semaphore available = new Semaphore(1, true);
            available.drainPermits();
            service.setFrameworkListener(new FrameworkListener() {
                public void frameworkEvent(FrameworkEvent event) {
                    if (event.getType() == FrameworkEvent.STARTED) {
                        available.release();
                    }
                }
            });

            service.setRootDir(rootDir);
            service.setConfigFile(configFile);
            service.setStorageDir(storageDir);


            Map bootParameters = null;
            for (String attributeName : context.getAttributeNames()) {
                if (Daemon.class.getName().equals(attributeName)) {
                    continue;
                }
                if (bootParameters == null) {
                    bootParameters = new HashMap(context.getAttributeNames().size());
                }
                bootParameters.put(attributeName, context.getAttribute(attributeName));
            }
            service.setBootParameters(bootParameters);
            service.setDaemon(true);
            service.init();
            service.start();
            available.acquire();
            System.out.println("Framework Started - OK");
            context.setAttribute(Daemon.class.getName(), service);
        }
    }

    @AfterSuite
    public void stopOpenIDM(ITestContext context) throws Exception {
        Object service = context.getAttribute(Daemon.class.getName());
        if (service instanceof Daemon) {
            ((Daemon) service).stop();
            ((Daemon) service).destroy();
        }
    }

    @Test
    public void firstTest(ITestContext context) {
        Object service = context.getAttribute(Daemon.class.getName());
        Assert.assertNotNull(service);
    }

}
