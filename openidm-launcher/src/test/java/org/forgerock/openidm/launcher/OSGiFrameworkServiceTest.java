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
 * Copyright 2012-2017 ForgeRock AS.
 */

package org.forgerock.openidm.launcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import java.io.File;
import java.net.URLDecoder;
import java.util.concurrent.Semaphore;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class OSGiFrameworkServiceTest {

    private String[] arguments = null;
    private String testName;
    private OSGiFrameworkService service = null;
    private final Semaphore available = new Semaphore(1, true);

    @Factory(dataProvider = "provider")
    public OSGiFrameworkServiceTest(String testName, String[] args) {
        this.arguments = args;
        this.testName = testName;
    }

    @DataProvider
    public static Object[][] provider() throws Exception {
        String test1 =
                URLDecoder.decode(
                        new File(OSGiFrameworkServiceTest.class.getResource("/test1/").toURI())
                                .toString(), "utf-8");
        String test2 =
                URLDecoder.decode(
                        new File(OSGiFrameworkServiceTest.class.getResource("/test2/").toURI())
                                .toString(), "utf-8");

        return new Object[][] {
            new Object[] {
                    "test1", new String[]{"-p", test1, "-w", test1, "-c", "bin/launcher.json", "-s", "felix-cache"}},
            new Object[] {
                    "test2", new String[]{"-p", test2, "-w", test2, "-c", "bin/launcher.json"}}};
    }

    @BeforeClass
    // (timeOut = 5000)
    public void beforeClass() throws Exception {
        service = new OSGiFrameworkService();
        service.setInstallDir(URLDecoder.decode(new File(OSGiFrameworkServiceTest.class.getResource("/").toURI()
                .resolve("../osgi/")).toString(), "utf-8"));
        service.setFrameworkListener(new FrameworkListener() {
            public void frameworkEvent(FrameworkEvent event) {
                if (event.getType() == FrameworkEvent.STARTED) {
                    available.release();
                }
            }
        });
        available.drainPermits();
        service.init(arguments);
        service.setNewThread(true);
        service.start();

        available.acquire();
        System.out.println("Framework Started - OK");
    }

    @AfterClass
    public void afterClass() throws Exception {
        service.stop();
    }

    @Test
    @SuppressWarnings({ "deprecation" })
    public void testConfigurationAdmin() throws Exception {
        Assert.assertNotNull(service.getSystemBundle());
        ServiceTracker<PackageAdmin, PackageAdmin> tracker =
                new ServiceTracker<PackageAdmin, PackageAdmin>(service.getSystemBundle()
                        .getBundleContext(), PackageAdmin.class, null);
        tracker.open();
        Assert.assertNotNull(tracker.getService());
    }

    @Test
    public void testBundles() throws Exception {
        Assert.assertNotNull(service.getSystemBundle());
        Bundle[] installedBundles = service.getSystemBundle().getBundleContext().getBundles();

        if ("test1".equals(testName)) {
            Assert.assertEquals(4, installedBundles.length, "Only 4 bundles should be installed");
        }
        if ("test2".equals(testName)) {
            Assert.assertEquals(6, installedBundles.length, "Only 6 bundles should be installed");
        }
    }

    @Test
    public void parserTestOk() throws Exception {
        OSGiFrameworkService testable = new OSGiFrameworkService();
        CmdLineParser parser = new CmdLineParser(testable);

        String[] arguments =
                {"-p", "project-location", "-w", "working-location", "-c", "launcher.json", "-s", "storage-location",
                        "-P", "key1=value1", "-P", "key2=value2", "-t"};

        parser.parseArgument(arguments);
        Assert.assertEquals(testable.getProjectDir(), "project-location");
        Assert.assertEquals(testable.getWorkingDir(), "working-location");
        Assert.assertEquals(testable.getConfigFile(), "launcher.json");
        Assert.assertEquals(testable.getStorageDir(), "storage-location");
        Assert.assertEquals(testable.isNewThread(), true);
        assertThat(testable.getBootParameters())
                .hasSize(2)
                .contains(entry("key1", "value1"))
                .contains(entry("key2", "value2"));
    }

    @Test(expectedExceptions = CmdLineException.class)
    public void parserTestFail() throws Exception {
        OSGiFrameworkService testable = new OSGiFrameworkService();
        CmdLineParser parser = new CmdLineParser(testable);

        String[] arguments =
                {"-p", "project-location", "-w", "working-location", "-c", "launcher.json", "-s", "storage-location",
                        "-P", "key=value1", "-t", "must-fail"};

        parser.parseArgument(arguments);
        Assert.fail("Argument parser should fail");
    }
}
