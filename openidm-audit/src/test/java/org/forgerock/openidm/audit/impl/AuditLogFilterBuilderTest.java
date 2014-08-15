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

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.sync.TriggerContext;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.javascript.RhinoScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.source.DirectoryContainer;
import org.forgerock.util.promise.Function;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newActivityActionFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newCompositeActionFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newReconActionFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newScriptedFilter;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Test the audit log filter builder
 */
public class AuditLogFilterBuilderTest {

    private ScriptRegistryImpl scriptRegistry;

    @BeforeClass
    public void initScriptRegistry() throws Exception {
        Map<String, Object> configuration = new HashMap<String, Object>(1);
        configuration.put(RhinoScriptEngineFactory.LANGUAGE_NAME, new HashMap<String, Object>(1));

        final Router router = new Router();

        scriptRegistry =
                new ScriptRegistryImpl(configuration, ServiceLoader.load(ScriptEngineFactory.class), null, null);

        URL container = AuditLogFilterBuilderTest.class.getResource("/container/");
        Assert.assertNotNull(container);
        scriptRegistry.addSourceUnit(new DirectoryContainer("container", container));

        scriptRegistry.setPersistenceConfig(PersistenceConfig.builder().connectionProvider(
                new ConnectionProvider() {
                    @Override
                    public Connection getConnection(String connectionId) throws ResourceException {
                        if ("DEFAULT".equalsIgnoreCase(connectionId)) {
                            return Resources.newInternalConnection(router);
                        } else {
                            throw new InternalServerErrorException("Connection not found with id: "
                                    + connectionId);
                        }
                    }

                    @Override
                    public String getConnectionId(Connection connection) throws ResourceException {
                        return "DEFAULT";
                    }
                }).build());
    }

