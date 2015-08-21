package org.forgerock.openidm.cluster;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.forgerock.http.context.RootContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.openidm.cluster.mocks.MockRepositoryService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests {@link ClusterManager}
 */
public class ClusterManagerTest {
	
    private static JsonValue config = json(object(
                field("instanceId", "test-node"),
                field("instanceTimeout", "30000"),
                field("instanceRecoveryTimeout", "30000"),
                field("instanceCheckInInterval", "5000"),
                field("instanceCheckInOffset", "0"),
                field("enabled", true)));

    private RequestHandler clusterHandler = null;
    private ClusterManagementService clusterService = null;

    @BeforeMethod
    public void setUp() throws ResourceException, InterruptedException {
    	final ClusterManager clusterManager = new ClusterManager();
    	final MockRepositoryService mockRepoService = new MockRepositoryService();
    	clusterManager.repoService = mockRepoService;
    	clusterManager.connectionFactory = Resources.newInternalConnectionFactory(mockRepoService);
    	clusterManager.init(config);
    	clusterHandler = clusterManager;
    	clusterService = clusterManager;
    	// Start the Cluster Management Service thread
    	clusterService.startClusterManagement();
    	
    	// Allow enough time for the cluster management thread 
    	// to initialize the node data in the repository
    	Thread.sleep(1000);
    }
    
    @AfterMethod
    public void tearDown() throws ResourceException {
    	// Stop the Cluster Management Service thread
    	clusterService.stopClusterManagement();
    }
    
    @Test
    public void testReadEntry() throws ResourceException, Exception {
    	final ReadRequest readRequest = Requests.newReadRequest("test-node");
    	final ResourceResponse resource = clusterHandler.handleRead(new RootContext(), readRequest).get();
    	// Test basic instance fields
    	Assertions.assertThat(resource.getContent().get("state").asString()).isEqualTo("running");
    	Assertions.assertThat(resource.getContent().get("instanceId").asString()).isEqualTo("test-node");
    	Assertions.assertThat(resource.getContent().get("startup").asString()).isNotNull();
    	Assertions.assertThat(resource.getContent().get("shutdown").asString()).isEqualTo("");
    }
    
    @Test
    public void testClusterManagement() throws ResourceException, IOException {
    	// Test basic ClusterManagementService methods
    	Assertions.assertThat(clusterService.getInstanceId()).isEqualTo("test-node");
    	Assertions.assertThat(clusterService.isEnabled()).isTrue();
    	Assertions.assertThat(clusterService.isStarted()).isTrue();    	
    }

	
	
}
