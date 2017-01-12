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
 * Copyright 2015-2017 ForgeRock AS.
 */
package org.forgerock.openidm.cluster;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.router.IDMConnectionFactoryWrapper;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.openidm.cluster.mocks.MockRepositoryService;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Tests {@link ClusterManager}
 */
public class ClusterManagerTest {
    private static final boolean REMOVE_OFFLINE_NODE = true;
    private static final String NODE_ID = "test-node";

    private static JsonValue CONFIG = json(object(
                field("instanceId", NODE_ID),
                field("instanceTimeout", "30000"),
                field("instanceRecoveryTimeout", "30000"),
                field("instanceCheckInInterval", "5000"),
                field("instanceCheckInOffset", "0"),
                field("enabled", true)));

    private RequestHandler clusterHandler = null;
    private ClusterManager node = null;
    private MockRepositoryService mockRepoService;

    public void setUp(final boolean removeOfflineNode) throws ResourceException, InterruptedException {
        System.setProperty(ClusterManager.OPENIDM_CLUSTER_REMOVE_OFFLINE_NODE_STATE, String.valueOf(removeOfflineNode));
        mockRepoService = new MockRepositoryService();
        final IDMConnectionFactory idmConnectionFactory =
                new IDMConnectionFactoryWrapper(Resources.newInternalConnectionFactory(mockRepoService));

        node = createClusterManager(mockRepoService, idmConnectionFactory, CONFIG);
    	// Start the Cluster Management Service thread
    	node.startClusterManagement();

        // Allow enough time for the cluster management thread
        // to initialize the node data in the repository
        Thread.sleep(1000);

        // create a ClusterManager to act as the request handler
        clusterHandler = new ClusterManager() {{
            repoService = mockRepoService;
            connectionFactory = idmConnectionFactory;
        }};
    }

    private ClusterManager createClusterManager(RepositoryService repoService, IDMConnectionFactory connectionFactory,
            JsonValue config) {
        final ClusterManager clusterManager = new ClusterManager();
        clusterManager.repoService = repoService;
        clusterManager.connectionFactory = connectionFactory;
        clusterManager.init(config);
        return clusterManager;
    }

    @AfterMethod
    public void tearDown() throws ResourceException {
    	// Stop the Cluster Management Service thread
    	node.stopClusterManagement();
    }
    
    @Test
    public void testReadEntry() throws Exception {
        setUp(!REMOVE_OFFLINE_NODE);
    	final ReadRequest readRequest = Requests.newReadRequest("test-node");
    	final ResourceResponse resource = clusterHandler.handleRead(new RootContext(), readRequest).get();
    	// Test basic instance fields
    	assertThat(resource.getContent()).stringAt("state").isEqualTo("running");
    	assertThat(resource.getContent()).stringAt("instanceId").isEqualTo("test-node");
    	assertThat(resource.getContent()).stringAt("startup").isNotNull();
    	assertThat(resource.getContent()).stringAt("shutdown").isEqualTo("");
    }
    
    @Test
    public void testClusterManagement() throws IOException, InterruptedException {
        setUp(!REMOVE_OFFLINE_NODE);
    	// Test basic ClusterManagementService methods
    	Assertions.assertThat(node.getInstanceId()).isEqualTo("test-node");
    	Assertions.assertThat(node.isEnabled()).isTrue();
    	Assertions.assertThat(node.isStarted()).isTrue();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testNoClusterNodeIdInConfig() throws Exception  {
        setUp(!REMOVE_OFFLINE_NODE);
        final ClusterManager clusterManager = new ClusterManager();
        clusterManager.init(json(object(field("instanceId","&{openidm.node.id}"))));
    }

    @Test
    public void noClusterRepoHasStateDownUponDeactivation() throws ResourceException, InterruptedException{
        setUp(!REMOVE_OFFLINE_NODE);
        node.deactivate(newMockComponentContext());
        final JsonValue instanceState = mockRepoService.read(Requests.newReadRequest("cluster/states/" + NODE_ID)).getContent();
        Assertions.assertThat(instanceState.get("state").asInteger()).isEqualTo(InstanceState.STATE_DOWN);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void clusterRepoHasStateDownUponDeactivation() throws ResourceException, InterruptedException{
        setUp(REMOVE_OFFLINE_NODE);
        node.deactivate(newMockComponentContext());
        mockRepoService.read(Requests.newReadRequest("cluster/states/" + NODE_ID)).getContent();
    }

    private ComponentContext newMockComponentContext() {
        final ComponentContext context = mock(ComponentContext.class);
        when(context.toString()).thenReturn("mock CC");
        return context;
    }
}
