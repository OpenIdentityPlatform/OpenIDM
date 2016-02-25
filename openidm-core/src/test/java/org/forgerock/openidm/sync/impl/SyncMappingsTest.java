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
 * Portions copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.util.Scripts;
import org.forgerock.script.ScriptRegistry;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * Test the SyncMappings
 */
public class SyncMappingsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public void init() {
        Scripts.init(mock(ScriptRegistry.class));
    }

    private JsonValue getConfig() throws URISyntaxException, IOException {
        URL syncConfigUrl = ObjectMappingTest.class.getResource("/conf/sync.json");
        assertThat(syncConfigUrl).isNotNull();
        return json(mapper.readValue(new File(syncConfigUrl.toURI()), Map.class));
    }

    @Test
    public void testGetMapping() throws Exception {
        EnhancedConfig enhancedConfig = mock(EnhancedConfig.class);
        when(enhancedConfig.getConfiguration(any(ComponentContext.class))).thenReturn(getConfig().asMap());

        SyncMappings mappings = new SyncMappings();
        mappings.bindEnhancedConfig(enhancedConfig);
        mappings.activate(mock(ComponentContext.class));

        ObjectMapping mapping = mappings.getMapping("testMapping");
        assertThat(mapping).isNotNull();
        assertThat(mapping.getName()).isEqualTo("testMapping");
        assertThat(mapping.getSourceObjectSet()).isEqualTo("managed/user");
        assertThat(mapping.getTargetObjectSet()).isEqualTo("system/ldap/account");
    }

    @Test(expectedExceptions = SynchronizationException.class)
    public void testGetMappingNotPresent() throws Exception {
        EnhancedConfig enhancedConfig = mock(EnhancedConfig.class);
        when(enhancedConfig.getConfiguration(any(ComponentContext.class))).thenReturn(getConfig().asMap());

        SyncMappings mappings = new SyncMappings();
        mappings.bindEnhancedConfig(enhancedConfig);
        mappings.activate(mock(ComponentContext.class));

        ObjectMapping mapping = mappings.getMapping("bogusMapping");
    }

    @Test
    public void testCreateMapping() throws Exception {
        SyncMappings mappings = new SyncMappings();
        ObjectMapping mapping = mappings.createMapping(getConfig().get("mappings").get(0));
        assertThat(mapping).isNotNull();
        assertThat(mapping.getName()).isEqualTo("testMapping");
        assertThat(mapping.getSourceObjectSet()).isEqualTo("managed/user");
        assertThat(mapping.getTargetObjectSet()).isEqualTo("system/ldap/account");
    }

    @Test
    public void testIterator() throws Exception {
        EnhancedConfig enhancedConfig = mock(EnhancedConfig.class);
        when(enhancedConfig.getConfiguration(any(ComponentContext.class))).thenReturn(getConfig().asMap());

        SyncMappings mappings = new SyncMappings();
        mappings.bindEnhancedConfig(enhancedConfig);
        mappings.activate(mock(ComponentContext.class));
        Iterator<ObjectMapping> iterator = mappings.iterator();

        assertThat(iterator.hasNext()).isTrue();
        ObjectMapping mapping = iterator.next();
        assertThat(mapping).isNotNull();
        assertThat(mapping.getName()).isEqualTo("testMapping");
        assertThat(mapping.getSourceObjectSet()).isEqualTo("managed/user");
        assertThat(mapping.getTargetObjectSet()).isEqualTo("system/ldap/account");

        assertThat(iterator.hasNext()).isFalse();
    }
}
