package org.forgerock.openidm.provisioner.openicf.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.openicf.framework.api.osgi.ConnectorManager;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.api.operations.SchemaApiOp;
import org.identityconnectors.framework.api.operations.TestApiOp;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;
import org.identityconnectors.framework.impl.api.AbstractConnectorInfo;
import org.identityconnectors.framework.impl.api.remote.RemoteConnectorInfoImpl;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * ConnectorInfoProviderService
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = ConnectorInfoProviderService.PID, policy = ConfigurationPolicy.OPTIONAL, description = "OpenICF Connector Info Service", immediate = true)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenICF Connector Info Service")
})
public class ConnectorInfoProviderService implements ConnectorInfoProvider, MetaDataProvider {
    private final static Logger TRACE = LoggerFactory.getLogger(ConnectorInfoProviderService.class);


    //Public Constants
    public static final String DEFAULT_CONNECTORS_LOCATION = "connectors";
    public static final String PROPERTY_OPENICF_CONNECTOR_URL = "connectorsLocation";
    public static final String PID = "org.forgerock.openidm.provisioner.openicf.connectorinfoprovider";

    //Private
    private Map<String, RemoteFrameworkConnectionInfo> remoteFrameworkConnectionInfo = new HashMap<String, RemoteFrameworkConnectionInfo>();
    private URL[] connectorURLs = null;
    /*
     * If this instance was instantiated for MetaDataProvider by Class#newInstance then this is false.
     * If this instance was activated by OSGi SCR then this is true.
     */
    private boolean isOSGiServiceInstance = false;

    /**
     * ConnectorManager service.
     */
    @Reference(
            name = "ref_ConnectorManager_ConnectorInfoProvider",
            referenceInterface = ConnectorManager.class,
            bind = "bind",
            unbind = "unbind",
            cardinality = ReferenceCardinality.OPTIONAL_UNARY,
            policy = ReferencePolicy.STATIC)
    private ConnectorManager osgiConnectorManager = null;


    protected void bind(ConnectorManager service) {
        TRACE.info("ConnectorManager is bound.");
        this.osgiConnectorManager = service;

    }

    protected void unbind(ConnectorManager service) {
        this.osgiConnectorManager = null;
        TRACE.info("ConnectorManager is unbound.");
    }


    @Activate
    protected void activate(ComponentContext context) {
        TRACE.trace("Activating Service with configuration {}", context.getProperties());
        JsonValue configuration = getConfiguration(context);

        // Create a single instance of ConnectorInfoManagerFactory
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        try {
            // String connectorLocation = DEFAULT_CONNECTORS_LOCATION;
            String connectorLocation = configuration.get(PROPERTY_OPENICF_CONNECTOR_URL).defaultTo(DEFAULT_CONNECTORS_LOCATION).asString();
            // Initialise Local ConnectorInfoManager
            initialiseLocalManager(factory, connectorLocation);
        } catch (JsonValueException e) {
            TRACE.error("Invalid configuration {}", configuration.getValue(), e);
            throw new ComponentException("Invalid configuration, service can not be started", e);
        }


        JsonValue remoteConnectorHosts = null;
        try {
            remoteConnectorHosts = configuration.get(ConnectorUtil.OPENICF_REMOTE_CONNECTOR_SERVERS).expect(List.class);
            if (!remoteConnectorHosts.isNull()) {
                initialiseRemoteManager(factory, remoteConnectorHosts);
            }
        } catch (JsonValueException e) {
            TRACE.error("Invalid configuration remoteConnectorHosts must be list or null. {}", remoteConnectorHosts, e);
            throw new ComponentException("Invalid configuration, service can not be started", e);
        }
        isOSGiServiceInstance = true;
        TRACE.info("Component is activated.");
    }

    protected void initialiseRemoteManager(ConnectorInfoManagerFactory factory, JsonValue remoteConnectorHosts) throws JsonValueException {
        for (Object o : remoteConnectorHosts.asList()) {
            if (o instanceof Map) {
                Map<String, Object> info = (Map<String, Object>) o;
                try {
                    RemoteFrameworkConnectionInfo rfi = ConnectorUtil.getRemoteFrameworkConnectionInfo(info);
                    String name = (String) info.get("name");
                    if (StringUtil.isNotBlank(name) && null != rfi) {
                        try {
                            remoteFrameworkConnectionInfo.put(name, rfi);
                            factory.getRemoteManager(rfi);
                        } catch (Exception e) {
                            TRACE.error("Remote ConnectorServer: {} initialization failed.", rfi, e);
                        }
                    } else {
                        TRACE.error("RemoteFrameworkConnectionInfo has no name");
                    }
                } catch (IllegalArgumentException e) {
                    TRACE.error("RemoteFrameworkConnectionInfo can not be read", e);
                }
            }
        }
    }

