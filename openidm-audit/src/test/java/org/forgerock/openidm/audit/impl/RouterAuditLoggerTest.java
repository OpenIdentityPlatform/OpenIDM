/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.audit.mocks.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.openidm.audit.util.AuditTestUtils.createLogEntry;
import static org.forgerock.openidm.audit.util.AuditTestUtils.checkLogEntry;
import static org.forgerock.openidm.audit.util.AuditTestUtils.Operation;

public class RouterAuditLoggerTest {

    private ConnectionFactory connectionFactory;
    private RouterAuditLogger routerAuditLogger;
    private MockRequestHandler mockRequestHandler;
    final private ObjectMapper mapper = new ObjectMapper();

    @DataProvider(name = "logTypes")
    public static Object[][]auditLogTypes() {
        return new Object[][] {
                {AuditServiceImpl.TYPE_SYNC},
                {AuditServiceImpl.TYPE_RECON},
                {AuditServiceImpl.TYPE_ACTIVITY},
                {AuditServiceImpl.TYPE_ACCESS}
        };
    }

    @BeforeMethod
    public void setUp() {
        mockRequestHandler = new MockRequestHandler();
        connectionFactory = Resources.newInternalConnectionFactory(mockRequestHandler);
        final JsonValue config = new JsonValue(new HashMap<String, Object>());
        config.add(RouterAuditLogger.CONFIG_LOG_LOCATION, "location");
        routerAuditLogger = new RouterAuditLogger(connectionFactory);
        routerAuditLogger.setConfig(config);
    }

    @AfterMethod
    public void tearDown() {
        mockRequestHandler.getRequests().clear();
        mockRequestHandler = null;
        connectionFactory = null;
        routerAuditLogger = null;
    }

    @Test(dataProvider = "logTypes")
    public void testReadSingleEntry(String logType) throws ResourceException, IOException {
        //given
        final JsonValue content = new JsonValue(createLogEntry(logType, Operation.READ));
        mockRequestHandler.addResource(new Resource(Resource.FIELD_ID, Resource.FIELD_REVISION, content));

        //when
        final Map<String, Object> entry =
                routerAuditLogger.read(new ServerContext(new RootContext()), logType, Resource.FIELD_ID);

        //then
        assertThat(mockRequestHandler.getRequests().size() == 1);
        mockRequestHandler.getRequests().clear();
        checkLogEntry(logType, entry, Operation.READ);

    }

    @Test(dataProvider = "logTypes")
    public void testReadMultipleEntries(String logType) throws ResourceException, IOException {
        //given
        final JsonValue content = new JsonValue(createLogEntry(logType, Operation.READ));
        mockRequestHandler.addResource(new Resource(Resource.FIELD_ID, Resource.FIELD_REVISION, content));

        //when
        final Map<String, Object> listOfEntries =
                routerAuditLogger.read(new ServerContext(new RootContext()), logType, null);

        //then
        @SuppressWarnings("unchecked")
        final List<Map<String,Object>> entries = (List<Map<String,Object>>) listOfEntries.get("entries");

        assertThat(mockRequestHandler.getRequests().size() == 1);
        mockRequestHandler.getRequests().clear();

        for (Map<String, Object> entry : entries) {
            checkLogEntry(logType, entry, Operation.READ);
        }
    }

    @Test(dataProvider = "logTypes")
    public void testCreateEntry(String logType) throws ResourceException, IOException {
        //given
        final JsonValue content = new JsonValue(createLogEntry(logType, Operation.CREATE));

        //when
        routerAuditLogger.create(new ServerContext(new RootContext()), logType, content.asMap());

        //then

        assertThat(mockRequestHandler.getRequests().size() == 1);
        CreateRequest request = (CreateRequest) mockRequestHandler.getRequests().get(0);
        //check it against read because the router audit logger doesn't flatten entries
        checkLogEntry(logType, request.getContent().asMap(), Operation.READ);
        mockRequestHandler.getRequests().clear();
    }

}
