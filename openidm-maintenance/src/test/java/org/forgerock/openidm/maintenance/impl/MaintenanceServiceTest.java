/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.maintenance.impl;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.servlet.internal.ServletConnectionFactory;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.groovy.GroovyScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.http.routing.RoutingMode;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.script.Bindings;
import javax.script.SimpleBindings;

/**
 * Test the Maintenance Service
 */
public class MaintenanceServiceTest {
    
    private MaintenanceService maintenanceService = new MaintenanceService();
    
    private Connection connection = null;

    @BeforeClass
    public void BeforeClass() throws Exception {

        URL config = MaintenanceServiceTest.class.getResource("/conf/router.json");
        assertThat(config).isNotNull().overridingErrorMessage("router configuration is not found");

        JsonValue configuration =
                new JsonValue((new ObjectMapper()).readValue(new File(config.toURI()), Map.class));

        final Router router = new Router();
        router.addRoute(uriTemplate("/audit/recon"), new MemoryBackend());
        router.addRoute(uriTemplate("/managed/user"), new MemoryBackend());
        router.addRoute(uriTemplate("/system/OpenDJ/account"), new MemoryBackend());
        router.addRoute(uriTemplate("/system/AD/account"), new MemoryBackend());
        router.addRoute(RoutingMode.EQUALS, uriTemplate("maintenance"), maintenanceService);

        final ScriptRegistryImpl sr = new ScriptRegistryImpl(new HashMap<String, Object>(),
                Collections.<ScriptEngineFactory>singleton(new GroovyScriptEngineFactory()), new SimpleBindings());

        final EnhancedConfig enhancedConfig = mock(EnhancedConfig.class);
        when(enhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(configuration);
        when(enhancedConfig.getConfigurationFactoryPid(any(ComponentContext.class)))
                .thenReturn("");

        ServletConnectionFactory filterService = new ServletConnectionFactory() {{
            bindRequestHandler(router);
            bindScriptRegistry(sr);
            bindEnhancedConfig(enhancedConfig);
            bindMaintenanceFilter(maintenanceService);
            activate(mock(ComponentContext.class));
        }};
        connection = filterService.getConnection();
    }

    @Test(expectedExceptions = ServiceUnavailableException.class)
    public void testMaintenanceModeEnable() throws Exception {
        ActionRequest enableAction = Requests.newActionRequest("maintenance", "enable");
        assertThat(connection.action(new RootContext(), enableAction).getJsonContent()
                .get("maintenanceEnabled").asBoolean());
        connection.delete(new RootContext(), Requests.newDeleteRequest("managed/user/0"));
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testMaintenanceModeDisable() throws Exception {
        ActionRequest enableAction = Requests.newActionRequest("maintenance", "enable");
        assertThat(connection.action(new RootContext(), enableAction).getJsonContent()
                .get("maintenanceEnabled").asBoolean());

        ActionRequest disableAction = Requests.newActionRequest("maintenance", "disable");
        assertThat(!connection.action(new RootContext(), disableAction).getJsonContent()
                .get("maintenanceEnabled").asBoolean());
        connection.delete(new RootContext(), Requests.newDeleteRequest("managed/user/0"));
    }
}