    protected void initialiseLocalManager(ConnectorInfoManagerFactory factory, String connectorsArea) {
        try {
            String connectorsDir = URLDecoder.decode(connectorsArea, "UTF-8");
            TRACE.debug("Using connectors from [" + connectorsDir + "]");
            File dir = IdentityServer.getFileForPath(connectorsDir);
            //This is a fix to support absolute path on OSX
            if (!dir.exists()) {
                String absolutePath = dir.getAbsolutePath();
                if (!absolutePath.endsWith(File.separator)) {
                    dir = new File(absolutePath.concat(File.separator));
                }
            }

            if (!dir.exists()) {
                String absolutePath = dir.getAbsolutePath();
                TRACE.error("Configuration area [" + absolutePath + "] does not exist. Unable to load connectors.");
            } else {
                try {
                    TRACE.debug("Looking for connectors in {} directory.", dir.getAbsoluteFile().toURI().toURL());
                    URL[] bundleUrls = getConnectorURLs(dir.getAbsoluteFile().toURI().toURL());
                    factory.getLocalManager(bundleUrls);
                } catch (MalformedURLException e) {
                    TRACE.error("How can this happen?", e);
                }

            }
        } catch (UnsupportedEncodingException e) {
            // Should never happen.
            throw new UndeclaredThrowableException(e);
        } catch (Throwable t) {
            TRACE.error("LocalManager initialisation for {} failed.", connectorsArea, t);
            throw new ComponentException("LocalManager initialisation failed.", t);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        TRACE.trace("Deactivating Service {}", context.getProperties());
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        factory.clearLocalCache();
        connectorURLs = null;
        factory.clearRemoteCache();
        remoteFrameworkConnectionInfo.clear();
        TRACE.info("Component is deactivated.");
    }

//    @Modified
//    protected void update(ComponentContext context) {
//    }

    /**
     * {@inheritDoc}
     */
    public ConnectorInfo findConnectorInfo(ConnectorReference connectorReference) {
        ConnectorInfoManager connectorInfoManager = null;
        ConnectorInfo connectorInfo = null;
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        if (ConnectorReference.SINGLE_LOCAL_CONNECTOR_MANAGER.equals(connectorReference.getConnectorHost())) {
            connectorInfoManager = factory.getLocalManager(getConnectorURLs());
        } else if (ConnectorReference.OSGI_SERVICE_CONNECTOR_MANAGER.equals(connectorReference.getConnectorHost())) {
            connectorInfoManager = osgiConnectorManager;
        } else {
            RemoteFrameworkConnectionInfo rfci = remoteFrameworkConnectionInfo.get(connectorReference.getConnectorHost());
            if (null != rfci) {
                connectorInfoManager = factory.getRemoteManager(rfci);
            }
        }
        if (null != connectorInfoManager) {
            try {
                connectorInfo = connectorInfoManager.findConnectorInfo(connectorReference.getConnectorKey());
            } catch (Exception e) {
                TRACE.error("Can not find ConnectorInfo for {}", connectorReference, e);
            }
        }
        return connectorInfo;
    }

    /**
     * {@inheritDoc}
     */
    public List<ConnectorInfo> getAllConnectorInfo() {
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        ConnectorInfoManager connectorInfoManager = factory.getLocalManager(getConnectorURLs());

        List<ConnectorInfo> result = new ArrayList<ConnectorInfo>(connectorInfoManager.getConnectorInfos());

        if (null != osgiConnectorManager) {
            result.addAll(osgiConnectorManager.getConnectorInfos());
        }
        for (RemoteFrameworkConnectionInfo entry : remoteFrameworkConnectionInfo.values()) {
            try {
                ConnectorInfoManager remoteConnectorInfoManager = factory.getRemoteManager(entry);
                result.addAll(remoteConnectorInfoManager.getConnectorInfos());
            } catch (Exception e) {
                TRACE.error("Remote Connector Server is not available for {}", entry, e);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.identityconnectors.framework.common.exceptions.ConnectorException
     *          if OpenICF failed to create new
     *          connector facade
     */
    public void testConnector(APIConfiguration configuration) {
        ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = connectorFacadeFactory.newInstance(configuration);
        if (null == facade && null != osgiConnectorManager) {
            try {
                facade = osgiConnectorManager.newInstance(configuration);
            } catch (Exception e) {
                TRACE.warn("OSGi ConnectorManager can not create ConnectorFacade", e);
            }
        }
        if (null != facade) {
            TestApiOp operation = (TestApiOp) facade.getOperation(TestApiOp.class);
            if (null != operation) {
                operation.test();
            }
        }
        throw new UnsupportedOperationException("ConnectorFacade can not be initialised");
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> createSystemConfiguration(APIConfiguration configuration, boolean validate) {
        ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = connectorFacadeFactory.newInstance(configuration);
        if (null == facade && null != osgiConnectorManager) {
            try {
                facade = osgiConnectorManager.newInstance(configuration);
            } catch (Exception e) {
                TRACE.warn("OSGi ConnectorManager can not create ConnectorFacade", e);
            }
        }
        if (null != facade) {
            Map<String, Object> jsonConfiguration = new LinkedHashMap<String, Object>();
            APIConfigurationImpl impl = (APIConfigurationImpl) configuration;
            AbstractConnectorInfo connectorInfo = impl.getConnectorInfo();
            ConnectorReference connectorReference = null;
            if (connectorInfo instanceof RemoteConnectorInfoImpl) {
                RemoteConnectorInfoImpl remoteInfo = (RemoteConnectorInfoImpl) connectorInfo;
                for (Map.Entry<String, RemoteFrameworkConnectionInfo> entry : remoteFrameworkConnectionInfo.entrySet()) {
                    if (entry.getValue().equals(remoteInfo.getRemoteConnectionInfo())) {
                        connectorReference = new ConnectorReference(connectorInfo.getConnectorKey(), entry.getKey());
                        break;
                    }
                }

            } else {
                connectorReference = new ConnectorReference(connectorInfo.getConnectorKey());
            }
            ConnectorUtil.setConnectorReference(connectorReference, jsonConfiguration);
            ConnectorUtil.createSystemConfigurationFromAPIConfiguration(configuration, jsonConfiguration);
            if (validate && facade.getSupportedOperations().contains(TestApiOp.class)) {
                facade.test();
            }
            if (facade.getSupportedOperations().contains(SchemaApiOp.class)) {
                ConnectorUtil.setObjectAndOperationConfiguration(facade.schema(), jsonConfiguration);
            }
            return jsonConfiguration;
        }
        throw new UnsupportedOperationException("ConnectorFacade can not be initialised");
    }

    /**
     * {@inheritDoc}
     */
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias, JsonValue config) throws WaitForMetaData {
        List<JsonPointer> result = null;
        if (null != pidOrFactory && null != config) {
            if (PID.equals(pidOrFactory)) {
                try {
                    JsonValue remoteConnectorHosts = config.get(ConnectorUtil.OPENICF_REMOTE_CONNECTOR_SERVERS).expect(List.class);
                    if (!remoteConnectorHosts.isNull()) {
                        result = new ArrayList<JsonPointer>(remoteConnectorHosts.size());
                        for (JsonValue hostConfig : remoteConnectorHosts) {
                            result.add(hostConfig.get(ConnectorUtil.OPENICF_KEY).getPointer());
                        }
                    }
                } catch (JsonValueException e) {
                    TRACE.error("Invalid configuration remoteConnectorHosts must be list or null.", e);
                }
            } else if (OpenICFProvisionerService.PID.equals(pidOrFactory)) {
                if (isOSGiServiceInstance) {
                    ConfigurationProperties properties = null;
                    try {
                        ConnectorReference connectorReference = ConnectorUtil.getConnectorReference(config);
                        ConnectorInfo ci = findConnectorInfo(connectorReference);
                        if (null != ci) {
                            properties = ci.createDefaultAPIConfiguration().getConfigurationProperties();
                        }
                    } catch (Exception e) {
                        TRACE.error("Failed to parse the config of {}-{}", new Object[]{pidOrFactory, instanceAlias}, e);
                    }
                    if (null != properties) {
                        JsonPointer configurationProperties = new JsonPointer(ConnectorUtil.OPENICF_CONFIGURATION_PROPERTIES);
                        result = new ArrayList<JsonPointer>(properties.getPropertyNames().size());
                        for (String name : properties.getPropertyNames()) {
                            ConfigurationProperty property = properties.getProperty(name);
                            if (property.isConfidential()) {
                                result.add(configurationProperties.child(name));
                            }
                        }
                    } else {
                        throw new WaitForMetaData(pidOrFactory);
                    }
                } else {
                    throw new WaitForMetaData("Wait for the MetaDataProvider service instance");
                }
            }
        }
        return result;
    }

    private URL[] getConnectorURLs(URL... resourceURLs) {
        if (null == connectorURLs) {
            Set<URL> _bundleURLs = new HashSet<URL>();
            for (URL resourceURL : resourceURLs) {
                try {
                    Vector<URL> urls = null;
                    if ("file".equals(resourceURL.getProtocol())) {
                        File file = new File(resourceURL.toURI());
                        if (file.isDirectory()) {
                            FileFilter filter = new FileFilter() {

                                public boolean accept(File f) {
                                    return (f.isDirectory()) || (f.getName().endsWith(".jar"));
                                }
                            };
                            File[] files = file.listFiles(filter);
                            urls = new Vector<URL>(files.length);
                            for (File subFile : files) {
                                String fname = subFile.getName();
                                TRACE.trace("Load Connector Bundle: {}", fname);
                                urls.add(new URL(resourceURL, fname));
                            }
                        }
                    } else if (("jar".equals(resourceURL.getProtocol())) || ("wsjar".equals(resourceURL.getProtocol()))) {
                        urls = getJarFileListing(resourceURL, "^META-INF/" + DEFAULT_CONNECTORS_LOCATION + "/(.*).jar$");

                    } else {
                        TRACE.info("Local connector support disabled.  No support for bundle URLs with protocol {}", resourceURL.getProtocol());
                    }
                    if ((urls == null) || (urls.size() == 0)) {
                        TRACE.info("No local connector bundles found within {}", resourceURL);
                    }
                    if (null != urls) {
                        _bundleURLs.addAll(urls);
                    }
                } catch (IOException ex) {
                    //TODO Add Message
                    TRACE.error("XXX", ex);
                } catch (URISyntaxException e) {
                    TRACE.error("URL instance does not comply with RFC 2396", e);
                }
            }
            if (TRACE.isDebugEnabled()) {
                for (URL u : _bundleURLs) {
                    TRACE.debug("Connector URL: {}", u);
                }
            }
            connectorURLs = _bundleURLs.toArray(new URL[_bundleURLs.size()]);
        }
        return connectorURLs;
    }

    /**
     * <p>Retrieve a list of filepaths from a given directory within a jar
     * file. If filtered results are needed, you can supply a |filter|
     * regular expression which will match each entry.
     *
     * @param jarLocation
     * @param filter      to filter the results within a regular expression.
     * @return a list of files within the jar |file|
     */
    private static Vector<URL> getJarFileListing(URL jarLocation, String filter) {
        Vector<URL> files = new Vector<URL>();
        if (jarLocation == null) {
            return files; // Empty.
        }

        //strip out the file: and the !META-INF/bundles so only the JAR file left
        String jarPath = jarLocation.getPath().substring(5, jarLocation.getPath().indexOf("!"));

        try {
            // Lets stream the jar file
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarPath));
            JarEntry jarEntry;

            // Iterate the jar entries within that jar. Then make sure it follows the
            // filter given from the user.
            do {
                jarEntry = jarInputStream.getNextJarEntry();
                if (jarEntry != null) {
                    String fileName = jarEntry.getName();

                    // The filter could be null or has a matching regular expression.
                    if (filter == null || fileName.matches(filter)) {
                        files.add(new URL(jarLocation, fileName.replace(DEFAULT_CONNECTORS_LOCATION, "")));
                    }
                }
            } while (jarEntry != null);
            jarInputStream.close();
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to get Jar input stream from '" + jarLocation + "'", ioe);
        }
        return files;
    }

    private JsonValue getConfiguration(ComponentContext componentContext) {
        EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
        return new JsonValue(enhancedConfig.getConfiguration(componentContext));
    }
}
