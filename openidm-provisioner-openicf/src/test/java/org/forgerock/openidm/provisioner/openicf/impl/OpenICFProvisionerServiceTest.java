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
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.PreconditionRequiredException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.Route;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.PropertyAccessor;
import org.forgerock.openidm.provisioner.impl.SystemObjectSetService;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
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
import org.identityconnectors.framework.common.objects.SyncToken;
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
            router.addRoute("repo/synchronisation/pooledSyncStage", new MemoryBackend());
            router.addRoute("audit/activity", new MemoryBackend());
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

    @DataProvider(name = "groovy-only")
    public Object[][] createGroovyData() throws Exception {
           return new Object[][]{
                   {"groovy"},
                   {"groovyremote"}
           };
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
            service.bindConnectionFactory(Resources.newInternalConnectionFactory(router));

            // Attempt to activate the provisioner service up to 4 times, using ConnectorFacade#test to
            // validate proper initializationn.  If the connector info manager is not be initialized, the
            // test fails because the connector cannot connect to the remote server.  In this test, it 
            // manifests as a timing issue owing to the flexibility in the provisioner service and the 
            // connector info provider supporting the ability for the connector server to come and go, as
            // managed by the health check thread (see ConnectorInfoProviderService#initialiseRemoteManager).
            // The test simply executes too fast for the health check thread to complete setup of the
            // connector info manager.
            for (int count = 0; count < 4; count++)  {
                service.activate(context);
                try {
                    service.getConnectorFacade().test();
                    break;
                } catch (Exception e) {
                    Thread.sleep(1000);
                }
            }

            systems.add(Pair.of(service, context));
        }
        // bind SystemObjectSetService dependencies in closure as the bind methods
        // are protected
        SystemObjectSetService systemObjectSetService =
                new SystemObjectSetService() {{
                    bindConnectionFactory(Resources.newInternalConnectionFactory(router));
                    for (Pair<OpenICFProvisionerService, ComponentContext> pair : systems) {
                        bindProvisionerService(pair.getLeft(),(Map) null);
                    }
                }};

        router.addRoute("system", systemObjectSetService);

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
        createAttributes.put("attributeString", name);
        createAttributes.put("attributeLong", (long) name.hashCode());
        return createAttributes;
    }

    private JsonValue getAccountObject(String name) {
        JsonValue createAttributes = new JsonValue(new LinkedHashMap<String, Object>());
        createAttributes.put(Name.NAME, name);
        createAttributes.put("userName", name);
        createAttributes.put("email", name + "@example.com");
        return createAttributes;
    }

    private static class SyncStub implements SingletonResourceProvider {

        final public ArrayList<ActionRequest> requests = new ArrayList<ActionRequest>();

        public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
            requests.add(request);
            handler.handleResult(new JsonValue(true));
        }

        public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
            handler.handleError(new NotSupportedException());
        }

        public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
            handler.handleError(new NotSupportedException());
        }

        public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
            handler.handleError(new NotSupportedException());
        }
    }

    @Test(dataProvider = "groovy-only", enabled = true)
    public void testSync(String systemName) throws Exception {
            JsonValue stage = new JsonValue(new LinkedHashMap<String, Object>());
            stage.put("connectorData", ConnectorUtil.convertFromSyncToken(new SyncToken(0)));
            CreateRequest createRequest = Requests
                    .newCreateRequest("repo/synchronisation/pooledSyncStage",
                            ("system" + systemName + "account").toUpperCase(),
                            stage);
            connection.create(new RootContext(), createRequest);

            SyncStub sync = new SyncStub();
            Route r = router.addRoute("sync", sync);


            ActionRequest actionRequest = Requests.newActionRequest("system/" + systemName + "/account",
                    SystemObjectSetService.SystemAction.liveSync.toString());

            stage = connection.action(new RootContext(), actionRequest);
            Assert.assertEquals(ConnectorUtil.convertToSyncToken(stage.get("connectorData")).getValue(), 1);
            Assert.assertEquals(sync.requests.size(), 1);
            ActionRequest delta = sync.requests.remove(0);
            Assert.assertEquals(delta.getAction(), "notifyCreate");


            stage = connection.action(new RootContext(), actionRequest);
            Assert.assertEquals(ConnectorUtil.convertToSyncToken(stage.get("connectorData")).getValue(), 2);
            Assert.assertEquals(sync.requests.size(), 1);
            delta = sync.requests.remove(0);
            Assert.assertEquals(delta.getAction(), "notifyUpdate");


            stage = connection.action(new RootContext(), actionRequest);
            Assert.assertEquals(ConnectorUtil.convertToSyncToken(stage.get("connectorData")).getValue(), 3);
            Assert.assertEquals(sync.requests.size(), 1);
            delta = sync.requests.remove(0);
            Assert.assertEquals(delta.getAction(), "notifyUpdate");


            stage = connection.action(new RootContext(), actionRequest);
            Assert.assertEquals(ConnectorUtil.convertToSyncToken(stage.get("connectorData")).getValue(), 4);
            Assert.assertEquals(sync.requests.size(), 1);
            delta = sync.requests.remove(0);
            Assert.assertEquals(delta.getAction(), "notifyUpdate");
            Assert.assertEquals(delta.getContent().get("newValue").get("_previous-id").asString(), "001");


            stage = connection.action(new RootContext(), actionRequest);
            Assert.assertEquals(ConnectorUtil.convertToSyncToken(stage.get("connectorData")).getValue(), 5);
            Assert.assertEquals(sync.requests.size(), 1);
            delta = sync.requests.remove(0);
            Assert.assertEquals(delta.getAction(), "notifyDelete");


            stage = connection.action(new RootContext(), actionRequest);
            Assert.assertEquals(ConnectorUtil.convertToSyncToken(stage.get("connectorData")).getValue(), 10);
            Assert.assertTrue(sync.requests.isEmpty());


            stage = connection.action(new RootContext(), actionRequest);
            Assert.assertEquals(ConnectorUtil.convertToSyncToken(stage.get("connectorData")).getValue(), 17);
            Assert.assertEquals(sync.requests.size(), 4);
            sync.requests.clear();


            stage = new JsonValue(new LinkedHashMap<String, Object>());
            stage.put("connectorData", ConnectorUtil.convertFromSyncToken(new SyncToken(10)));
            createRequest = Requests
                    .newCreateRequest("repo/synchronisation/pooledSyncStage",
                            ("system" + systemName + "group").toUpperCase(),
                            stage);
            connection.create(new RootContext(), createRequest);
            actionRequest = Requests.newActionRequest("system/" + systemName + "/group",
                    SystemObjectSetService.SystemAction.liveSync.toString());

            stage = connection.action(new RootContext(), actionRequest);
            Assert.assertEquals(ConnectorUtil.convertToSyncToken(stage.get("connectorData")).getValue(), 16);
            Assert.assertEquals(sync.requests.size(), 3);

            router.removeRoute(r);
    }


    @Test(dataProvider = "groovy-only", enabled = true)
    public void testPagedSearch(String systemName) throws Exception {

        for (int i = 0; i < 100; i++) {
            JsonValue co = getAccountObject(String.format("TEST%05d", i));
            co.put("sortKey", i);

            CreateRequest request = Requests.newCreateRequest("system/" + systemName + "/account", co);
            connection.create(new SecurityContext(new RootContext(), "system", null ), request);
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

    @Test(dataProvider = "dp", enabled = true)
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

    // AlreadyExistsException -> ConflictException
    @Test(dataProvider = "groovy-only", expectedExceptions = ConflictException.class, enabled = true)
    public void testConflictException(String systemName) throws Exception {
        CreateRequest createRequest = Requests.newCreateRequest("system/" + systemName + "/__TEST__", getTestConnectorObject("TEST1"));
        connection.create(new SecurityContext(new RootContext(), "system", null ), createRequest);
    }

    // ConnectorIOException -> ServiceUnavailableException - Will work when new groovy script is updated
    @Test(dataProvider = "groovy-only", expectedExceptions = ServiceUnavailableException.class , enabled = true)
    public void testServiceUnavailableExceptionFromConnectorIOException(String systemName) throws Exception {
        DeleteRequest deleteRequest = Requests.newDeleteRequest("system/" + systemName + "/__TEST__/TESTEX_CIO");
        connection.delete((new SecurityContext(new RootContext(), "system", null)), deleteRequest);
    }

    // OperationTimeoutException -> ServiceUnavailableException
    @Test(dataProvider = "groovy-only", expectedExceptions = ServiceUnavailableException.class , enabled = true)
    public void testServiceUnavailableExceptionFromOperationTimeoutException(String systemName) throws Exception {
        DeleteRequest deleteRequest = Requests.newDeleteRequest("system/" + systemName + "/__TEST__/TESTEX_OT");
        connection.delete((new SecurityContext(new RootContext(), "system", null )), deleteRequest);

    }

    // RetryableException -> ServiceUnavailableException
    @Test(dataProvider = "groovy-only", expectedExceptions = ServiceUnavailableException.class , enabled = true)
    public void testServiceUnavailableExceptionFromRetryableException(String systemName) throws Exception {
        CreateRequest createRequest = Requests.newCreateRequest("system/" + systemName + "/__TEST__", getTestConnectorObject("TEST4"));
        connection.create(new SecurityContext(new RootContext(), "system", null), createRequest);
    }

    // ConfigurationException -> InternalServerErrorException
    @Test(dataProvider = "groovy-only", expectedExceptions = InternalServerErrorException.class, enabled = true)
    public void testInternalServerErrorExceptionFromConfigurationException(String systemName) throws Exception {
        DeleteRequest deleteRequest = Requests.newDeleteRequest("system/" + systemName + "/__TEST__/TESTEX_CE");
        connection.delete(new RootContext(), deleteRequest);
    }

    // ConnectionBrokenException -> ServiceUnavailableException
    @Test(dataProvider = "groovy-only", expectedExceptions = ServiceUnavailableException.class, enabled = true)
    public void testServiceUnavailableExceptionFromConnectionBrokenException(String systemName) throws Exception {
        DeleteRequest deleteRequest = Requests.newDeleteRequest("system/" + systemName + "/__TEST__/TESTEX_CB");
        connection.delete(new RootContext(), deleteRequest);
    }

    // ConnectionFailedException -> ServiceUnavailableException
    @Test(dataProvider = "groovy-only", expectedExceptions = ServiceUnavailableException.class, enabled = true)
    public void testServiceUnavailableExceptionFromConnectionFailedException(String systemName) throws Exception {
        DeleteRequest deleteRequest = Requests.newDeleteRequest("system/" + systemName + "/__TEST__/TESTEX_CF");
        connection.delete(new RootContext(), deleteRequest);
    }

    // ConnectorException -> InternalServerErrorException
    @Test(dataProvider = "groovy-only", expectedExceptions = InternalServerErrorException.class, enabled = true)
    public void testInternalServerErrorExceptionFromConnectorException(String systemName) throws Exception {
        DeleteRequest deleteRequest = Requests.newDeleteRequest("system/" + systemName + "/__TEST__/TESTEX_C");
        connection.delete(new RootContext(), deleteRequest);
    }

    // NullPointerException -> InternalServerErrorException
    @Test(dataProvider = "groovy-only", expectedExceptions = InternalServerErrorException.class, enabled = true)
    public void testInternalServerErrorExceptionFromNullPointerException(String systemName) throws Exception {
        DeleteRequest deleteRequest = Requests.newDeleteRequest("system/" + systemName + "/__TEST__/TESTEX_NPE");
        connection.delete(new RootContext(), deleteRequest);
    }

    // IllegalArgumentException -> InternalServerErrorException
    @Test(dataProvider = "groovy-only", expectedExceptions = InternalServerErrorException.class, enabled = true)
    public void testInternalServerErrorExceptionFromIllegalArgumentException(String systemName) throws Exception {
        CreateRequest createRequest = Requests.newCreateRequest("system/" + systemName + "/__TEST__", getTestConnectorObject("TEST3"));
        connection.create(new SecurityContext(new RootContext(), "system", null), createRequest);
    }

    // ConnectorSecurityException -> InternalServerErrorException
    @Test(dataProvider = "groovy-only", expectedExceptions = InternalServerErrorException.class, enabled = true)
    public void testInternalServerErrorExceptionFromConnectorSecurityException(String systemName) throws Exception {
        ActionRequest actionRequest = Requests.newActionRequest("system/" + systemName + "/__TEST__", "authenticate");
        actionRequest.setAdditionalParameter("username", "TEST1");
        actionRequest.setAdditionalParameter("password", "Passw0rd");
        connection.action(new RootContext(), actionRequest);
    }

    // InvalidCredentialException - >  PermanentException (UNAUTHORIZED_ERROR_CODE)
    @Test(dataProvider = "groovy-only", expectedExceptions = PermanentException.class, enabled = true)
    public void testPermanentExceptionFromInvalidCredentialException(String systemName) throws Exception {
        ActionRequest actionRequest = Requests.newActionRequest("system/" + systemName + "/__TEST__", "authenticate");
        actionRequest.setAdditionalParameter("username", "TEST2");
        actionRequest.setAdditionalParameter("password", "Passw0rd");
        connection.action(new RootContext(), actionRequest);
    }

    // InvalidPasswordException -> PermanentException (UNAUTHORIZED_ERROR_CODE)
    @Test(dataProvider = "groovy-only", expectedExceptions = PermanentException.class, enabled = true)
    public void testPermanentExceptionFromInvalidPasswordException(String systemName) throws Exception {
        ActionRequest actionRequest = Requests.newActionRequest("system/" + systemName + "/__TEST__", "authenticate");
        actionRequest.setAdditionalParameter("username", "TEST3");
        actionRequest.setAdditionalParameter("password", "Passw0rd");
        connection.action(new RootContext(), actionRequest);
    }

    // PermissionDeniedException -> ForbiddenException
    @Test(dataProvider = "groovy-only", expectedExceptions = ForbiddenException.class, enabled = true)
    public void testForbiddenExceptionPermissionDeniedException(String systemName) throws Exception {
        ActionRequest actionRequest = Requests.newActionRequest("system/" + systemName + "/__TEST__", "authenticate");
        actionRequest.setAdditionalParameter("username", "TEST4");
        actionRequest.setAdditionalParameter("password", "Passw0rd");
        connection.action(new RootContext(), actionRequest);
    }

    // PasswordExpiredException -> ForbiddenException
    @Test(dataProvider = "groovy-only", expectedExceptions = ForbiddenException.class, enabled = true)
    public void testForbiddenExceptionFromPasswordExpiredException(String systemName) throws Exception {
        ActionRequest actionRequest = Requests.newActionRequest("system/" + systemName + "/__TEST__", "authenticate");
        actionRequest.setAdditionalParameter("username", "TEST5");
        actionRequest.setAdditionalParameter("password", "Passw0rd");
        connection.action(new RootContext(), actionRequest);
    }

    // UnknownUidException -> NotFoundException
    @Test(dataProvider = "groovy-only", expectedExceptions = NotFoundException.class, enabled = true)
    public void testNotFoundExceptionFromUnknownException(String systemName) throws Exception {
        ActionRequest actionRequest = Requests.newActionRequest("system/" + systemName + "/__SAMPLE__", "authenticate");
        actionRequest.setAdditionalParameter("username", "Unknown-UID");
        actionRequest.setAdditionalParameter("password", "Passw0rd");
        connection.action(new RootContext(), actionRequest);
    }

    // UnsupportedOperationException -> NotFoundException
    @Test(dataProvider = "groovy-only", expectedExceptions = NotFoundException.class, enabled = true)
    public void testNotFoundExceptionFromUnsupportedOperationException(String systemName) throws Exception {
        ActionRequest actionRequest = Requests.newActionRequest("system/" + systemName + "/Unsupported-Object", "authenticate");
        actionRequest.setAdditionalParameter("username", "TEST6");
        actionRequest.setAdditionalParameter("password", "Passw0rd");
        connection.action(new RootContext(), actionRequest);
    }

    // InvalidAttributeValueException - > BadRequestException
    @Test(dataProvider = "groovy-only", expectedExceptions = BadRequestException.class, enabled = true)
    public void testBadRequestException(String systemName) throws Exception {
        CreateRequest createRequest = Requests.newCreateRequest("system/" + systemName + "/__TEST__", getTestConnectorObject("TEST2"));
        connection.create(new SecurityContext(new RootContext(), "system", null), createRequest);
    }

    // PreconditionFailedException ->  org.forgerock.json.resource.PreconditionFailedException
    @Test(dataProvider = "groovy-only", expectedExceptions = PreconditionFailedException.class, enabled = true)
    public void testPreconditionFailedException(String systemName) throws Exception {
        final String resourceId = "TEST4";
        UpdateRequest updateRequest = Requests.newUpdateRequest("system/" + systemName + "/__TEST__/",
                resourceId,
                getTestConnectorObject(resourceId));
        connection.update(new SecurityContext(new RootContext(), "system", null), updateRequest);
    }

    // PreconditionRequiredException ->  org.forgerock.json.resource.PreconditionRequiredException
    @Test(dataProvider = "groovy-only", expectedExceptions = PreconditionRequiredException.class, enabled = true)
    public void testPreconditionRequiredException(String systemName) throws Exception {
        final String resourceId = "TEST5";
        UpdateRequest updateRequest = Requests.newUpdateRequest("system/" + systemName + "/__TEST__/",
                resourceId,
                getTestConnectorObject(resourceId));
        connection.update(new SecurityContext(new RootContext(), "system", null ), updateRequest);
    }

    // ResourceException ->  org.forgerock.json.resource.ResourceException
    @Test(dataProvider = "groovy-only", expectedExceptions = ResourceException.class, enabled = true)
    public void testResourceException(String systemName) throws Exception {
        final String resourceId = "TEST6";
        JsonValue user = getTestConnectorObject(resourceId);
        user.put("missingKey", "ignoredValue");
        UpdateRequest updateRequest = Requests.newUpdateRequest("system/" + systemName + "/__TEST__/",
                resourceId,
                user);
        connection.update(new SecurityContext(new RootContext(), "system", null ), updateRequest);
    }

    @Test(dataProvider = "groovy-only", enabled = true)
    public void testSyncWithAllObjectClass(String systemName) throws Exception {

        JsonValue stage = new JsonValue(new LinkedHashMap<String, Object>());
        stage.put("connectorData", ConnectorUtil.convertFromSyncToken(new SyncToken(17)));
        CreateRequest createRequest = Requests
                .newCreateRequest("repo/synchronisation/pooledSyncStage",
                        ("system" + systemName).toUpperCase(),
                        stage);
        connection.create(new RootContext(), createRequest);

        SyncStub sync = new SyncStub();
        Route r = router.addRoute("sync", sync);

        ActionRequest actionRequest = Requests.newActionRequest("system/" + systemName,
                SystemObjectSetService.SystemAction.liveSync.toString());

        stage = connection.action(new RootContext(), actionRequest);

        Assert.assertEquals(ConnectorUtil.convertToSyncToken(stage.get("connectorData")).getValue(), 17);
        Assert.assertEquals(sync.requests.size(), 0);

        router.removeRoute(r);
    }

    @Override
    public SyncFailureHandler create(JsonValue config) throws Exception {
        return NullSyncFailureHandler.INSTANCE;
    }
}
