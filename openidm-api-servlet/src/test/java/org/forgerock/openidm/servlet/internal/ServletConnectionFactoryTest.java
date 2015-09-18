/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.servlet.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resources;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.source.DirectoryContainer;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A test of the ServletConnectionFactory and underlying request handler.
 * 
 */
public class ServletConnectionFactoryTest {

    private Connection testable = null;

    @BeforeClass
    public void BeforeClass() throws Exception {

        URL config = ServletConnectionFactoryTest.class.getResource("/conf/router.json");
        assertThat(config).isNotNull().overridingErrorMessage("router configuration is not found");

        JsonValue configuration =
                new JsonValue((new ObjectMapper()).readValue(new File(config.toURI()), Map.class));

        final Router requestHandler = new Router();
        requestHandler.addRoute(uriTemplate("/audit/recon"), new MemoryBackend());
        requestHandler.addRoute(uriTemplate("/managed/user"), new MemoryBackend());
        requestHandler.addRoute(uriTemplate("/system/OpenDJ/account"), new MemoryBackend());
        requestHandler.addRoute(uriTemplate("/system/AD/account"), new MemoryBackend());

        final ConnectionFactory connectionFactory = Resources.newInternalConnectionFactory(requestHandler);
        Bindings globalScope = new SimpleBindings();
        globalScope.put("openidm", FunctionFactory.getResource(connectionFactory));

        ScriptRegistryImpl sr =
                new ScriptRegistryImpl(new HashMap<String, Object>(), ServiceLoader
                        .load(ScriptEngineFactory.class), globalScope);

        URL script = ServletConnectionFactoryTest.class.getResource("/script/");
        assertThat(script).isNotNull().overridingErrorMessage("Failed to find /recon/script folder in test");
        sr.addSourceUnit(new DirectoryContainer("script", script));

        final EnhancedConfig enhancedConfig = mock(EnhancedConfig.class);
        when(enhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(configuration);
        when(enhancedConfig.getConfigurationFactoryPid(any(ComponentContext.class)))
                .thenReturn("");

        ServletConnectionFactory filterService = new ServletConnectionFactory();
        filterService.bindRequestHandler(requestHandler);
        filterService.bindScriptRegistry(sr);
        filterService.bindEnhancedConfig(enhancedConfig);
        filterService.activate(mock(ComponentContext.class));

        testable = filterService.getConnection();
    }

    @Test
    public void testActivate() throws Exception {
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        testable.create(createContext("admin"), Requests.newCreateRequest("/managed/user", content));
    }

    private Context createContext(String id) {
        final Map<String, Object> authzid = new HashMap<>();
        authzid.put(SecurityContext.AUTHZID_ID, id);
        List<String> roles = new ArrayList<String>();
        roles.add("system");
        authzid.put(SecurityContext.AUTHZID_ROLES, roles);
        authzid.put(SecurityContext.AUTHZID_COMPONENT, "managed");
        return new SecurityContext(new RootContext(), id, authzid);
    }
}
