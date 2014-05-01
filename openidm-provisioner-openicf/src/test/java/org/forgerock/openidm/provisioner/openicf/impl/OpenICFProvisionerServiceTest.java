package org.forgerock.openidm.provisioner.openicf.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.Route;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.PropertyAccessor;
import org.forgerock.openidm.provisioner.openicf.connector.TestConfiguration;
import org.forgerock.openidm.provisioner.openicf.connector.TestConnector;
import org.forgerock.openidm.provisioner.openicf.internal.SystemAction;
import org.forgerock.openidm.provisioner.openicf.syncfailure.NullSyncFailureHandler;
import org.forgerock.openidm.provisioner.openicf.syncfailure.SyncFailureHandler;
import org.forgerock.openidm.provisioner.openicf.syncfailure.SyncFailureHandlerFactory;
import org.forgerock.openidm.router.RouteBuilder;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.openidm.router.RouterRegistry;
import org.forgerock.openidm.util.FileUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.logging.impl.NoOpLogger;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;
import org.identityconnectors.framework.impl.api.AbstractConnectorInfo;
import org.identityconnectors.framework.impl.api.ConfigurationPropertiesImpl;
import org.identityconnectors.framework.impl.api.local.ConnectorPoolManager;
import org.identityconnectors.framework.impl.api.local.JavaClassProperties;
import org.identityconnectors.framework.impl.api.local.LocalConnectorFacadeImpl;
import org.identityconnectors.framework.impl.api.local.LocalConnectorInfoImpl;
import org.identityconnectors.framework.impl.test.TestHelpersImpl;
import org.identityconnectors.framework.server.ConnectorServer;
import org.identityconnectors.framework.server.impl.ConnectorServerImpl;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class OpenICFProvisionerServiceTest extends ConnectorFacadeFactory implements
        RouterRegistry, ConnectorInfoManager, SyncFailureHandlerFactory {

    public static final String LAUNCHER_INSTALL_LOCATION = "launcher.install.location";
    public static final String LAUNCHER_INSTALL_URL = "launcher.install.url";
    public static final String LAUNCHER_WORKING_LOCATION = "launcher.working.location";
    public static final String LAUNCHER_WORKING_URL = "launcher.working.url";
    public static final String LAUNCHER_PROJECT_LOCATION = "launcher.project.location";
    public static final String LAUNCHER_PROJECT_URL = "launcher.project.url";

    /* @formatter:off */
    private static final String CONFIGURATION_TEMPLATE =
            "{\n" +
                    "    \"connectorsLocation\" : \"connectors\",\n" +
                    "    \"remoteConnectorServers\" : [\n" +
                    "        {\n" +
                    "            \"name\"          : \"testServer\",\n" +
                    "            \"host\"          : \"127.0.0.1\",\n" +
                    "            \"_port\"         : \"${openicfServerPort}\",\n" +
                    "            \"port\"          : 8759,\n" +
                    "            \"useSSL\"        : false,\n" +
                    "            \"timeout\"       : 0,\n" +
                    "            \"key\"           : \"Passw0rd\",\n" +
                    "            \"heartbeatInterval\" : 5\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";
    /* @formatter:on */

    /**
     * Setup logging for the {@link OpenICFProvisionerServiceTest}.
     */
    private final static Logger logger = LoggerFactory
            .getLogger(OpenICFProvisionerServiceTest.class);

    private Connection connection = null;

    private ConnectorServer connectorServer = null;

    private Pair<ConnectorInfoProviderService, ComponentContext> provider = null;

    private final List<Pair<OpenICFProvisionerService, ComponentContext>> systems =
            new ArrayList<Pair<OpenICFProvisionerService, ComponentContext>>();

    protected final Router router = new Router();

    public OpenICFProvisionerServiceTest() {
        try {
            IdentityServer.initInstance(new PropertyAccessor() {
                @Override
                public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
                    if (String.class.isAssignableFrom(expected)) {
                        try {
                            if (LAUNCHER_INSTALL_LOCATION.equals(key)
                                    || LAUNCHER_PROJECT_LOCATION.equals(key)
                                    || LAUNCHER_WORKING_LOCATION.equals(key)) {
                                return (T) URLDecoder.decode(OpenICFProvisionerServiceTest.class
                                        .getResource("/").getPath(), "utf-8");
                            } else if (LAUNCHER_INSTALL_URL.equals(key)
                                    || LAUNCHER_PROJECT_URL.equals(key)
                                    || LAUNCHER_WORKING_URL.equals(key)) {
                                return (T) OpenICFProvisionerServiceTest.class.getResource("/")
                                        .toString();
                            }
                        } catch (UnsupportedEncodingException e) {
                            /* ignore */
                        }
                    }
                    return null;
                }
            });
        } catch (IllegalStateException e) {
            /* ignore */
        }
    }

    // ----- Implementation of RouterRegistry interface

    @Override
    public RouteEntry addRoute(RouteBuilder routeBuilder) {

        final Route[] routes = routeBuilder.register(router);

        return new RouteEntry() {
            @Override
            public boolean removeRoute() {
                return router.removeRoute(routes);
            }

            @Override
            public ServerContext createServerContext() throws ResourceException {
                return createServerContext(new RootContext());
            }

            @Override
            public ServerContext createServerContext(Context parentContext)
                    throws ResourceException {
                return new ServerContext(parentContext);
            }
        };
    }

    @Override
    public Connection getConnection(String connectionId) throws ResourceException {
        return Resources.newInternalConnection(router);
    }

    @Override
    public String getConnectionId(Connection connection) throws ResourceException {
        return "DEFAULT";
    }

    // ----- Implementation of ConnectorInfoManager interface

    @Override
    public List<ConnectorInfo> getConnectorInfos() {
        return null;
    }

    @Override
    public ConnectorInfo findConnectorInfo(ConnectorKey connectorKey) {
        LocalConnectorInfoImpl info = new LocalConnectorInfoImpl();
        info.setConnectorConfigurationClass(TestConfiguration.class);
        info.setConnectorClass(TestConnector.class);
        info.setConnectorDisplayNameKey("DUMMY_DISPLAY_NAME");
        info.setConnectorKey(connectorKey);
        info.setMessages(new TestHelpersImpl().createDummyMessages());

        APIConfigurationImpl rv = new APIConfigurationImpl();
        rv.setConnectorPoolingSupported(PoolableConnector.class
                .isAssignableFrom(TestConnector.class));
        ConfigurationPropertiesImpl properties =
                JavaClassProperties.createConfigurationProperties(new TestConfiguration());
        rv.setConfigurationProperties(properties);
        rv.setConnectorInfo(info);
        rv.getResultsHandlerConfiguration().setEnableAttributesToGetSearchResultsHandler(false);
        rv.getResultsHandlerConfiguration().setEnableFilteredResultsHandler(false);
        rv.setSupportedOperations(FrameworkUtil.getDefaultSupportedOperations(TestConnector.class));
        info.setDefaultAPIConfiguration(rv);
        return info;
    }

    // ----- Implementation of ConnectorFacadeFactory interface

    @Override
    public void dispose() {
        ConnectorPoolManager.dispose();
    }

    @Override
    public ConnectorFacade newInstance(APIConfiguration configuration) {
        ConnectorFacade ret = null;
        APIConfigurationImpl impl = (APIConfigurationImpl) configuration;
        AbstractConnectorInfo connectorInfo = impl.getConnectorInfo();
        if (connectorInfo instanceof LocalConnectorInfoImpl) {
            LocalConnectorInfoImpl localInfo = (LocalConnectorInfoImpl) connectorInfo;
            try {
                ret = new LocalConnectorFacadeImpl(localInfo, impl);
            } catch (Exception ex) {
                logger.error("Failed to create new connector facade: {}, {}", impl
                        .getConnectorInfo().getConnectorKey(), configuration, ex);
                throw ConnectorException.wrap(ex);
            }
        } else {
            throw new ConnectorException("RemoteConnector not supported!");
        }
        return ret;
    }

    // this implementation not used
    @Override
    public ConnectorFacade newInstance(ConnectorInfo info, String configuration) {
        return null;
    }

    @DataProvider(name = "dp")
    public Iterator<Object[]> createData() throws Exception {
        List<Object[]> tests = new ArrayList<Object[]>();
        for (Pair<OpenICFProvisionerService, ComponentContext> pair : systems) {
            tests.add(new Object[] { pair.getLeft().getSystemIdentifierName() });
        }
        return tests.iterator();
    }

    @BeforeClass
    public void setUp() throws Exception {
        // Start OpenICF Connector Server
        String openicfServerPort =
                IdentityServer.getInstance().getProperty("openicfServerPort", "8759");
        int port = 8759;// Integer.getInteger(openicfServerPort);
        System.setProperty(Log.LOGSPI_PROP, NoOpLogger.class.getName());

        connectorServer = new ConnectorServerImpl();
        connectorServer.setPort(port);

        File root = new File(OpenICFProvisionerService.class.getResource("/").toURI());

        List<URL> bundleURLs = new ArrayList<URL>();

        File[] connectors = (new File(root, "/connectors/")).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        Assert.assertNotNull(connectors, "You must copy the connectors first");

        for (File connector : connectors) {
            bundleURLs.add(connector.toURI().toURL());
        }

        Assert.assertFalse(bundleURLs.isEmpty(), "No Connectors were found!");
        connectorServer.setBundleURLs(bundleURLs);
        connectorServer.setKeyHash("xOS4IeeE6eb/AhMbhxZEC37PgtE=");
        connectorServer.setIfAddress(InetAddress.getByName("127.0.0.1"));
        connectorServer.start();

        // Start ConnectorInfoProvider Service
        Dictionary<String, Object> properties = new Hashtable<String, Object>(3);
        properties.put(JSONEnhancedConfig.JSON_CONFIG_PROPERTY, CONFIGURATION_TEMPLATE);
        // mocking
        ComponentContext context = mock(ComponentContext.class);
        // stubbing
        when(context.getProperties()).thenReturn(properties);

        provider = Pair.of(new ConnectorInfoProviderService(), context);
        provider.getLeft().bindConnectorFacadeFactory(this);
        provider.getLeft().bindConnectorInfoManager(this);
        provider.getLeft().activate(context);

        File[] configJsons = (new File(root, "/config/")).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("provisioner.openicf-");
            }
        });

        Assert.assertNotNull(configJsons, "You must copy the configurations first");

        for (File configJson : configJsons) {
            // Start OpenICFProvisionerService Service
            properties = new Hashtable<String, Object>(3);
            // properties.put(ComponentConstants.COMPONENT_ID, 42);
            // properties.put(ComponentConstants.COMPONENT_NAME,
            // getClass().getCanonicalName());
            properties.put(JSONEnhancedConfig.JSON_CONFIG_PROPERTY, FileUtil.readFile(configJson));

            context = mock(ComponentContext.class);
            // stubbing
            when(context.getProperties()).thenReturn(properties);

            OpenICFProvisionerService service = new OpenICFProvisionerService();

            service.bindConnectorInfoProvider(provider.getLeft());
            service.bindRouterRegistry(this);
            service.bindSyncFailureHandlerFactory(this);
            service.activate(context);

            systems.add(Pair.of(service, context));
        }

        connection = Resources.newInternalConnection(router);
    }

    @AfterClass
    public void tearDown() throws Exception {
        for (Pair<OpenICFProvisionerService, ComponentContext> pair : systems) {
            pair.getLeft().deactivate(pair.getRight());
        }
        provider.getLeft().deactivate(provider.getRight());
        connectorServer.stop();
    }

    @Test(dataProvider = "dp")
    public void testReadInstance(String systemName) throws Exception {

    }

    @Test(dataProvider = "dp")
    public void testActionInstance(String systemName) throws Exception {

    }

    @Test(dataProvider = "dp")
    public void testPatchInstance(String systemName) throws Exception {

    }

    @Test(dataProvider = "dp")
    public void testUpdateInstance(String systemName) throws Exception {

    }


    private JsonValue getTestConnectorObject(String name) {
        JsonValue createAttributes = new JsonValue(new LinkedHashMap<String, Object>());
        createAttributes.put(Name.NAME, name);
        createAttributes.put("userName", name);
        createAttributes.put("email", name + "@example.com");
        return createAttributes;
    }

    @Test(dataProvider = "dp", enabled = true)
    public void testPagedSearch(String systemName) throws Exception {
        if ("groovyremote".equals(systemName) || "groovy".equals(systemName)) {

            for (int i = 0; i < 100; i++) {
                JsonValue co = getTestConnectorObject(String.format("TEST%05d", i));
                co.put("sortKey", i);

                CreateRequest request = Requests.newCreateRequest("system/" + systemName + "/account", co);
                connection.create(new RootContext(), request);
            }

            QueryRequest queryRequest = Requests.newQueryRequest("system/" + systemName + "/account");
            queryRequest.setPageSize(10);
            queryRequest.addSortKey(SortKey.descendingOrder("sortKey"));
            queryRequest.setQueryFilter(QueryFilter.startsWith("__NAME__", "TEST"));

            QueryResult result = null;

            final Set<Resource> resultSet = new HashSet<Resource>();
            int pageIndex = 0;

            while ((result = connection.query(new RootContext(), queryRequest, new QueryResultHandler() {

                private int index = 101;

                public void handleError(ResourceException error) {
                    Assert.fail(error.getMessage(), error);
                }

                public boolean handleResource(Resource resource) {
                    Integer idx = resource.getContent().get("sortKey").asInteger();
                    Assert.assertTrue(idx < index);
                    index = idx;
                    return resultSet.add(resource);
                }

                public void handleResult(QueryResult result) {

                }
            })).getPagedResultsCookie() != null) {

                queryRequest.setPagedResultsCookie(result.getPagedResultsCookie());
                Assert.assertEquals(resultSet.size(), 10 * ++pageIndex);
            }
            Assert.assertEquals(pageIndex, 9);
            Assert.assertEquals(resultSet.size(), 100);
        }
    }

    @Test(dataProvider = "dp", enabled = false)
    public void testHelloWorldAction(String systemName) throws Exception {
        if ("Test".equals(systemName)) {

            //Request#1
            ActionRequest actionRequest = Requests.newActionRequest("/system/Test", "script");
            actionRequest.setAdditionalParameter(SystemAction.SCRIPT_ID, "ConnectorScript#1");

            JsonValue result = connection.action(new RootContext(), actionRequest);
            assertThat(result.get(new JsonPointer("actions/0/result")).getObject()).isEqualTo(
                    "Arthur Dent");

            //Request#2
            actionRequest = Requests.newActionRequest("/system/Test", "script");
            actionRequest.setAdditionalParameter(SystemAction.SCRIPT_ID, "ConnectorScript#2");
            JsonValue content = new JsonValue(new HashMap<String, Object>());
            content.put("testArgument", "Zaphod Beeblebrox");
            actionRequest.setContent(content);

            result = connection.action(new RootContext(), actionRequest);
            assertThat(result.get(new JsonPointer("actions/0/result")).getObject()).isEqualTo(
                    "Zaphod Beeblebrox");

            //Request#3
            actionRequest = Requests.newActionRequest("/system/Test", "script");
            actionRequest.setAdditionalParameter(SystemAction.SCRIPT_ID, "ConnectorScript#3");
            content = new JsonValue(new HashMap<String, Object>());
            content.put("testArgument", Arrays.asList("Ford Prefect", "Tricia McMillan"));
            actionRequest.setContent(content);

            result = connection.action(new RootContext(), actionRequest);
            assertThat(result.get(new JsonPointer("actions/0/result")).getObject()).isEqualTo(2);


            //Request#4
            actionRequest = Requests.newActionRequest("/system/Test", "script");
            actionRequest.setAdditionalParameter(SystemAction.SCRIPT_ID, "ConnectorScript#4");
            result = connection.action(new RootContext(), actionRequest);
            assertThat(result.get(new JsonPointer("actions/0/error")).getObject()).isEqualTo(
                    "Marvin");
        }
    }

    @Override
    public SyncFailureHandler create(JsonValue config) throws Exception {
        return NullSyncFailureHandler.INSTANCE;
    }
}
