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
package org.forgerock.openidm.scheduler;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.cluster.ClusterManagementService;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Basic unit tests for the scheduler service
 */
public class SchedulerServiceTest {
    
    /**
     * Returns a {@link JsonValue} object representing a JSON configuration.
     * 
     * @param configName the name of the file containing the configuration
     * @return a {@link JsonValue} object representing a JSON configuration
     * @throws Exception
     */
    private JsonValue getConfig(final String configName) throws Exception {
        final InputStream configStream = getClass().getResourceAsStream(configName);
        return new JsonValue(new ObjectMapper().readValue(configStream, LinkedHashMap.class)); 
    }
    
    /**
     * Creates a {@link SchedulerService} from the passed in configuration file.
     * 
     * @param configFile the scheduler's configuration file name
     * @return a {@link SchedulerService} implementation
     * @throws Exception
     */
    private SchedulerService createSchedulerService(final String configFile) throws Exception {
        final JSONEnhancedConfig jsonEnhancedConfig = mock(JSONEnhancedConfig.class);
        final ClusterManagementService clusterService = mock(ClusterManagementService.class);
        final SchedulerService schedulerService = new SchedulerService();
        when(jsonEnhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(getConfig(configFile));

        when(clusterService.getInstanceId()).thenReturn("test-node");
        // bind services
        schedulerService.bindEnhancedConfig(jsonEnhancedConfig);
        schedulerService.clusterManager = clusterService;
        // Activate the service
        schedulerService.activate(getMockedContext());
        return schedulerService;
    }

    /**
     * Returns a mocked {@link ComponentContext} instance.
     * @return a {@link ComponentContext} instance
     */
    private ComponentContext getMockedContext() {
        final ComponentContext mockedContext = mock(ComponentContext.class);
        final BundleContext mockedBundleContext = mock(BundleContext.class);
        final Dictionary<String, Object> compContextProperties = new Hashtable<>();
        when(mockedContext.getProperties()).thenReturn(compContextProperties);
        when(mockedContext.getBundleContext()).thenReturn(mockedBundleContext);
        return mockedContext;
    }

    @Test
    public void testCreateSchedulerService() throws Exception {
        final SchedulerService schedulerService = createSchedulerService("/scheduler.json");
        assertThat(schedulerService).isNotNull();
    }

    @Test
    public void testValidateQuartzCronExpressionAction() throws Exception {
        final SchedulerService schedulerService = createSchedulerService("/scheduler.json");
        assertThat(schedulerService).isNotNull();

        final ActionRequest request =
                Requests.newActionRequest("/scheduler", SchedulerService.SchedulerAction.validateQuartzCronExpression.name());
        request.setContent(json(object(field("cronExpression", "30 0/1 * * * ?"))));
        final Promise<ActionResponse, ResourceException> promise =
                schedulerService.handleAction(new RootContext(), request);

        assertThat(promise.get().getJsonContent().get("valid").asBoolean()).isEqualTo(true);
    }
}
