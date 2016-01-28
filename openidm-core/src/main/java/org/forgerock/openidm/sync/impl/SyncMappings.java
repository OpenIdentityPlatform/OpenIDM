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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;


/**
 * Describes the synchronization mappings described by the sync.json configuration file.
 *
 * Acts as a configuration holder object for the mapping config for use by the
 * {@link SynchronizationService} and {@link ReconciliationService}.
 */
@Component(name = SyncMappings.PID, policy = ConfigurationPolicy.OPTIONAL, immediate = true )
@Properties({
        @Property(name = "service.description", value = "OpenIDM object mapping service"),
        @Property(name = "service.vendor", value = "ForgeRock AS")
})
@Service
public class SyncMappings implements Mappings {
    public static final String PID = "org.forgerock.openidm.sync";

    /** Object mappings. Order of mappings evaluated during synchronization is significant. */
    private volatile ArrayList<ObjectMapping> mappings = null;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    @Activate
    protected void activate(ComponentContext context) {
        JsonValue config = new JsonValue(enhancedConfig.getConfiguration(context));
        try {
            mappings = new ArrayList<>();
            initMappings(mappings, config);
        } catch (JsonValueException jve) {
            throw new ComponentException("Configuration error: " + jve.getMessage(), jve);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        mappings = null;
    }

    @Modified
    protected void modified(ComponentContext context) {
        JsonValue config = new JsonValue(enhancedConfig.getConfiguration(context));
        ArrayList<ObjectMapping> newMappings = new ArrayList<>();
        try {
            initMappings(newMappings, config);
            mappings = newMappings;
        } catch (JsonValueException jve) {
            throw new ComponentException("Configuration error: " + jve.getMessage(), jve);
        }
    }

    @Override
    public Iterator<ObjectMapping> iterator() {
        return mappings.iterator();
    }

    private void initMappings(ArrayList<ObjectMapping> mappingList, JsonValue config) {
        for (JsonValue jv : config.get("mappings").expect(List.class)) {
            mappingList.add(new ObjectMapping(connectionFactory, jv)); // throws JsonValueException
        }
        for (ObjectMapping mapping : mappingList) {
            mapping.initRelationships(mappingList);
        }
    }

    /**
     * Return the {@link ObjectMapping} for a the mapping {@code name}.
     *
     * @param name the mapping name
     * @return the ObjectMapping
     * @throws SynchronizationException if no mapping exists by the given name.
     */
    public ObjectMapping getMapping(String name) throws SynchronizationException {
        for (ObjectMapping mapping : mappings) {
            if (mapping.getName().equals(name)) {
                return mapping;
            }
        }
        throw new SynchronizationException("No such mapping: " + name);
    }

    /**
     * Instantiate an {@link ObjectMapping} with the given config
     *
     * @param mappingConfig the mapping configuration
     * @return the created ObjectMapping
     */
    public ObjectMapping createMapping(JsonValue mappingConfig) {
        ObjectMapping createdMapping = new ObjectMapping(connectionFactory, mappingConfig);
        List<ObjectMapping> augmentedMappings = new ArrayList<>(mappings);
        augmentedMappings.add(createdMapping);
        createdMapping.initRelationships(augmentedMappings);
        return createdMapping;
    }
}
