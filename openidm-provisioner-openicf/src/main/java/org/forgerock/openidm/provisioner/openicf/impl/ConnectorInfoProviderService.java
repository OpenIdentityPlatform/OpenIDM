package org.forgerock.openidm.provisioner.openicf.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.impl.api.ConnectorInfoManagerFactoryImpl;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * ConnectorInfoProviderService
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = "org.forgerock.openidm.provisioner.openicf.ConnectorInfoProviderService", policy = ConfigurationPolicy.OPTIONAL, description = "OpenICF Connector Info Service",immediate = true)
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

        // Create a single instance of ConnectorInfoManagerFactory
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();

        //Initialise Local ConnectorInfoManager
        String connectorsArea = context.getBundleContext().getProperty(BUNDLES_CONFIGURATION_LOCATION);
        Map<String, Object> configuration = getConfiguration(context.getProperties());
        String connectorLocation = DEFAULT_CONNECTORS_LOCATION;
        Object connectorURL = configuration.get(PROPERTY_ORG_FORGEROCK_OPENICF_CONNECTOR_URL);
        if (connectorURL instanceof String) {
            connectorLocation = connectorURL.toString();
        }

        // Only run the configuration changes if the connectorsArea is set.
        if (connectorsArea == null) {
            TRACE.info("System property [{}] is not defined.", BUNDLES_CONFIGURATION_LOCATION);
            TRACE.info("Using default connectors location [{}].", connectorLocation);
            connectorsArea = connectorLocation;
        } else {
            try {
                connectorsArea = new URI(connectorsArea).resolve(connectorLocation+"/").toString();
            } catch (URISyntaxException e) {
                TRACE.error("Invalid connectorsArea {}",connectorsArea,e);
            }
        }

        TRACE.info("Using connectors from [" + connectorsArea + "]");
        File dir = new File(connectorsArea);
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

        Object remoteConnectorHosts = configuration.get(ConnectorUtil.OPENICF_REMOTE_CONNECTOR_SERVERS);
        if (remoteConnectorHosts instanceof Collection) {
            for (Map<String, Object> info : (List<Map<String, Object>>) remoteConnectorHosts) {
                try {
                    RemoteFrameworkConnectionInfo rfi = ConnectorUtil.getRemoteFrameworkConnectionInfo(info);
                    String name = (String) info.get("name");
                    if (StringUtil.isNotBlank(name) && null != rfi) {
                        try {
                            remoteFrameworkConnectionInfo.put(name, rfi);
                            factory.getRemoteManager(rfi);
                        } catch (Exception e) {
                            TRACE.error("Remote ConnectorServer: {} initialization failed.",rfi,e);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        TRACE.info("Component is activated.");
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

    @Override
    public ConnectorInfo findConnectorInfo(ConnectorReference connectorReference) {
        ConnectorInfoManager connectorInfoManager = null;
        ConnectorInfo connectorInfo = null;
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        if (ConnectorReference.SINGLE_LOCAL_CONNECTOR_MANAGER.equals(connectorReference.getConnectorHost())) {
            connectorInfoManager = factory.getLocalManager(connectorURLs);
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


    @Override
    public List<ConnectorInfo> getConnectorInfo() {
        ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
        ConnectorInfoManager connectorInfoManager = factory.getLocalManager(connectorURLs);
        List<ConnectorInfo> result = new ArrayList<ConnectorInfo>(connectorInfoManager.getConnectorInfos());
        for (RemoteFrameworkConnectionInfo info : remoteFrameworkConnectionInfo.values()) {
            ConnectorInfoManager rcim = factory.getRemoteManager(info);
            result.addAll(rcim.getConnectorInfos());
        }
        return Collections.unmodifiableList(result);
    }

    private URL[] getConnectorURLs(URL... resourceURLs) {
        if (null == connectorURLs) {
            Set<URL> _bundleURLs = new HashSet<URL>(10);
            //URL[] resourceURLs = ClasspathUrlFinder.findResourceBases(BUNDLES_REL_PATH);
            for (int j = 0; j < resourceURLs.length; j++) {
                try {
                    //URL bundleDirUrl = new URL(resourceURLs[j], BUNDLES_REL_PATH);
                    URL bundleDirUrl = resourceURLs[j];
                    TRACE.info("Make sure the URL {} end with \"/\"", bundleDirUrl);
                    Vector<URL> urls = null;
                    if ("file".equals(bundleDirUrl.getProtocol())) {
                        File file = new File(bundleDirUrl.getFile());
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
                    } else if ("jar".equals(bundleDirUrl.getProtocol())) {
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

    private Map<String,Object> getConfiguration(Dictionary properties) {
        Object config = properties.get("jsonconfig");
        TRACE.debug("JSON Configuration is: {}",config);
        Map<String,Object> result = null;
        if (config instanceof String) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                result = mapper.readValue((String) config, Map.class);
            } catch (IOException e) {
                TRACE.error("JSON configuration can not be read.",e);
            }
        }
        if (null == result) {
            throw new ComponentException("Required JSON Configuration is missing.");
        }
        return result;
    }
}
