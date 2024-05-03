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
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.sync.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;


/**
 * Describes the synchronization mappings described by the sync.json configuration file.
 *
 * Acts as a configuration holder object for the mapping config for use by the
 * {@link SynchronizationService} and {@link ReconciliationService}.
 */
@Component(
        name = SyncMappings.PID,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM object mapping service")
public class SyncMappings implements Mappings {
    public static final String PID = "org.forgerock.openidm.sync";

    /** Object mappings. Order of mappings evaluated during synchronization is significant. */
    private volatile List<ObjectMapping> mappings = new ArrayList<>();

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC)
    private IDMConnectionFactory connectionFactory;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile ScriptRegistry scriptRegistry;

    /**
     * Activate/modify the component.  Because the List of ObjectMappings is re-assigned based on the updated
     * configuration, this method is safe for both activation and modification.
     *
     * @param context the ComponentContext
     */
    @Activate
    @Modified
    protected void activate(ComponentContext context) {
        JsonValue config = enhancedConfig.getConfigurationAsJson(context);
        try {
            mappings = initMappings(config);
        } catch (JsonValueException jve) {
            throw new ComponentException("Configuration error: " + jve.getMessage(), jve);
        }
    }

    /**
     * Deactivate the component.
     *
     * @param context the ComponentContext
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        mappings = new ArrayList<>();
    }

    private List<ObjectMapping> initMappings(JsonValue config) {
        final List<ObjectMapping> mappingList = new ArrayList<>();
        for (JsonValue jv : config.get("mappings").expect(List.class)) {
            mappingList.add(new ObjectMapping(connectionFactory, jv)); // throws JsonValueException
        }
        for (ObjectMapping mapping : mappingList) {
            mapping.initRelationships(mappingList);
        }
        return mappingList;
    }

    @Override
    public Iterator<ObjectMapping> iterator() {
        return mappings.iterator();
    }

    /**
     * Return the {@link ObjectMapping} for a the mapping {@code name}.
     *
     * @param name the mapping name
     * @return the ObjectMapping
     * @throws SynchronizationException if no mapping exists by the given name.
     */
    @Override
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
    @Override
    public ObjectMapping createMapping(JsonValue mappingConfig) {
        ObjectMapping createdMapping = new ObjectMapping(connectionFactory, mappingConfig);
        List<ObjectMapping> augmentedMappings = new ArrayList<>(mappings);
        augmentedMappings.add(createdMapping);
        createdMapping.initRelationships(augmentedMappings);
        return createdMapping;
    }

	public void bindEnhancedConfig(EnhancedConfig enhancedConfig2) {
		this.enhancedConfig=enhancedConfig2;
	}
}
