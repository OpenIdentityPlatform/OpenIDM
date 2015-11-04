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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.AS_SINGLE_FIELD_VALUES_FILTER;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.NEVER_FILTER;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.TYPE_ACTIVITY;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newActionFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newAndCompositeFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newOrCompositeFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newReconActionFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newScriptedFilter;
import static org.forgerock.services.context.ClientContext.newInternalClientContext;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import javax.script.ScriptException;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.openidm.sync.TriggerContext;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.javascript.RhinoScriptEngineFactory;
import org.forgerock.script.registry.ScriptRegistryImpl;
import org.forgerock.script.source.DirectoryContainer;
import org.forgerock.services.context.Context;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test the audit log filter builder
 */
public class AuditLogFilterBuilderTest {

    private ScriptRegistryImpl scriptRegistry;

    @BeforeClass
    public void initScriptRegistry() throws Exception {
        Map<String, Object> configuration = new HashMap<String, Object>(1);
        configuration.put(RhinoScriptEngineFactory.LANGUAGE_NAME, new HashMap<String, Object>(1));

        scriptRegistry =
                new ScriptRegistryImpl(configuration, ServiceLoader.load(ScriptEngineFactory.class), null, null);

        URL container = AuditLogFilterBuilderTest.class.getResource("/container/");
        Assert.assertNotNull(container);
        scriptRegistry.addSourceUnit(new DirectoryContainer("container", container));

    }

