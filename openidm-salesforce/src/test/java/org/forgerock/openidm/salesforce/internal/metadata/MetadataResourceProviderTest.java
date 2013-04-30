package org.forgerock.openidm.salesforce.internal.metadata;

import java.io.File;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.openidm.salesforce.internal.SalesforceConfiguration;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.data.SalesforceRequestHandler;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class MetadataResourceProviderTest {

    private SalesforceConnection connection = null;
    private Connection router = null;

    @BeforeClass
    public void beforeClass() throws Exception {
        File configFile = new File(System.getProperty("user.home"), "salesforce.json");
        if (configFile.exists()) {
            JsonValue config = new JsonValue((new ObjectMapper()).readValue(configFile, Map.class));
            SalesforceConfiguration configuration =
                    SalesforceRequestHandler.parseConfiguration(config
                            .get("configurationProperties"));
            connection = new SalesforceConnection(configuration);

            Router r = new Router();

            r.addRoute("/metadata/{metadataType}", new MetadataResourceProvider(connection));

            router = Resources.newInternalConnection(r);
        }
    }

    @AfterClass
    public void afterClass() throws Exception {
        if (null != connection) {
            connection.dispose();
        }
    }

    @Test
    public void testReadInstance() throws Exception {
        if (null == connection) {
            throw new SkipException("Salesforce connection is not available");
        }
        ReadRequest readRequest = Requests.newReadRequest("/metadata/SamlSsoConfig","xxx");
        Resource r = router.read(new RootContext(), readRequest);
    }
}