    private AuditLogFilterBuilder auditLogFilterBuilder = new AuditLogFilterBuilder()
            .add(new JsonPointer("eventTypes/activity/filter/actions"),
                    new Function<JsonValue, AuditLogFilter, Exception>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) throws Exception {
                            return newActivityActionFilter(actions);
                        }
                    })
            .add(new JsonPointer("eventTypes/activity/filter/triggers"),
                    new Function<JsonValue, AuditLogFilter, Exception>() {
                        @Override
                        public AuditLogFilter apply(JsonValue triggers) throws Exception {
                            List<AuditLogFilter> filters = new ArrayList<AuditLogFilter>();
                            for (String trigger : triggers.keys()) {
                                filters.add(newActivityActionFilter(triggers.get(trigger), trigger));
                            }
                            return newCompositeActionFilter(filters);
                        }
                    })
            .add(new JsonPointer("eventTypes/recon/filter/actions"),
                    new Function<JsonValue, AuditLogFilter, Exception>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) throws Exception {
                            return newReconActionFilter(actions);
                        }
                    })
            .add(new JsonPointer("eventTypes/recon/filter/triggers"),
                    new Function<JsonValue, AuditLogFilter, Exception>() {
                        @Override
                        public AuditLogFilter apply(JsonValue triggers) throws Exception {
                            List<AuditLogFilter> filters = new ArrayList<AuditLogFilter>();
                            for (String trigger : triggers.keys()) {
                                filters.add(newReconActionFilter(triggers.get(trigger), trigger));
                            }
                            return newCompositeActionFilter(filters);
                        }
                    });

    @Test
    public void testActivityActionFilter() {
        // Given
        /*
        "eventTypes" : {
            "activity" : {
                "filter" : {
                    "actions" : [ "create" ]
                },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("activity", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("actions", new ArrayList<String>() {{
                                    add("create");
                                }});
                            }});
                        }});
                    }});
                }});
        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        ServerContext context = mock(ServerContext.class);

        // When
        CreateRequest create = Requests.newCreateRequest("activity", null, json(object(field("action", "create"))));
        CreateRequest update = Requests.newCreateRequest("activity", null, json(object(field("action", "update"))));
        CreateRequest skittle = Requests.newCreateRequest("activity", null, json(object(field("action", "skittle"))));

        // Then
        assertFalse(filter.isFiltered(context, create));
        assertTrue(filter.isFiltered(context, update));
        assertFalse(filter.isFiltered(context, skittle)); // non-RequestTypes are always unfiltered
    }

    @Test
    public void testActivityActionFilterWithNonRequestTypeAction() {
        // Given
        /*
        "eventTypes" : {
            "activity" : {
                "filter" : {
                    "actions" : [ "skittle" ]
                },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("activity", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("actions", new ArrayList<String>() {{
                                    add("skittle");
                                }});
                            }});
                        }});
                    }});
                }});
        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        ServerContext context = mock(ServerContext.class);

        // When
        CreateRequest create = Requests.newCreateRequest("activity", null, json(object(field("action", "create"))));
        CreateRequest update = Requests.newCreateRequest("activity", null, json(object(field("action", "update"))));
        CreateRequest skittle = Requests.newCreateRequest("activity", null, json(object(field("action", "skittle"))));

        // Then
        assertTrue(filter.isFiltered(context, create));
        assertTrue(filter.isFiltered(context, update));
        assertFalse(filter.isFiltered(context, skittle)); // non-RequestTypes are always unfiltered
    }

    @Test
    public void testReconActionFilter() {
        // Given
        /*
        "eventTypes" : {
            "recon" : {
                "filter" : {
                    "actions" : [ "link", "unlink" ]
                },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("recon", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("actions", new ArrayList<String>() {{
                                    add("link");
                                    add("unlink");
                                }});
                            }});
                        }});
                    }});
                }});
        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        ServerContext context = mock(ServerContext.class);

        // When
        CreateRequest link = Requests.newCreateRequest("recon", null, json(object(field("action", "link"))));
        CreateRequest unlink = Requests.newCreateRequest("recon", null, json(object(field("action", "unlink"))));
        CreateRequest exception = Requests.newCreateRequest("recon", null, json(object(field("action", "exception"))));
        CreateRequest skittle = Requests.newCreateRequest("recon", null, json(object(field("action", "skittle"))));

        // Then
        assertFalse(filter.isFiltered(context, link));
        assertFalse(filter.isFiltered(context, unlink));
        assertTrue(filter.isFiltered(context, exception));
        assertFalse(filter.isFiltered(context, skittle)); // non-Actions are always unfiltered
    }

    @Test
    public void testReconActionFilterWithNonRequestTypeAction() {
        // Given
        /*
        "eventTypes" : {
            "recon" : {
                "filter" : {
                    "actions" : [ "skittle" ]
                },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("recon", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("actions", new ArrayList<String>() {{
                                    add("skittle");
                                }});
                            }});
                        }});
                    }});
                }});
        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        ServerContext context = mock(ServerContext.class);

        // When
        CreateRequest link = Requests.newCreateRequest("recon", null, json(object(field("action", "link"))));
        CreateRequest unlink = Requests.newCreateRequest("recon", null, json(object(field("action", "unlink"))));
        CreateRequest exception = Requests.newCreateRequest("recon", null, json(object(field("action", "exception"))));
        CreateRequest skittle = Requests.newCreateRequest("recon", null, json(object(field("action", "skittle"))));

        // Then
        assertTrue(filter.isFiltered(context, link));
        assertTrue(filter.isFiltered(context, unlink));
        assertTrue(filter.isFiltered(context, exception));
        assertFalse(filter.isFiltered(context, skittle)); // non-Actions are always unfiltered
    }

    @Test
    public void testActivityTriggerFilter() {
        // Given
        /*
        "eventTypes" : {
            "activity" : {
                "filter" : {
                     "triggers" : {
                         "sometrigger" : [
                             "create",
                             "update"
                         ]
                     }
                 },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("activity", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("triggers", new HashMap<String, Object>() {{
                                    put("sometrigger", new ArrayList<String>() {{
                                        add("create");
                                    }});
                                }});
                            }});
                        }});
                    }});
                }});
        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        ServerContext noTrigger = mock(ServerContext.class);
        ServerContext hasTrigger = new ServerContext(new TriggerContext(new RootContext(), "sometrigger"));

        // When
        CreateRequest create = Requests.newCreateRequest("activity", null, json(object(field("action", "create"))));
        CreateRequest update = Requests.newCreateRequest("activity", null, json(object(field("action", "update"))));
        CreateRequest skittle = Requests.newCreateRequest("activity", null, json(object(field("action", "skittle"))));

        // Then
        assertFalse(filter.isFiltered(noTrigger, create));
        assertFalse(filter.isFiltered(noTrigger, update));
        assertFalse(filter.isFiltered(noTrigger, skittle));
        assertFalse(filter.isFiltered(hasTrigger, create));
        assertTrue(filter.isFiltered(hasTrigger, update));
        assertFalse(filter.isFiltered(hasTrigger, skittle));// non-RequestTypes are always unfiltered
    }

    @Test
    public void testActivityTriggerFilterWithNonRequestTypeAction() {
        // Given
        /*
        "eventTypes" : {
            "activity" : {
                "filter" : {
                     "triggers" : {
                         "sometrigger" : [ "skittle" ]
                     }
                 },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("activity", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("triggers", new HashMap<String, Object>() {{
                                    put("sometrigger", new ArrayList<String>() {{
                                        add("skittle");
                                    }});
                                }});
                            }});
                        }});
                    }});
                }});
        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        ServerContext noTrigger = mock(ServerContext.class);
        ServerContext hasTrigger = new ServerContext(new TriggerContext(new RootContext(), "sometrigger"));

        // When
        CreateRequest create = Requests.newCreateRequest("activity", null, json(object(field("action", "create"))));
        CreateRequest update = Requests.newCreateRequest("activity", null, json(object(field("action", "update"))));
        CreateRequest skittle = Requests.newCreateRequest("activity", null, json(object(field("action", "skittle"))));

        // Then
        assertFalse(filter.isFiltered(noTrigger, create));
        assertFalse(filter.isFiltered(noTrigger, update));
        assertFalse(filter.isFiltered(noTrigger, skittle));
        assertTrue(filter.isFiltered(hasTrigger, create));
        assertTrue(filter.isFiltered(hasTrigger, update));
        assertFalse(filter.isFiltered(hasTrigger, skittle));// non-RequestTypes are always unfiltered
    }

    @Test
    public void testActivityScriptFilter() {
        // Given
        /*
        "eventTypes" : {
            "activity" : {
                "filter" : {
                    "script" : {
                        "type" : "text/javascript",
                        "name" : "logfilter.js"
                    }
                },
            },
        }
        */
        JsonValue config = new JsonValue(
                new HashMap<String, Object>() {{
                    put("eventTypes", new HashMap<String, Object>() {{
                        put("activity", new HashMap<String, Object>() {{
                            put("filter", new HashMap<String, Object>() {{
                                put("script", new HashMap<String, Object>() {{
                                    put("type", "text/javascript");
                                    put("name", "logfilter.js");
                                }});
                            }});
                        }});
                    }});
                }});

        AuditLogFilter filter = new AuditLogFilterBuilder()
                .add(new JsonPointer("eventTypes/activity/filter/script"),
                        new Function<JsonValue, AuditLogFilter, Exception>() {
                            @Override
                            public AuditLogFilter apply(JsonValue scriptConfig) throws Exception {
                                return newScriptedFilter(scriptRegistry.takeScript(scriptConfig));
                            }
                        })
                .build(config);

        ServerContext context = mock(ServerContext.class);

        // When
        CreateRequest create = Requests.newCreateRequest("activity", null, json(object(field("action", "create"))));
        CreateRequest update = Requests.newCreateRequest("activity", null, json(object(field("action", "update"))));
        CreateRequest skittle = Requests.newCreateRequest("activity", null, json(object(field("action", "skittle"))));

        // Then
        assertFalse(filter.isFiltered(context, create));  // don't filter creates
        assertTrue(filter.isFiltered(context, update));   // filter out updates
        assertFalse(filter.isFiltered(context, skittle)); // don't filter weird stuff
    }

}
