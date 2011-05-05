package org.forgerock.openidm.provisioner.openicf.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
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
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.ComponentContext;

import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * ConnectorInfoProviderService
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = "org.forgerock.openidm.provisioner.openicf.ConnectorInfoProviderService", policy = ConfigurationPolicy.OPTIONAL, description = "OpenICF Connector Info Service", immediate = true)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = "ForgeRock AS"),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenICF Connector Info Service"),
        @Property(name = ConnectorInfoProviderService.PROPERTY_ORG_FORGEROCK_OPENICF_CONNECTOR_URL, value = ConnectorInfoProviderService.DEFAULT_CONNECTORS_LOCATION)
})
public class ConnectorInfoProviderService implements ConnectorInfoProvider {
    private final static Logger TRACE = LoggerFactory.getLogger(ConnectorInfoProviderService.class);


    //Public Constants
    public static final String DEFAULT_CONNECTORS_LOCATION = "connectors";
    public static final String PROPERTY_ORG_FORGEROCK_OPENICF_CONNECTOR_URL = "org.forgerock.openicf.connectorURL";

    //Private
    private static final String BUNDLES_CONFIGURATION_LOCATION = "bundles.configuration.location";
    private Map<String, RemoteFrameworkConnectionInfo> remoteFrameworkConnectionInfo = new HashMap<String, RemoteFrameworkConnectionInfo>();
    private URL[] connectorURLs = null;


    @Activate
    protected void activate(ComponentContext context) {
        TRACE.trace("Activating Service with configuration {}", context.getProperties());
        JsonNode configuration = getConfiguration(context);

        // Create a single instance of ConnectorInfoManagerFactory
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        String connectorsArea = context.getBundleContext().getProperty(BUNDLES_CONFIGURATION_LOCATION);
        try {
            // String connectorLocation = DEFAULT_CONNECTORS_LOCATION;
            String connectorLocation = configuration.get(PROPERTY_ORG_FORGEROCK_OPENICF_CONNECTOR_URL).asString();

            if (StringUtil.isBlank(connectorLocation)) {
                connectorLocation = DEFAULT_CONNECTORS_LOCATION;
            }
            // Only run the configuration changes if the connectorsArea is set.
            if (connectorsArea == null) {
                TRACE.info("System property [{}] is not defined.", BUNDLES_CONFIGURATION_LOCATION);
                TRACE.info("Using default connectors location [{}].", connectorLocation);
                connectorsArea = connectorLocation;
            } else {
                try {
                    connectorsArea = new URI(connectorsArea).resolve(connectorLocation + "/").toString();
                } catch (URISyntaxException e) {
                    TRACE.error("Invalid connectorsArea {}", connectorsArea, e);
                }
            }
        } catch (JsonNodeException e) {
            TRACE.error("Invalid configuration {}", configuration.getValue(), e);
            throw new ComponentException("Invalid configuration, service can not be started", e);
        }

        // Initialise Local ConnectorInfoManager
        initialiseLocalManager(factory, connectorsArea);

        JsonNode remoteConnectorHosts = null;
        try {
            remoteConnectorHosts = configuration.get(ConnectorUtil.OPENICF_REMOTE_CONNECTOR_SERVERS).expect(List.class);
            if (!remoteConnectorHosts.isNull()) {
                initialiseRemoteManager(factory, remoteConnectorHosts);
            }
        } catch (JsonNodeException e) {
            TRACE.error("Invalid configuration remoteConnectorHosts must be list or null. {}", remoteConnectorHosts, e);
            throw new ComponentException("Invalid configuration, service can not be started", e);
        }
        TRACE.info("Component is activated.");
    }

