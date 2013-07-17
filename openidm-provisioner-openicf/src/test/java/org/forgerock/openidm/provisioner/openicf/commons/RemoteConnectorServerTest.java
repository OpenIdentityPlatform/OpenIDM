//package org.forgerock.openidm.provisioner.openicf.commons;
//
//
//import org.identityconnectors.common.logging.Log;
//import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
//import org.identityconnectors.framework.common.exceptions.ConnectorException;
//import org.identityconnectors.framework.server.ConnectorServer;
//import org.testng.Assert;
//import org.testng.annotations.AfterTest;
//import org.testng.annotations.BeforeTest;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.ArrayList;
//import java.util.List;
//
//public class RemoteConnectorServerTest {
//    private static final String DEFAULT_LOG_SPI = "org.identityconnectors.common.logging.StdOutLogger";
//    private static final String DEFAULT_JDK_LOG_SPI = "org.identityconnectors.common.logging.impl.JDKLogger";
//
//    private static ConnectorServer _server;
//
//
//    //@BeforeTest
//    public void beforeTest() throws Exception {
//        URL libDir = RemoteConnectorServerTest.class.getResource("/connectorServer/lib/");
//        Assert.assertNotNull(libDir);
//        URL bundleDir = RemoteConnectorServerTest.class.getResource("/connectorServer/connectors/");
//        Assert.assertNotNull(bundleDir);
//        run(bundleDir.getPath(), libDir.getPath());
//    }
//
//    //@AfterTest
//    public void afterTest() {
//        stop();
//    }
//
//
//    private void run(String bundleDirStr, String libDirStr) throws Exception {
//        System.setProperty(Log.LOGSPI_PROP, DEFAULT_JDK_LOG_SPI);
//
//        // Work around issue 604. It seems that sometimes procrun will run
//        // the start method in a thread with a null context class loader.
//        if (Thread.currentThread().getContextClassLoader() == null) {
//            Thread.currentThread().setContextClassLoader(RemoteConnectorServerTest.class.getClassLoader());
//        }
//
//        _server = ConnectorServer.newInstance();
//        String openicfServerPort = System.getProperty("openicfServerPort");
//        int port =  8759;
//        if (openicfServerPort instanceof String) {
//            //port = Integer.parseInt(openicfServerPort);
//        }
//        _server.setPort(port);
//        _server.setBundleURLs(getJarFiles(new File(bundleDirStr)));
//        if (libDirStr != null) {
//            _server.setBundleParentClassLoader(buildLibClassLoader(new File(libDirStr)));
//        }
//        //Hash of "Passw0rd"
//        _server.setKeyHash("xOS4IeeE6eb/AhMbhxZEC37PgtE=");
//        _server.setUseSSL(false);
//        _server.setIfAddress(InetAddress.getByName("127.0.0.1"));
//        _server.start();
//        //_server.awaitStop();
//    }
//
//    public void stop() {
//        if (_server == null) {
//            // Procrun called stop() without calling main().
//            // Do not use a logging statement here to avoid initializing logging
//            // too early just because a bug in procrun.
//            System.err.println("Server has not been started yet");
//            return;
//        }
//
//        // Work around issue 604. It seems that sometimes procrun will run
//        // the start method in a thread with a null context class loader.
//        if (Thread.currentThread().getContextClassLoader() == null) {
//            Thread.currentThread().setContextClassLoader(RemoteConnectorServerTest.class.getClassLoader());
//        }
//
//        _server.stop();
//        // Do not set _server to null, because that way the check in run() fails
//        // and we ensure that the server cannot be started twice in the same JVM.
//    }
//
//    private ClassLoader buildLibClassLoader(File dir) throws MalformedURLException {
//        List<URL> jars = getJarFiles(dir);
//        if (!jars.isEmpty()) {
//            return new URLClassLoader(jars.toArray(new URL[jars.size()]), ConnectorInfoManagerFactory.class.getClassLoader());
//        }
//        return null;
//
//    }
//
//    private List<URL> getJarFiles(File dir) throws MalformedURLException {
//        if (!dir.isDirectory()) {
//            throw new ConnectorException(dir.getPath() + " does not exist");
//        }
//        List<URL> rv = new ArrayList<URL>();
//        for (File bundle : dir.listFiles()) {
//            if (bundle.getName().endsWith(".jar")) {
//                rv.add(bundle.toURI().toURL());
//            }
//        }
//        return rv;
//    }
//
//
//    //@Test
//    public void activatorTest1() {
////        URL root = RemoteConnectorServerTest.class.getResource("/connectors");
////        Assert.assertNotNull(root);
////        Map<String, Object> properties = new HashMap<String, Object>(1);
////        properties.put(ConnectorInfoProviderService.PROPERTY_OPENICF_CONNECTOR_URL, root);
////        ConnectorInfoProviderService newBuilder = new ConnectorInfoProviderService();
////        newBuilder.activate(properties);
//    }
//
//    //@Test
//    public void activatorTest() throws IOException {
////        URL connectorConfigurationURL = RemoteConnectorServerTest.class.getResource("/config/provisioner.openicf.connectorinfoprovider.json");
////        Assert.assertNotNull(connectorConfigurationURL);
////        ObjectMapper mapper = new ObjectMapper();
////        Map<String, Object> properties = mapper.readValue(connectorConfigurationURL, Map.class);
////
////
////        ConnectorInfoProviderService newBuilder = new ConnectorInfoProviderService();
////        newBuilder.activate(properties);
//    }
//}
