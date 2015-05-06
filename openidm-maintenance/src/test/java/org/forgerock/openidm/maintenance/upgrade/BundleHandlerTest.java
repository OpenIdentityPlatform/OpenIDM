package org.forgerock.openidm.maintenance.upgrade;

import org.forgerock.commons.launcher.OSGiFrameworkService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.FrameworkWiring;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;


public class BundleHandlerTest {

    private String[] arguments;

    private BundleContext context;

    private OSGiFrameworkService service;

    private BundleHandler bundleHandler;

    // Path to the helloworld bundle jar
    private Path bundlePath;

    private Map<String, Bundle> installedBundles = new HashMap<String, Bundle>();

    private void setInstalledBundles(BundleContext context) {
        for (Bundle bundle : context.getBundles()) {
            installedBundles.put(bundle.getLocation(), bundle);
        }
    }

    private void updateInstalledBundles(BundleContext context) {
        for (Bundle bundle : context.getBundles()) {
            installedBundles.put(bundle.getLocation(), bundle);
        }
    }

    @Factory(dataProvider = "provider")
    public BundleHandlerTest( String[] args) {
        this.arguments = args;
    }

    @DataProvider
    public static Object[][] provider() throws Exception {
        String bundleHandlerDir = URLDecoder.decode(new File(BundleHandlerTest.class.getResource("/bundleHandler/")
                                                .toURI()).toString(), "utf-8");

        String install = URLDecoder.decode(new File(BundleHandlerTest.class.getResource("/")
                                        .toURI().resolve("../osgi/")).toString(), "utf-8");
        return new Object[][] {
            new Object[] {
                new String[] {
                    "-i", install, "-p", bundleHandlerDir, "-w", bundleHandlerDir, "-c", "bin/launcher.json",
                    "-s", "felix-cache"
                }
            }
        };
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        service = new OSGiFrameworkService();
        service.init(arguments);
        service.setNewThread(true);
        service.start();
        System.out.println("Framework Started - OK");
        assertNotNull(service.getSystemBundle());
        context = service.getSystemBundle().getBundleContext();
        setInstalledBundles(context);
        bundleHandler = new BundleHandler(context);
        bundlePath =  Paths.get(service.getProjectURI().toString(), "bundle/HelloWorld-1.0-SNAPSHOT.jar");
    }

    @AfterClass
    public void afterClass() throws Exception {
        service.stop();
    }

    private void stopBundle(Bundle bundle) throws Exception{
        bundleHandler.stopBundle(bundle);
        // wait 2 seconds for the framework to finish
        Thread.sleep(2000);
    }

    private void uninstallBundle(Bundle bundle) throws Exception {
        bundleHandler.uninstallBundle(bundle);
        updateInstalledBundles(context);
    }

    private Bundle installBundle(Path path) throws Exception {
        bundleHandler.installBundle(path);
        updateInstalledBundles(context);
        return installedBundles.get(path.toString());
    }

    private void startBundle(Bundle bundle) throws Exception {
        bundleHandler.startBundle(bundle);
        // Wait for bundle to start
        Thread.sleep(2000);
    }

    @Test
    public void testStopBundle() throws Exception {
        // install the bundle
        Bundle bundle = installBundle(bundlePath);

        // start the bundle
        startBundle(bundle);

        // Assert that the bundle is installed and running
        assertEquals(bundle.getState(), Bundle.ACTIVE);

        // stop the bundle
        stopBundle(bundle);

        // assert that bundle is in stopped state
        assertEquals(bundle.getState(), Bundle.RESOLVED);
    }

    @Test
    public void testUninstallBundle() throws Exception {

        // install bundle
        Bundle bundle = installBundle(bundlePath);
        // start bundle
        startBundle(bundle);

        // assert that the bundle is installed in the framework
        assertNotNull(bundle);

        // stop bundle
        stopBundle(bundle);

        // after uninstalling we want to refresh the packages to use this new bundle
        Set<Bundle> bundlesToRefresh = new HashSet<Bundle>();
        bundlesToRefresh.add(bundle);

        // uninstall bundle
        uninstallBundle(bundle);

        // refresh to show uninstalled bundle
        FrameworkWiring frameworkWiring = service.getSystemBundle().adapt(FrameworkWiring.class);
        frameworkWiring.refreshBundles(bundlesToRefresh);

        // assert that the bundle is in uninstalled state
        bundle = installedBundles.get(bundlePath.toString());
        assertEquals(bundle.getState(), Bundle.UNINSTALLED);

        // at the "system" level remove the bundle from the bundles/directory now
    }

    @Test
    public void testInstallBundle() throws Exception {
        // create bundle info for installing
        Bundle bundle = installedBundles.get(bundlePath.toString());

        // check that the bundle does not exist already in the framework
        assertNull(bundle);

        // install the bundle
        bundle = installBundle(bundlePath);

        // validate that the bundle has actually been installed
        assertNotNull(bundle);
        assertEquals(bundle.getState(), Bundle.INSTALLED);

    }

    @Test
    public void testStartBundle() throws Exception {
        // install bundle
        Bundle bundle = installBundle(bundlePath);

        // start the bundle
        startBundle(bundle);

        // assert that the bundle is now started in the Active state
        assertEquals(bundle.getState(), Bundle.ACTIVE);

    }
    @Test
    public void testUpdateBundle() throws Exception {

        Path bundlePathV2  = Paths.get(service.getProjectURI().toString(), "bundle/HelloWorld-2.0.jar");

        Bundle bundle2 = installedBundles.get(bundlePathV2.toString());

        // check that the bundle does not exist already in the framework
        assertNull(bundle2);

        // install the bundle
        bundle2 = installBundle(bundlePathV2);

        // start new bundle
        startBundle(bundle2);

        // update bundle
        bundleHandler.updateBundle(bundle2);

        // refresh all bundles that would need the new import from bundle2
        FrameworkWiring frameworkWiring = service.getSystemBundle().adapt(FrameworkWiring.class);
        frameworkWiring.refreshBundles(null);

    }
}