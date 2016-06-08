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

package org.forgerock.openidm.router.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Router.uriTemplate;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.SimpleBindings;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Router;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.groovy.GroovyScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.SecurityContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A test of the filters created from router.json.
 */
public class RouterConfigTest {

    private Router requestHandler;
    private RouterConfig routerConfig;
    private Filter filter;

    @BeforeClass
    public void BeforeClass() throws Exception {

        URL config = RouterConfigTest.class.getResource("/conf/filter.json");
        assertThat(config).isNotNull().overridingErrorMessage("router configuration is not found");
        JsonValue configuration = new JsonValue((new ObjectMapper()).readValue(new File(config.toURI()), Map.class));

        requestHandler = new Router();
        requestHandler.addRoute(uriTemplate("managed/user"), new MemoryBackend());

        // needed to run scripted groovy router filter
        ScriptRegistryImpl sr = new ScriptRegistryImpl(new HashMap<String, Object>(),
                Collections.<ScriptEngineFactory>singleton(new GroovyScriptEngineFactory()), new SimpleBindings());

        routerConfig = new RouterConfig();
        routerConfig.bindScriptRegistry(sr);

        filter = routerConfig.newFilter(configuration);
        assertThat(filter).isNotNull();
    }

    @Test
    public void testFilterCreate() throws Exception {
        JsonValue content = json(object(field("username", "bob"), field("password", "secret")));
        final JsonValue response = filter
                .filterCreate(createContext("admin"),
                        Requests.newCreateRequest("managed/user", content), requestHandler)
                .getOrThrow()
                .getContent();

        assertThat(response.get("_id").asString()).isEqualTo("0");
        assertThat(response.get("password").asString()).isEqualTo("removed");
    }

    @Test
    public void testFilterUpdate() throws Exception {
        JsonValue content = json(object(field("username", "bob"), field("password", "secret")));
        final JsonValue response = filter
                .filterUpdate(createContext("admin"),
                        Requests.newUpdateRequest("managed/user/0", content), requestHandler)
                .getOrThrow()
                .getContent();

        assertThat(response.get("_id").asString()).isEqualTo("0");
        assertThat(response.get("password").asString()).isEqualTo("secret");
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
