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

import org.forgerock.openidm.sync.impl.MappingConfiguration;
import org.forgerock.openidm.sync.impl.MapEntry;

/**
 * Test the parsing, required and default values for mapping configuration.
 */
public class MappingConfigTest {

    private Map map;
    private MappingConfiguration mappingConfiguration;

    @BeforeClass
    public void loadConfiguration() throws Exception {
        Map<String, Object> config = null;
        ConfigurationLoader loader = new ConfigurationLoader();
        config = loader.loadConfiguration(ConfigurationConstants.objectSynchronizationTestConfig());
        map = (Map) config.get("objectSynchronization");
        fullConfigurationParse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fullConfigurationParse() throws Exception {

        mappingConfiguration = new MappingConfiguration(map);

    }

    @Test
    public void sourceObjectMustExist() throws Exception {
        Collection<MapEntry> entries = mappingConfiguration.getMapEntries();
        for (MapEntry entry : entries) {
            assertThat(entry.getSourceObject()).isNotNull();
        }
    }

    @Test
    public void targetObjectMustExist() throws Exception {
        Collection<MapEntry> entries = mappingConfiguration.getMapEntries();
        for (MapEntry entry : entries) {
            assertThat(entry.getTargetObject()).isNotNull();
        }
    }

    @Test
    public void namedQueryMustExist() throws Exception {
        Collection<MapEntry> entries = mappingConfiguration.getMapEntries();
        for (MapEntry entry : entries) {
            assertThat(entry.getNamedQuery()).isNotNull();
        }
    }

    @Test
    public void propertyMappingsMustExist() throws Exception {
        Collection<MapEntry> entries = mappingConfiguration.getMapEntries();
        for (MapEntry entry : entries) {
            assertThat(entry.getPropertyEntries()).isNotNull();
            assertThat(entry.getPropertyEntries()).isNotEmpty();
        }
    }


}
