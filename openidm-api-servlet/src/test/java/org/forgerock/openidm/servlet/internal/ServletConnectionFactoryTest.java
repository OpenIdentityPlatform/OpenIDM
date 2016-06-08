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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openidm.servlet.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.SimpleBindings;

import org.forgerock.json.resource.*;
import org.forgerock.script.groovy.GroovyScriptEngineFactory;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.source.DirectoryContainer;
import org.forgerock.util.promise.Promise;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A test of the ServletConnectionFactory and underlying request handler.
 */
public class ServletConnectionFactoryTest {

    private final Router requestHandler = new Router();

    private final ScriptRegistryImpl sr = new ScriptRegistryImpl(new HashMap<String, Object>(),
            Collections.<ScriptEngineFactory>singleton(new GroovyScriptEngineFactory()), new SimpleBindings());

    private final EnhancedConfig enhancedConfig = mock(EnhancedConfig.class);

    @BeforeClass
    public void BeforeClass() throws Exception {

        URL config = ServletConnectionFactoryTest.class.getResource("/conf/router.json");
        assertThat(config).isNotNull().overridingErrorMessage("router configuration is not found");

        JsonValue configuration =
                new JsonValue((new ObjectMapper()).readValue(new File(config.toURI()), Map.class));

        requestHandler.addRoute(uriTemplate("/audit/recon"), new MemoryBackend());
        requestHandler.addRoute(uriTemplate("/managed/user"), new MemoryBackend());
        requestHandler.addRoute(uriTemplate("/system/OpenDJ/account"), new MemoryBackend());
        requestHandler.addRoute(uriTemplate("/system/AD/account"), new MemoryBackend());

        URL script = ServletConnectionFactoryTest.class.getResource("/script/");
        assertThat(script).isNotNull().overridingErrorMessage("Failed to find /recon/script folder in test");
        sr.addSourceUnit(new DirectoryContainer("script", script));

        when(enhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(configuration);
        when(enhancedConfig.getConfigurationFactoryPid(any(ComponentContext.class))).thenReturn("");
    }

    @Test
    public void testNoFiltersActive() throws Exception {
        ServletConnectionFactory filterService = new ServletConnectionFactory();
        filterService.bindRequestHandler(requestHandler);
        filterService.bindEnhancedConfig(enhancedConfig);
        filterService.activate(mock(ComponentContext.class));
        filterService.setRouterFilterReady();

        final ResourceResponse response = sendRequest(filterService);

        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_ID).isNotNull();
        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_REVISION).isNotNull();
        assertThat(response.getContent()).doesNotContain("filterMarker");
    }

    @Test
    public void testMaintenanceFilterActive() throws Exception {
        ServletConnectionFactory filterService = new ServletConnectionFactory();
        filterService.bindRequestHandler(requestHandler);
        filterService.bindEnhancedConfig(enhancedConfig);
        filterService.bindMaintenanceFilter(createMockFilter("maintenance"));
        filterService.activate(mock(ComponentContext.class));
        filterService.setRouterFilterReady();

        final ResourceResponse response = sendRequest(filterService);

        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_ID).isNotNull();
        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_REVISION).isNotNull();
        assertThat(response.getContent()).hasArray("filterMarker").contains("maintenance");
    }

    @Test
    public void testAuditFilterActive() throws Exception {
        ServletConnectionFactory filterService = new ServletConnectionFactory();
        filterService.bindRequestHandler(requestHandler);
        filterService.bindEnhancedConfig(enhancedConfig);
        filterService.bindAuditFilter(createMockFilter("audit"));
        filterService.activate(mock(ComponentContext.class));
        filterService.setRouterFilterReady();

        final ResourceResponse response = sendRequest(filterService);

        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_ID).isNotNull();
        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_REVISION).isNotNull();
        assertThat(response.getContent()).hasArray("filterMarker").contains("audit");
    }

    @Test
    public void testMaintenanceAndAuditFilterActive() throws Exception {
        ServletConnectionFactory filterService = new ServletConnectionFactory();
        filterService.bindRequestHandler(requestHandler);
        filterService.bindEnhancedConfig(enhancedConfig);
        filterService.bindMaintenanceFilter(createMockFilter("maintenance"));
        filterService.bindAuditFilter(createMockFilter("audit"));
        filterService.activate(mock(ComponentContext.class));
        filterService.setRouterFilterReady();

        final ResourceResponse response = sendRequest(filterService);

        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_ID).isNotNull();
        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_REVISION).isNotNull();
        assertThat(response.getContent()).hasArray("filterMarker").containsSequence("maintenance", "audit");
    }

    @Test
    public void testMaintenanceFilterActiveThenInactive() throws Exception {
        ServletConnectionFactory filterService = new ServletConnectionFactory();
        filterService.bindRequestHandler(requestHandler);
        filterService.bindEnhancedConfig(enhancedConfig);
        filterService.bindMaintenanceFilter(createMockFilter("maintenance"));
        filterService.activate(mock(ComponentContext.class));
        filterService.setRouterFilterReady();

        ResourceResponse response = sendRequest(filterService);

        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_ID).isNotNull();
        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_REVISION).isNotNull();
        assertThat(response.getContent()).hasArray("filterMarker").contains("maintenance");

        filterService.unbindMaintenanceFilter(mock(Filter.class));
        response = sendRequest(filterService);

        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_ID).isNotNull();
        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_REVISION).isNotNull();
        assertThat(response.getContent()).doesNotContain("filterMarker");
    }

    @Test
    public void testAuditFilterActiveThenInactive() throws Exception {
        ServletConnectionFactory filterService = new ServletConnectionFactory();
        filterService.bindRequestHandler(requestHandler);
        filterService.bindEnhancedConfig(enhancedConfig);
        filterService.bindAuditFilter(createMockFilter("audit"));
        filterService.activate(mock(ComponentContext.class));
        filterService.setRouterFilterReady();

        ResourceResponse response = sendRequest(filterService);

        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_ID).isNotNull();
        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_REVISION).isNotNull();
        assertThat(response.getContent()).hasArray("filterMarker").contains("audit");

        filterService.unbindAuditFilter(mock(Filter.class));
        response = sendRequest(filterService);

        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_ID).isNotNull();
        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_REVISION).isNotNull();
        assertThat(response.getContent()).doesNotContain("filterMarker");
    }

    @Test(expectedExceptions = ServiceUnavailableException.class)
    public void testRouterFiltersNotReady() throws Exception {
        ServletConnectionFactory filterService = new ServletConnectionFactory();
        filterService.bindRequestHandler(requestHandler);
        filterService.bindEnhancedConfig(enhancedConfig);
        filterService.activate(mock(ComponentContext.class));

        sendRequest(filterService);
    }

    @Test
    public void testAddRemoveRouterFilter() throws Exception {
        ServletConnectionFactory filterService = new ServletConnectionFactory();
        filterService.bindRequestHandler(requestHandler);
        filterService.bindEnhancedConfig(enhancedConfig);
        filterService.activate(mock(ComponentContext.class));
        Filter filter = createMockFilter("crestFilter");
        filterService.addFilter(filter);
        filterService.setRouterFilterReady();

        ResourceResponse response = sendRequest(filterService);

        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_ID).isNotNull();
        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_REVISION).isNotNull();
        assertThat(response.getContent()).hasArray("filterMarker").contains("crestFilter");

        filterService.removeFilter(filter);
        response = sendRequest(filterService);

        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_ID).isNotNull();
        assertThat(response.getContent()).stringAt(ResourceResponse.FIELD_CONTENT_REVISION).isNotNull();
        assertThat(response.getContent()).doesNotContain("filterMarker");
    }

    private ResourceResponse sendRequest(ServletConnectionFactory filterService) throws ResourceException {
        Connection connection = filterService.getConnection();
        JsonValue content = new JsonValue(new HashMap<String, Object>());
        return connection.create(createContext("admin"),
                Requests.newCreateRequest("/managed/user", content));
    }

    private Filter createMockFilter(final String markerString) {
        Filter filter = mock(Filter.class);
        when(filter.filterCreate(any(Context.class), any(CreateRequest.class), any(RequestHandler.class)))
                .thenAnswer(new Answer<Promise<ResourceResponse, ResourceException>>() {
                    public Promise<ResourceResponse, ResourceException> answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        Context context = (Context) args[0];
                        CreateRequest request = (CreateRequest) args[1];
                        List<String> markerList = request.getContent().get("filterMarker")
                                .defaultTo(new ArrayList<String>())
                                .asList(String.class);
                        markerList.add(markerString);
                        request.getContent().put("filterMarker", markerList);
                        RequestHandler handler = (RequestHandler) args[2];
                        return handler.handleCreate(context, request);
                    }
                });
        return filter;
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
