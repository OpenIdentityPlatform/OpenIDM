/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.recon;

import java.util.Map;

import java.util.Collection;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import static org.fest.assertions.Assertions.assertThat;


import org.forgerock.openidm.sync.impl.MapEntry;
import org.forgerock.openidm.sync.impl.MappingConfiguration;
import org.forgerock.openidm.sync.impl.PropertyEntry;

/**
 * Test the parsing and required properties for property entries.
 */
public class PropertyEntryTest {

    private Map map;
    private MappingConfiguration mappingConfiguration;

    @BeforeClass
    public void loadConfiguration() throws Exception {
        Map<String, Object> config = null;
        ConfigurationLoader loader = new ConfigurationLoader();
        config = loader.loadConfiguration(ConfigurationConstants.objectSynchronizationTestConfig());
        map = (Map) config.get("objectSynchronization");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fullConfigurationParse() throws Exception {

        mappingConfiguration = new MappingConfiguration(map);

    }

    @Test
    public void sourcePathMustExist() {
        Collection<MapEntry> entries = mappingConfiguration.getMapEntries();
        for (MapEntry entry : entries) {
            Collection<PropertyEntry> properties = entry.getPropertyEntries();
            for (PropertyEntry property : properties) {
                assertThat(property.getSourcePath()).isNotNull();
            }
        }
    }

    @Test
    public void targetPathMustExist() {
        Collection<MapEntry> entries = mappingConfiguration.getMapEntries();
        for (MapEntry entry : entries) {
            Collection<PropertyEntry> properties = entry.getPropertyEntries();
            for (PropertyEntry property : properties) {
                assertThat(property.getTargetPath()).isNotNull();
            }
        }

    }

    @Test
    public void scriptMustExist() {
        Collection<MapEntry> entries = mappingConfiguration.getMapEntries();
        for (MapEntry entry : entries) {
            Collection<PropertyEntry> properties = entry.getPropertyEntries();
            for (PropertyEntry property : properties) {
                assertThat(property.getScript()).isNotNull();
            }
        }
    }

}
