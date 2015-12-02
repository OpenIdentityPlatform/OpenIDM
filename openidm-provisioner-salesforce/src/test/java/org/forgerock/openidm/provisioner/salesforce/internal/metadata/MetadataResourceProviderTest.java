/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.provisioner.salesforce.internal.metadata;

import java.awt.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.salesforce.internal.GuiceSalesforceModule;
import org.forgerock.openidm.provisioner.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.provisioner.salesforce.internal.TestUtil;
import org.forgerock.services.context.RootContext;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import com.google.inject.Inject;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
@Guice(modules = GuiceSalesforceModule.class)
public class MetadataResourceProviderTest {

    @Inject
    @Nullable
    TestUtil.TestRouterRegistry routerRegistry = null;

    @BeforeClass
    public void beforeClass() throws Exception {
        if (null == routerRegistry) {
            throw new SkipException("Test is skipped because config file not exits at: ");
        }
    }

    @Test
    public void testReadInstance() throws Exception {

        JsonValue content =
                new JsonValue(SalesforceConnection.mapper.readValue(TEST_CONFIG, Map.class));
        CreateRequest createRequest =
                Requests.newCreateRequest("/system/test/metadata/samlssoconfig", content);
        ResourceResponse resource0 =
                routerRegistry.getConnection().create(new RootContext(), createRequest);

        Assert.assertNotNull(resource0.getId());
        ReadRequest readRequest =
                Requests.newReadRequest("/system/test/metadata/SamlSsoConfig", resource0.getId());
        ResourceResponse resource1 =
                routerRegistry.getConnection().read(new RootContext(), readRequest);

        UpdateRequest updateRequest =
                Requests.newUpdateRequest("/system/test/metadata/SamlSsoConfig", resource1.getId(),
                        content);
        updateRequest.getContent().put("fullName", "Test SAML Config RENAME");
        ResourceResponse resource2 =
                routerRegistry.getConnection().update(new RootContext(), updateRequest);

        Assert.assertEquals(resource2.getId(), "Test SAML Config RENAME");
        DeleteRequest deleteRequest =
                Requests.newDeleteRequest("/system/test/metadata/SamlSsoConfig", resource2.getId());
        ResourceResponse resource3 =
                routerRegistry.getConnection().delete(new RootContext(), deleteRequest);
    }

    @Test
    public void testQueryCollection() throws Exception {
        QueryRequest queryRequest = Requests.newQueryRequest("/system/test/metadata/samlssoconfig");
        queryRequest.setQueryId(ServerConstants.QUERY_ALL_IDS);
        Set<ResourceResponse> results = new HashSet<ResourceResponse>();

        QueryResponse queryResult =
                routerRegistry.getConnection()
                        .query(new RootContext(), queryRequest, results);

        Assert.assertFalse(results.isEmpty());

    }

    @Test(groups = { "timeout" })
    public void testTimeout() throws Exception {
        QueryRequest queryRequest = Requests.newQueryRequest("/system/test/metadata/samlssoconfig");
        queryRequest.setQueryId(ServerConstants.QUERY_ALL_IDS);
        int i = 0;
        do {
            Set<ResourceResponse> results = new HashSet<ResourceResponse>();
            routerRegistry.getConnection().query(new RootContext(), queryRequest, results);
            // Assert.assertFalse(results.isEmpty());
            Thread.sleep(16 * 60 * 1000);
            Toolkit.getDefaultToolkit().beep();
        } while (i++ < 5);
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