    protected void initialiseRemoteManager(ConnectorInfoManagerFactory factory, JsonNode remoteConnectorHosts) throws JsonNodeException {
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
        if (null != connectorsArea) {
            try {
                String connectorsDir = URLDecoder.decode(connectorsArea, "UTF-8");
                TRACE.info("Using connectors from [" + connectorsDir + "]");
                File dir = new File(connectorsDir);
                if (!dir.exists()) {
                    String absolutePath = dir.getAbsolutePath();
                    TRACE.error("Configuration area [" + absolutePath + "] does not exist. Unable to load connectors.");
                } else {
                    try {
                        URL[] bundleUrls = getConnectorURLs(dir.getAbsoluteFile().toURI().toURL());
                        factory.getLocalManager(bundleUrls);
                    } catch (MalformedURLException e) {
                        TRACE.error("How can this happen?", e);
                    }

                }
            } catch (UnsupportedEncodingException e) {
                // Should never happen.
                throw new UndeclaredThrowableException(e);
            }
        } else {
            throw new ComponentException("connectors directory MUST be configured");
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
    @Override
    public ConnectorInfo findConnectorInfo(ConnectorReference connectorReference) {
        ConnectorInfoManager connectorInfoManager = null;
        ConnectorInfo connectorInfo = null;
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        if (ConnectorReference.SINGLE_LOCAL_CONNECTOR_MANAGER.equals(connectorReference.getConnectorHost())) {
            connectorInfoManager = factory.getLocalManager(getConnectorURLs());
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
    @Override
    public List<ConnectorInfo> getAllConnectorInfo() {
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        ConnectorInfoManager connectorInfoManager = factory.getLocalManager(getConnectorURLs());

        List<ConnectorInfo> result = new ArrayList<ConnectorInfo>(connectorInfoManager.getConnectorInfos());

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
    @Override
    public void testConnector(APIConfiguration configuration) {
        ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = connectorFacadeFactory.newInstance(configuration);
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
    @Override
    public Map<String, Object> createSystemConfiguration(APIConfiguration configuration, boolean validate) {
        ConnectorFacadeFactory connectorFacadeFactory = ConnectorFacadeFactory.getInstance();
        ConnectorFacade facade = connectorFacadeFactory.newInstance(configuration);
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

    private URL[] getConnectorURLs(URL... resourceURLs) {
        if (null == connectorURLs) {
            Set<URL> _bundleURLs = new HashSet<URL>();
            //URL[] resourceURLs = ClasspathUrlFinder.findResourceBases(BUNDLES_REL_PATH);
            for (int j = 0; j < resourceURLs.length; j++) {
                try {
                    //URL bundleDirUrl = new URL(resourceURLs[j], BUNDLES_REL_PATH);
                    URL bundleDirUrl = resourceURLs[j];

                    TRACE.info("Make sure the URL {} end with \"/\"", bundleDirUrl);
                    Vector<URL> urls = null;
                    if ("file".equals(bundleDirUrl.getProtocol())) {
                        File file = new File(bundleDirUrl.toURI());
                        if (file.isDirectory()) {
                            FileFilter filter = new FileFilter() {

                                @Override
                                public boolean accept(File f) {
                                    return (f.isDirectory()) || (f.getName().endsWith(".jar"));
                                }
                            };
                            File[] files = file.listFiles(filter);
                            urls = new Vector<URL>(files.length);
                            for (int i = 0; i < files.length; ++i) {
                                File subFile = files[i];
                                String fname = subFile.getName();
                                TRACE.info("Load Connector Bundle: {}", fname);
                                urls.add(new URL(bundleDirUrl, fname));
                            }
                        }
                    } else if (("jar".equals(bundleDirUrl.getProtocol())) || ("wsjar".equals(bundleDirUrl.getProtocol()))) {
                        urls = getJarFileListing(bundleDirUrl, "^META-INF/" + DEFAULT_CONNECTORS_LOCATION + "/(.*).jar$");

                    } else {
                        TRACE.info("Local connector support disabled.  No support for bundle URLs with protocol {}", bundleDirUrl.getProtocol());
                    }
                    if ((urls == null) || (urls.size() == 0)) {
                        TRACE.info("No local connector bundles found within {}", bundleDirUrl);
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
            connectorURLs = _bundleURLs.toArray(new URL[0]);
        }
        return connectorURLs;
    }

    /**
     * <p>Retrieve a list of filepaths from a given directory within a jar
     * file. If filtered results are needed, you can supply a |filter|
     * regular expression which will match each entry.
     *
     * @param filter to filter the results within a regular expression.
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

    private JsonNode getConfiguration(ComponentContext componentContext) {
        EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
        return new JsonNode(enhancedConfig.getConfiguration(componentContext));
    }
}
