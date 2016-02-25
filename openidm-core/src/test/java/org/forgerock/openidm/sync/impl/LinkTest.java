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
 * Portions copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.util.Scripts;
import org.forgerock.script.ScriptRegistry;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

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

        ConnectionFactory connectionFactoryMock = mock(ConnectionFactory.class);
        Connection connectionMock = mock(Connection.class);
        LinkType linkTypeMock = mock(LinkType.class);

        when(connectionFactoryMock.getConnection()).thenReturn(connectionMock);

        objectMapping = new ObjectMapping(connectionFactoryMock, mappingConfig);
        Link link = new Link(objectMapping);
        objectMapping.linkType = linkTypeMock;
        link.setLinkQualifier("default");

        when(linkTypeMock.normalizeSourceId(anyString())).thenReturn("sourceId");
        when(linkTypeMock.normalizeTargetId(anyString())).thenReturn("targetId");
        when(linkTypeMock.getName()).thenReturn("linkType");
        when(linkTypeMock.useReverse()).thenReturn(true);

        when(connectionMock.create(any(Context.class), any(CreateRequest.class)))
                .thenReturn(newResourceResponse("testId", "testRevision", new JsonValue("testObject")));

        link.create(new RootContext());

        assertEquals(link.linkQualifier, "default");
        assertEquals(link._rev, "testRevision");
        assertEquals(link._id, "testId");
        assertEquals(link.initialized, true);
    }

    @Test
    public void updateTest() throws ResourceException {
        ConnectionFactory connectionFactoryMock = mock(ConnectionFactory.class);
        Connection connectionMock = mock(Connection.class);
        LinkType linkTypeMock = mock(LinkType.class);

        when(connectionFactoryMock.getConnection()).thenReturn(connectionMock);

        objectMapping = new ObjectMapping(connectionFactoryMock, mappingConfig);
        Link link = new Link(objectMapping);
        objectMapping.linkType = linkTypeMock;
        link.setLinkQualifier("default");

        when(linkTypeMock.normalizeSourceId(anyString())).thenReturn("sourceId");
        when(linkTypeMock.normalizeTargetId(anyString())).thenReturn("targetId");
        when(linkTypeMock.getName()).thenReturn("linkType");
        when(linkTypeMock.useReverse()).thenReturn(true);

        doReturn(newResourceResponse("testId", "testRevision", new JsonValue("testObject")))
                .when(connectionMock).update(any(Context.class), any(UpdateRequest.class));
        link._id = "testId";
        link.update(new RootContext());

        assertEquals(link.linkQualifier, "default");
        assertEquals(link._rev, "testRevision");
        assertEquals(link._id, "testId");

    }

    @Test
    public void deleteTest() throws ResourceException {
        ConnectionFactory connectionFactoryMock = mock(ConnectionFactory.class);
        Connection connectionMock = mock(Connection.class);
        LinkType linkTypeMock = mock(LinkType.class);

        when(connectionFactoryMock.getConnection()).thenReturn(connectionMock);

        objectMapping = new ObjectMapping(connectionFactoryMock, mappingConfig);
        Link link = new Link(objectMapping);
        objectMapping.linkType = linkTypeMock;
        link.setLinkQualifier("default");
        link._id = UUID.randomUUID().toString();

        doReturn(newResourceResponse("testId", "testRevision", new JsonValue("testObject")))
                .when(connectionMock).delete(any(Context.class), any(DeleteRequest.class));
        link.delete(new RootContext());

        // make sure that _id has been set to null
        assertEquals(link._id, null);
    }
}
