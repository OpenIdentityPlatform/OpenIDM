/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.salesforce.internal.metadata;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.salesforce.internal.SalesforceConfiguration;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.data.SalesforceRequestHandler;
import org.forgerock.openidm.util.JsonUtil;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class MetadataResourceProviderTest {

    private SalesforceConnection connection = null;
    private Connection router = null;
    private ObjectMapper mapper = JsonUtil.build();

    @BeforeClass
    public void beforeClass() throws Exception {
        File configFile = new File(System.getProperty("user.home"), "salesforce.json");
        if (configFile.exists()) {
            JsonValue config = new JsonValue(mapper.readValue(configFile, Map.class));
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
        JsonValue content = new JsonValue(mapper.readValue(TEST_CONFIG, Map.class));
        CreateRequest createRequest = Requests.newCreateRequest("/metadata/samlssoconfig", content);
        Resource resource0 = router.create(new RootContext(), createRequest);

        Assert.assertNotNull(resource0.getId());
        ReadRequest readRequest =
                Requests.newReadRequest("/metadata/SamlSsoConfig", resource0.getId());
        Resource resource1 = router.read(new RootContext(), readRequest);

        UpdateRequest updateRequest =
                Requests.newUpdateRequest("/metadata/SamlSsoConfig", resource1.getId(), content);
        updateRequest.getNewContent().put("fullName", "Test SAML Config RENAME");
        Resource resource2 = router.update(new RootContext(), updateRequest);

        Assert.assertEquals(resource2.getId(), "Test SAML Config RENAME");
        DeleteRequest deleteRequest =
                Requests.newDeleteRequest("/metadata/SamlSsoConfig", resource2.getId());
        Resource resource3 = router.delete(new RootContext(), deleteRequest);
    }

    @Test
    public void testQueryCollection() throws Exception {
        if (null == connection) {
            throw new SkipException("Salesforce connection is not available");
        }
        QueryRequest queryRequest = Requests.newQueryRequest("/metadata/samlssoconfig");
        queryRequest.setQueryId(ServerConstants.QUERY_ALL_IDS);
        Set<Resource> results = new HashSet<Resource>();

        QueryResult queryResult = router.query(new RootContext(), queryRequest, results);

        Assert.assertFalse(results.isEmpty());

    }

    @Test
    public void testLoadMetadata() throws Exception {

        byte[] zipFile =
                IOUtils.toByteArray(MetadataResourceProviderTest.class
                        .getResourceAsStream("/metadata/metadata.zip"));
        String type = "samlssoconfig";
        String id = "Migrated_SAML_Config";
        Resource metaResource = MetadataResourceProvider.getMetadataResource(zipFile, type, id);

        Assert.assertNotNull(metaResource);

        Assert.assertNull(MetadataResourceProvider.getMetadataResource(zipFile, type, "Not_EXITS"));

        try {
            MetadataResourceProvider.getMetadataResource(zipFile, "Not_Supported", id);
            Assert.fail();
        } catch (NotFoundException e) {
            /* expected */
        }
    }

    /* @formatter:off */
    private static final String TEST_CONFIG =
            "{\n" +
            "   \"attributeName\":\"ATTR_UID\",\n" +
            "   \"attributeNameIdFormat\":null,\n" +
            "   \"errorUrl\":null,\n" +
            "   \"fullName\":\"Test SAML Config\",\n" +
            "   \"identityLocation\":\"Attribute\",\n" +
            "   \"identityMapping\":\"FederationId\",\n" +
            "   \"issuer\":\"https://localhost:8443/admin/index.html\",\n" +
            "   \"loginUrl\":\"https://localhost:8443/admin/index.html\",\n" +
            "   \"logoutUrl\":\"https://localhost:8443/admin/index.html#logout/\",\n" +
            "   \"name\":\"Test SAML Config\",\n" +
            /*"   \"oauthTokenEndpoint\":\"https://na9.salesforce.com/services/oauth2/token?so=00DE0000000crze\",\n" +*/
            "   \"redirectBinding\":true,\n" +
            /*"   \"salesforceLoginUrl\":\"https://na9.salesforce.com?so=00DE0000000crze\",\n" +*/
            "   \"samlEntityId\":\"https://saml.salesforce.com\",\n" +
            "   \"samlVersion\":\"SAML2_0\",\n" +
            "   \"userProvisioning\":false,\n" +
            "   \"validationCert\":\"MIICTzCCAbigAwIBAgIIHGZngDP0KZ0wDQYJKoZIhvcNAQEEBQAwajE0MDIGA1UEAwwraHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0My9vcGVuaWRtdWkvaW5kZXguaHRtbDENMAsGA1UECwwETm9uZTEUMBIGA1UECgwLTm9uZSBMPU5vbmUxDTALBgNVBAYTBE5vbmUwHhcNMTMwMzE4MDMyODMyWhcNMjMwNDE1MDMyODMyWjBqMTQwMgYDVQQDDCtodHRwczovL2xvY2FsaG9zdDo4NDQzL29wZW5pZG11aS9pbmRleC5odG1sMQ0wCwYDVQQLDAROb25lMRQwEgYDVQQKDAtOb25lIEw9Tm9uZTENMAsGA1UEBhMETm9uZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAuUetutP5TUTJAKHfcT7pdaxxsERESih9HyTiXta/aFFjKd7ioLkv7b6l/5vndu/5CjCxcmNU6U+21z/tiRJYYN57Pbi1HY8fQLzVbU4NnHhjuAoGI5vc8W0CRS42g62TH3e01lnshveW+PIJuLByAKN2pRcqZauSxwd/3OupfN8CAwEAATANBgkqhkiG9w0BAQQFAAOBgQA1fmTMCchH2+1nTHuHdgBhSU8h9uGA6xMSSg8oyV5pt7hDc5cP2lTCxKvf9omrawFbEdYbY3vu4qTzZbSXF4hs2oK2Nxq7/oWIzA5Ocf3TwiqfD/tS4M2mqOE3Numx7m3V4hva0OWSy/54xWntf+tJuxcnkAJjNiywEnpwJoGcXQ==\"\n" +
            "}";
    /* @formatter:on */
}