    private AuditLogFilterBuilder auditLogFilterBuilder = new AuditLogFilterBuilder()
            .add("eventTypes/activity/filter/actions",
                    new AuditLogFilters.JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) {
                            return newActionFilter(TYPE_ACTIVITY, actions);
                        }
                    })
            .add("eventTypes/activity/filter/triggers",
                    new AuditLogFilters.JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue triggers) {
                            List<AuditLogFilter> filters = new ArrayList<AuditLogFilter>();
                            for (String trigger : triggers.keys()) {
                                filters.add(newActionFilter(TYPE_ACTIVITY, triggers.get(trigger), trigger));
                            }
                            return newOrCompositeFilter(filters);
                        }
                    })
            .add("eventTypes/recon/filter/actions",
                    new AuditLogFilters.JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) {
                            return newReconActionFilter(actions);
                        }
                    })
            .add("eventTypes/recon/filter/triggers",
                    new AuditLogFilters.JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue triggers) {
                            List<AuditLogFilter> filters = new ArrayList<AuditLogFilter>();
                            for (String trigger : triggers.keys()) {
                                filters.add(newReconActionFilter(triggers.get(trigger), trigger));
                            }
                            return newOrCompositeFilter(filters);
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
        JsonValue config = json(
                object(
                        field("eventTypes", object(
                                field("activity", object(
                                        field("filter", object(
                                                field("actions", array("create"))
                                        ))
                                ))
                        ))
                ));

        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        Context context = mock(Context.class);

        // When
        CreateRequest create = Requests.newCreateRequest("activity", null, json(object(field("operation", "create"))));
        CreateRequest update = Requests.newCreateRequest("activity", null, json(object(field("operation", "update"))));
        CreateRequest skittle = Requests.newCreateRequest("activity", null, json(object(field("operation", "skittle"))));

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
        JsonValue config = json(
                object(
                        field("eventTypes", object(
                                field("activity", object(
                                        field("filter", object(
                                                field("actions", array("skittle"))
                                        ))
                                ))
                        ))
                ));

        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        Context context = mock(Context.class);

        // When
        CreateRequest create = Requests.newCreateRequest("activity", null, json(object(field("operation", "create"))));
        CreateRequest update = Requests.newCreateRequest("activity", null, json(object(field("operation", "update"))));
        CreateRequest skittle = Requests.newCreateRequest("activity", null, json(object(field("operation", "skittle"))));

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
        JsonValue config = json(
                object(
                        field("eventTypes", object(
                                field("recon", object(
                                        field("filter", object(
                                                field("actions", array("link", "unlink"))
                                        ))
                                ))
                        ))
                ));
        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        Context context = mock(Context.class);

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
        JsonValue config = json(
                object(
                        field("eventTypes", object(
                                field("recon", object(
                                        field("filter", object(
                                                field("actions", array("skittle"))
                                        ))
                                ))
                        ))
                ));
        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        Context context = mock(Context.class);

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
        JsonValue config = json(
                object(
                        field("eventTypes", object(
                                field("activity", object(
                                        field("filter", object(
                                                field("triggers", object(
                                                        field("sometrigger", array("create"))
                                                ))
                                        ))
                                ))
                        ))
                ));
        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        Context noTrigger = mock(Context.class);
        Context hasTrigger = new TriggerContext(noTrigger, "sometrigger");

        // When
        CreateRequest create = Requests.newCreateRequest("activity", null, json(object(field("operation", "create"))));
        CreateRequest update = Requests.newCreateRequest("activity", null, json(object(field("operation", "update"))));
        CreateRequest skittle = Requests.newCreateRequest("activity", null, json(object(field("operation", "skittle"))));

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
        JsonValue config = json(
                object(
                        field("eventTypes", object(
                                field("activity", object(
                                        field("filter", object(
                                                field("triggers", object(
                                                        field("sometrigger", array("skittle"))
                                                ))
                                        ))
                                ))
                        ))
                ));
        AuditLogFilter filter = auditLogFilterBuilder.build(config);
        Context noTrigger = mock(Context.class);
        Context hasTrigger = newInternalClientContext(new TriggerContext(noTrigger, "sometrigger"));

        // When
        CreateRequest create = Requests.newCreateRequest("activity", null, json(object(field("operation", "create"))));
        CreateRequest update = Requests.newCreateRequest("activity", null, json(object(field("operation", "update"))));
        CreateRequest skittle = Requests.newCreateRequest("activity", null, json(object(field("operation", "skittle"))));

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
        JsonValue config = json(
                object(
                        field("eventTypes", object(
                                field("activity", object(
                                        field("filter", object(
                                                field("script", object(
                                                        field("type", "text/javascript"),
                                                        field("name", "logfilter.js")
                                                ))
                                        ))
                                ))
                        ))
                ));
        AuditLogFilter filter = new AuditLogFilterBuilder()
                .add("eventTypes/activity/filter/script",
                        new AuditLogFilters.JsonValueObjectConverter<AuditLogFilter>() {
                            @Override
                            public AuditLogFilter apply(JsonValue scriptConfig) {
                                try {
                                    return newScriptedFilter(scriptRegistry.takeScript(scriptConfig));
                                } catch (ScriptException e) {
                                    return NEVER_FILTER;
                                }
                            }
                        })
                .build(config);

        Context context = mock(Context.class);

        // When
        CreateRequest create = Requests.newCreateRequest("activity", null, json(object(field("operation", "create"))));
        CreateRequest update = Requests.newCreateRequest("activity", null, json(object(field("operation", "update"))));
        CreateRequest skittle = Requests.newCreateRequest("activity", null, json(object(field("operation", "skittle"))));

        // Then
        assertFalse(filter.isFiltered(context, create));  // don't filter creates
        assertTrue(filter.isFiltered(context, update));   // filter out updates
        assertFalse(filter.isFiltered(context, skittle)); // don't filter weird stuff
    }

    private AuditLogFilterBuilder getFieldValueFilterBuilder() {
        return new AuditLogFilterBuilder()
                .add("/",
                        new AuditLogFilters.JsonValueObjectConverter<AuditLogFilter>() {
                            @Override
                            public AuditLogFilter apply(JsonValue filterConfig) {
                                return newAndCompositeFilter(filterConfig.asList(AS_SINGLE_FIELD_VALUES_FILTER));
                            }
                        });
    }

    @Test
    public void testFieldFilter() {
        // Filter-in if entryType is summary or reconType is full

        // Given
        JsonValue config = json(array(
                object(
                        field("name", "entryType"),
                        field("values", array("summary"))
                ),
                object(
                        field("name", "reconType"),
                        field("values", array("full"))
                )
        ));

        AuditLogFilter filter = getFieldValueFilterBuilder().build(config);
        Context context = mock(Context.class);

        // When
        CreateRequest start = Requests.newCreateRequest("recon", null, json(object(field("entryType", "start"))));
        CreateRequest entry = Requests.newCreateRequest("recon", null, json(object(field("entryType", "entry"))));
        CreateRequest summary = Requests.newCreateRequest("recon", null, json(object(field("entryType", "summary"))));
        CreateRequest full = Requests.newCreateRequest("recon", null, json(object(field("entryType", "entry"), field("reconType", "full"))));
        CreateRequest byId = Requests.newCreateRequest("recon", null, json(object(field("entryType", "entry"), field("reconType", "byId"))));


        // Then
        assertTrue(filter.isFiltered(context, start));  // filter out start
        assertTrue(filter.isFiltered(context, entry));   // filter out entry
        assertFalse(filter.isFiltered(context, summary)); // don't filter summary
        assertFalse(filter.isFiltered(context, full)); // don't filter full
        assertTrue(filter.isFiltered(context, byId)); // filter out byId
    }

    @Test
    public void testFieldFilterDuplicate() {
        // test duplicate field filters

        // Given
        JsonValue config = json(array(
                object(
                        field("name", "entryType"),
                        field("values", array("summary"))
                ),
                object(
                        field("name", "entryType"),
                        field("values", array("start"))
                )
        ));

        AuditLogFilter filter = getFieldValueFilterBuilder().build(config);
        Context context = mock(Context.class);

        // When
        CreateRequest start = Requests.newCreateRequest("recon", null, json(object(field("entryType", "start"))));
        CreateRequest entry = Requests.newCreateRequest("recon", null, json(object(field("entryType", "entry"))));
        CreateRequest summary = Requests.newCreateRequest("recon", null, json(object(field("entryType", "summary"))));


        // Then
        assertFalse(filter.isFiltered(context, start));  // don't filter start
        assertTrue(filter.isFiltered(context, entry));   // filter out entiy
        assertFalse(filter.isFiltered(context, summary)); // don't filter summary
    }

    /** an always-true filter */
    private static AuditLogFilter TRUE = new AuditLogFilter() {
        @Override
        public boolean isFiltered(Context context, CreateRequest request) {
            return true;
        }
    };

    /** an always-false filter */
    public static AuditLogFilter FALSE = new AuditLogFilter() {
        @Override
        public boolean isFiltered(Context context, CreateRequest request) {
            return false;
        }
    };

    /** Test data for composite audit log filters:
     *
     * <ul>
     *     <li>array of 3 filters from which to create a composite filter</li>
     *     <li>expected result from OrCompositeFilter</li>
     *     <li>expected result from AndCompositeFilter</li>
     * </ul>
     */
    @DataProvider(name = "compositeData")
    public Object[][] createCompositeData() {
        return new Object[][] {
                { new AuditLogFilter[] { TRUE, TRUE, TRUE }, true, true },
                { new AuditLogFilter[] { TRUE, TRUE, FALSE }, true, false },
                { new AuditLogFilter[] { TRUE, FALSE, TRUE }, true, false },
                { new AuditLogFilter[] { TRUE, FALSE, FALSE }, true, false },
                { new AuditLogFilter[] { FALSE, TRUE, TRUE }, true, false },
                { new AuditLogFilter[] { FALSE, TRUE, FALSE }, true, false },
                { new AuditLogFilter[] { FALSE, FALSE, TRUE }, true, false },
                { new AuditLogFilter[] { FALSE, FALSE, FALSE }, false, false }
        };
    }

    @Test(dataProvider = "compositeData")
    public void testCompositeIdentity(AuditLogFilter[] filters, boolean orFilterResult, boolean andFilterResult) {
        Context context = mock(Context.class);
        CreateRequest request = mock(CreateRequest.class);

        assertThat(newOrCompositeFilter(Arrays.asList(filters)).isFiltered(context, request)).isEqualTo(orFilterResult);
        assertThat(newAndCompositeFilter(Arrays.asList(filters)).isFiltered(context, request)).isEqualTo(andFilterResult);
    }

    @Test
    public void testGetByGlob() {

        JsonValue config = json(
                object(
                        field("eventTypes", object(
                                field("activity", object(
                                        field("filter", object(
                                                field("actions", array("create"))
                                        ))
                                )),
                                field("recon", object(
                                        field("filter", object(
                                                field("script", object(
                                                        field("type", "text/javascript"),
                                                        field("name", "reconfilter.js")
                                                ))
                                        ))
                                )),
                                field("custom", object(
                                        field("filter", object(
                                                field("script", object(
                                                        field("type", "text/javascript"),
                                                        field("name", "customfilter.js")
                                                ))
                                        ))
                                ))
                        ))
                ));

        JsonValue result = new AuditLogFilterBuilder().getByGlob(config, "eventTypes/*/filter/script");
        assertThat(result.isDefined("activity")).isFalse();
        assertThat(result.isDefined("recon")).isTrue();
        assertThat(result.get("recon").isDefined("type")).isTrue();
        assertThat(result.get("recon").isDefined("name")).isTrue();
        assertThat(result.get("recon").get("name").asString()).isEqualTo("reconfilter.js");
        assertThat(result.isDefined("custom")).isTrue();
        assertThat(result.get("custom").isDefined("type")).isTrue();
        assertThat(result.get("custom").isDefined("name")).isTrue();
        assertThat(result.get("custom").get("name").asString()).isEqualTo("customfilter.js");
    }
}
