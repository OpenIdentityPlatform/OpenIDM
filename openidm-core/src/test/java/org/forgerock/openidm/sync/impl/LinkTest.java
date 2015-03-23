/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock Inc. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openidm.sync.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.script.ScriptRegistry;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

public class LinkTest {

    private ObjectMapping objectMapping;
    private JsonValue mappingConfig;

    @BeforeClass
    public void setUp() throws Exception {
        URL config = ObjectMappingTest.class.getResource("/conf/sync.json");
        Assert.assertNotNull(config, "sync configuration is not found");
        JsonValue syncConfig = new JsonValue((new ObjectMapper()).readValue(new File(config.toURI()), Map.class));
        mappingConfig = syncConfig.get("mappings").get(0);
        Scripts.init(mock(ScriptRegistry.class));

    }

    @AfterMethod
    public void tearDown() {
    }

    @Test
    public void setLinkQualifierTest() {
        Link link = new Link(mock(ObjectMapping.class));
        link.setLinkQualifier("default");
        assertEquals(link.linkQualifier, "default");
    }

    @Test
    public void clearTest() {

        Link link = new Link(mock(ObjectMapping.class));
        link._id = UUID.randomUUID().toString();
        link._rev = "testRev";
        link.sourceId = "sourceId";
        link.targetId = "targetId";

        link.clear();

        assertEquals(link._id, null);
        assertEquals(link._rev, null);
        assertEquals(link.sourceId, null);
        assertEquals(link.targetId, null);

    }

    @Test
    public void createTest() throws ResourceException {

        SynchronizationService synchronizationServiceMock = mock(SynchronizationService.class);
        ConnectionFactory connectionFactorymock = mock(ConnectionFactory.class);
        Connection connectionMock = mock(Connection.class);
        LinkType linkTypeMock = mock(LinkType.class);

        when(synchronizationServiceMock.getConnectionFactory()).thenReturn(connectionFactorymock);
        when(connectionFactorymock.getConnection()).thenReturn(connectionMock);

        objectMapping = new ObjectMapping(synchronizationServiceMock, mappingConfig);
        Link link = new Link(objectMapping);
        objectMapping.linkType = linkTypeMock;
        link.setLinkQualifier("default");

        when(linkTypeMock.normalizeSourceId(anyString())).thenReturn("sourceId");
        when(linkTypeMock.normalizeTargetId(anyString())).thenReturn("targetId");
        when(linkTypeMock.getName()).thenReturn("linkType");
        when(linkTypeMock.useReverse()).thenReturn(true);

        when(objectMapping.getService().getServerContext()).thenReturn(new ServerContext(new RootContext()));
        when(connectionMock.create(any(ServerContext.class), any(CreateRequest.class)))
                .thenReturn(new Resource("testId", "testRevision", new JsonValue("testObject")));

        link.create();

        assertEquals(link.linkQualifier, "default");
        assertEquals(link._rev, "testRevision");
        assertEquals(link._id, "testId");
        assertEquals(link.initialized, true);

    }

    @Test
    public void updateTest() throws ResourceException {
        SynchronizationService synchronizationServiceMock = mock(SynchronizationService.class);
        ConnectionFactory connectionFactorymock = mock(ConnectionFactory.class);
        Connection connectionMock = mock(Connection.class);
        LinkType linkTypeMock = mock(LinkType.class);

        when(synchronizationServiceMock.getConnectionFactory()).thenReturn(connectionFactorymock);
        when(connectionFactorymock.getConnection()).thenReturn(connectionMock);

        objectMapping = new ObjectMapping(synchronizationServiceMock, mappingConfig);
        Link link = new Link(objectMapping);
        objectMapping.linkType = linkTypeMock;
        link.setLinkQualifier("default");

        when(linkTypeMock.normalizeSourceId(anyString())).thenReturn("sourceId");
        when(linkTypeMock.normalizeTargetId(anyString())).thenReturn("targetId");
        when(linkTypeMock.getName()).thenReturn("linkType");
        when(linkTypeMock.useReverse()).thenReturn(true);

        when(objectMapping.getService().getServerContext()).thenReturn(new ServerContext(new RootContext()));
        doReturn(new Resource("testId", "testRevision", new JsonValue("testObject")))
                .when(connectionMock).update(any(ServerContext.class), any(UpdateRequest.class));
        link._id = "testId";
        link.update();

        assertEquals(link.linkQualifier, "default");
        assertEquals(link._rev, "testRevision");
        assertEquals(link._id, "testId");

    }

    @Test
    public void deleteTest() throws ResourceException {
        SynchronizationService synchronizationServiceMock = mock(SynchronizationService.class);
        ConnectionFactory connectionFactorymock = mock(ConnectionFactory.class);
        Connection connectionMock = mock(Connection.class);
        LinkType linkTypeMock = mock(LinkType.class);

        when(synchronizationServiceMock.getConnectionFactory()).thenReturn(connectionFactorymock);
        when(connectionFactorymock.getConnection()).thenReturn(connectionMock);

        objectMapping = new ObjectMapping(synchronizationServiceMock, mappingConfig);
        Link link = new Link(objectMapping);
        objectMapping.linkType = linkTypeMock;
        link.setLinkQualifier("default");
        link._id = UUID.randomUUID().toString();

        when(objectMapping.getService().getServerContext()).thenReturn(new ServerContext(new RootContext()));
        doReturn(new Resource("testId", "testRevision", new JsonValue("testObject")))
                .when(connectionMock).delete(any(ServerContext.class), any(DeleteRequest.class));
        link.delete();

        // make sure that _id has been set to null
        assertEquals(link._id, null);
    }
}
